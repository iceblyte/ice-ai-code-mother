package com.iceblyte.aicodemother.core.generation;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一次 AI 代码生成任务（按 appId 维度）
 * <p>
 * 设计要点：
 * <ul>
 *     <li>生成内容全部缓存在 replay-all 的 Sinks 中，任何时刻订阅都能从头重放（支持离开页面后回看）</li>
 *     <li>{@link #flux()} 先把已累积内容合并为一条快照 chunk 发出，再接实时增量（精确跳过已快照部分），
 *     避免海量小 chunk 重放压垮前端渲染</li>
 *     <li>终态（完成/失败）后流自动结束，终态事件保留一段时间供迟到的订阅者查询</li>
 * </ul>
 */
@Slf4j
public class GenerationTask {

    private final long appId;

    /**
     * replay-all：缓存生成全过程的所有 chunk，新订阅者从头重放
     */
    private final Sinks.Many<String> sink = Sinks.many().replay().all();

    /**
     * 已累积内容快照（与 chunkCount 一起保证快照/增量精确衔接）
     */
    private final StringBuilder snapshot = new StringBuilder();

    /**
     * 已发出的 chunk 计数（快照时记录，观察流跳过该数量的历史 chunk）
     */
    private final AtomicInteger chunkCount = new AtomicInteger(0);

    /**
     * 是否已进入终态（完成或失败）
     */
    private final AtomicBoolean terminal = new AtomicBoolean(false);

    /**
     * 进入终态的时间戳（用于过期清理）
     */
    private volatile long doneAt;

    public GenerationTask(long appId) {
        this.appId = appId;
    }

    public long getAppId() {
        return appId;
    }

    public boolean isTerminal() {
        return terminal.get();
    }

    public long getDoneAt() {
        return doneAt;
    }

    /**
     * 追加一个生成 chunk（终态后静默丢弃）
     */
    public void append(String chunk) {
        if (terminal.get()) {
            return;
        }
        synchronized (this) {
            if (terminal.get()) {
                return;
            }
            snapshot.append(chunk);
            chunkCount.incrementAndGet();
            Sinks.EmitResult result = sink.tryEmitNext(chunk);
            if (result.isFailure()) {
                log.warn("推送生成内容失败: appId={}, result={}", appId, result);
            }
        }
    }

    /**
     * 标记正常完成并结束流（幂等）
     */
    public void complete() {
        synchronized (this) {
            if (!terminal.compareAndSet(false, true)) {
                return;
            }
            doneAt = System.currentTimeMillis();
            sink.tryEmitComplete();
        }
    }

    /**
     * 标记失败并以错误结束流（幂等）
     */
    public void error(Throwable throwable) {
        synchronized (this) {
            if (!terminal.compareAndSet(false, true)) {
                return;
            }
            doneAt = System.currentTimeMillis();
            sink.tryEmitError(throwable);
        }
    }

    /**
     * 获取观察流：先发出一条合并快照 chunk（已累积的全部内容），
     * 再接实时增量流（精确跳过已快照的历史 chunk，保证不重不漏）。
     * 可多次订阅，互不影响（页面刷新 / 多标签页场景）。
     */
    public Flux<String> flux() {
        synchronized (this) {
            String snap = snapshot.toString();
            int emittedCount = chunkCount.get();
            return Flux.concat(
                    snap.isEmpty() ? Flux.empty() : Flux.just(snap),
                    sink.asFlux().skip(emittedCount)
            );
        }
    }
}
