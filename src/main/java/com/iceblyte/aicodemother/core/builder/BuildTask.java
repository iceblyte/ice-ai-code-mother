package com.iceblyte.aicodemother.core.builder;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一次构建任务（按 appId 维度）
 * <p>
 * 内部持有一个 replay-latest 的 Sinks：
 * <ul>
 *     <li>构建线程实时推送进度 / 日志事件</li>
 *     <li>任意时刻订阅都能先拿到最近 1 条事件（覆盖页面刷新、迟到订阅场景）</li>
 *     <li>终态事件发出后流自动 complete，之后到达的订阅者也会先收到终态再结束</li>
 * </ul>
 */
@Slf4j
public class BuildTask {

    private final long appId;

    /**
     * replay-latest：新订阅者先重放最近 1 条事件，再接入实时流
     */
    private final Sinks.Many<BuildProgressMessage> sink = Sinks.many().replay().latest();

    /**
     * 是否已进入终态（done 事件已发出或任务已被取消）
     */
    private final AtomicBoolean terminal = new AtomicBoolean(false);

    /**
     * 当前构建子进程（用于取消时强制终止）
     */
    private volatile Process currentProcess;

    /**
     * 进入终态的时间戳（用于过期清理）
     */
    private volatile long doneAt;

    public BuildTask(long appId) {
        this.appId = appId;
    }

    public long getAppId() {
        return appId;
    }

    /**
     * 获取进度流（可多订阅者，重放最近 1 条事件）
     */
    public Flux<BuildProgressMessage> flux() {
        return sink.asFlux();
    }

    public boolean isTerminal() {
        return terminal.get();
    }

    public long getDoneAt() {
        return doneAt;
    }

    /**
     * 推送阶段进度事件（终态后静默丢弃）
     */
    public void emitProgress(BuildPhaseEnum phase, BuildStatusEnum status, String message) {
        if (terminal.get()) {
            return;
        }
        synchronized (this) {
            if (terminal.get()) {
                return;
            }
            Sinks.EmitResult result = sink.tryEmitNext(BuildProgressMessage.progress(appId, phase, status, message));
            if (result.isFailure()) {
                log.warn("推送构建进度事件失败: appId={}, result={}", appId, result);
            }
        }
    }

    /**
     * 推送构建日志事件（终态后静默丢弃）
     */
    public void emitLog(String line) {
        if (terminal.get()) {
            return;
        }
        synchronized (this) {
            if (terminal.get()) {
                return;
            }
            Sinks.EmitResult result = sink.tryEmitNext(BuildProgressMessage.log(appId, line));
            if (result.isFailure()) {
                log.warn("推送构建日志事件失败: appId={}, result={}", appId, result);
            }
        }
    }

    /**
     * 推送终态事件并结束流（幂等：已终态则忽略）
     */
    public void emitDone(BuildStatusEnum status, String message) {
        synchronized (this) {
            if (terminal.get()) {
                return;
            }
            terminal.set(true);
            doneAt = System.currentTimeMillis();
            Sinks.EmitResult result = sink.tryEmitNext(BuildProgressMessage.done(appId, status, message));
            if (result.isFailure()) {
                log.warn("推送构建终态事件失败: appId={}, result={}", appId, result);
            }
            sink.tryEmitComplete();
        }
    }

    /**
     * 取消构建：终止子进程、推送 cancelled 终态并结束流
     *
     * @param reason 取消原因（推送给订阅者）
     * @return 是否真正执行了取消（已终态的任务返回 false）
     */
    public boolean cancel(String reason) {
        synchronized (this) {
            if (terminal.get()) {
                return false;
            }
            terminal.set(true);
            doneAt = System.currentTimeMillis();
            Process process = currentProcess;
            if (process != null && process.isAlive()) {
                log.info("强制终止构建进程: appId={}", appId);
                process.destroyForcibly();
            }
            log.info("构建任务已取消: appId={}, reason={}", appId, reason);
            Sinks.EmitResult result = sink.tryEmitNext(BuildProgressMessage.done(appId, BuildStatusEnum.CANCELLED, reason));
            if (result.isFailure()) {
                log.warn("推送取消事件失败: appId={}, result={}", appId, result);
            }
            sink.tryEmitComplete();
            return true;
        }
    }

    /**
     * 将本任务适配为构建进度监听器（供 VueProjectBuilder 回调）
     */
    public BuildProgressListener asListener() {
        return new BuildProgressListener() {
            @Override
            public void onPhase(BuildPhaseEnum phase, BuildStatusEnum status, String message) {
                emitProgress(phase, status, message);
            }

            @Override
            public void onLog(String line) {
                emitLog(line);
            }

            @Override
            public void onProcessStarted(Process process) {
                synchronized (BuildTask.this) {
                    if (terminal.get()) {
                        // 取消恰好发生在"上一个命令结束、下一个命令启动"的间隙，直接终止新进程
                        process.destroyForcibly();
                        return;
                    }
                    currentProcess = process;
                }
            }

            @Override
            public boolean isCancelled() {
                return terminal.get();
            }
        };
    }
}
