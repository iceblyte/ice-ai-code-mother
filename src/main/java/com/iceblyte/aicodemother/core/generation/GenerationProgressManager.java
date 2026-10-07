package com.iceblyte.aicodemother.core.generation;

import com.iceblyte.aicodemother.exception.BusinessException;
import com.iceblyte.aicodemother.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 代码生成进度管理器
 * <p>
 * 职责（修复「生成过程中离开页面后回来无法看到流式输出」的问题）：
 * <ul>
 *     <li>{@link #track(long, Flux)}：以 appId 为维度登记生成任务，<b>由服务端内部订阅独立驱动生成</b>——
 *     AI 生成、代码保存、对话历史入库都在该订阅链上执行，不随任何 SSE 观察者（页面）的断开而中断；
 *     返回给调用方的只是观察流，观察者断开/重连互不影响</li>
 *     <li>{@link #subscribe(long)}：供「回到页面的用户」查询生成状态并续看流式输出（三态：空闲/进行中/已完成）</li>
 *     <li>同一 appId 已有进行中生成时拒绝新任务，避免并发生成互相覆盖</li>
 *     <li>终态任务保留一段时间（默认 10 分钟）供迟到的订阅者查询结果，之后惰性清理</li>
 * </ul>
 */
@Slf4j
@Component
public class GenerationProgressManager {

    /**
     * 终态任务保留时长（毫秒）：期间内重新订阅仍能拿到最终状态
     */
    private static final long TERMINAL_RETENTION_MS = 10 * 60 * 1000L;

    /**
     * 生成订阅状态
     */
    public enum State {
        /** 无生成任务（前端无需任何处理） */
        IDLE,
        /** 生成进行中（前端应续接流式输出） */
        RUNNING,
        /** 生成已结束（保留期内；AI 回复已入库，前端重新拉取对话历史即可） */
        FINISHED
    }

    /**
     * 订阅结果：状态 + 观察流（仅 RUNNING 时非空）
     */
    public record Subscription(State state, Flux<String> flux) {
    }

    /**
     * appId -> 生成任务
     */
    private final Map<Long, GenerationTask> tasks = new ConcurrentHashMap<>();

    /**
     * 跟踪一次代码生成：登记任务并由服务端内部订阅独立驱动源 Flux 执行。
     * <p>
     * 关键语义：源 Flux（AI 生成 → 代码保存 → 对话历史入库）由内部订阅驱动，
     * 与返回给调用方的观察流解耦——SSE 观察者断开（用户离开页面）不会取消生成。
     *
     * @param appId  应用 ID
     * @param source 生成源 Flux（冷流，被内部订阅后开始执行）
     * @return 观察流（快照 + 实时增量，可多次订阅）
     * @throws BusinessException 同一应用已有进行中的生成时抛出
     */
    public Flux<String> track(long appId, Flux<String> source) {
        synchronized (this) {
            evictExpiredTasks();
            GenerationTask existing = tasks.get(appId);
            if (existing != null && !existing.isTerminal()) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "当前应用正在生成中，请稍候再试");
            }
            GenerationTask task = new GenerationTask(appId);
            tasks.put(appId, task);
            try {
                // 服务端内部订阅：独立驱动生成全流程（AI 生成 / 代码保存 / 对话历史入库）。
                // 不持有 Disposable，不随任何 SSE 观察者取消；错误已通过 task 传播给观察者，这里只记日志。
                source.doOnNext(task::append)
                        .doOnComplete(task::complete)
                        .doOnError(task::error)
                        .subscribe(
                                chunk -> { },
                                error -> log.error("生成任务执行失败: appId={}, error={}", appId, error.getMessage())
                        );
            } catch (Exception e) {
                // 源 Flux 订阅阶段同步抛出的异常（如参数校验）：清理任务并继续传播
                tasks.remove(appId, task);
                throw e;
            }
            return task.flux();
        }
    }

    /**
     * 订阅指定应用的生成进度（供回到页面的用户续看）
     * <ul>
     *     <li>无任务：IDLE</li>
     *     <li>进行中：RUNNING + 观察流（先快照后实时）</li>
     *     <li>已结束（保留期内）：FINISHED（AI 回复已入库，前端重新拉历史即可）</li>
     * </ul>
     *
     * @param appId 应用 ID
     * @return 订阅结果
     */
    public Subscription subscribe(long appId) {
        GenerationTask task;
        synchronized (this) {
            evictExpiredTasks();
            task = tasks.get(appId);
        }
        if (task == null) {
            return new Subscription(State.IDLE, Flux.empty());
        }
        if (task.isTerminal()) {
            return new Subscription(State.FINISHED, Flux.empty());
        }
        return new Subscription(State.RUNNING, task.flux());
    }

    /**
     * 清理过期的终态任务（调用方需持有锁）
     */
    private void evictExpiredTasks() {
        long now = System.currentTimeMillis();
        tasks.entrySet().removeIf(entry ->
                entry.getValue().isTerminal()
                        && now - entry.getValue().getDoneAt() > TERMINAL_RETENTION_MS);
    }
}
