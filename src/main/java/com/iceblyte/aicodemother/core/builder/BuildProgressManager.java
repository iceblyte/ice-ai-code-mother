package com.iceblyte.aicodemother.core.builder;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Vue 项目构建进度管理器
 * <p>
 * 职责：
 * <ul>
 *     <li>以 appId 为维度登记构建任务，构建在独立虚拟线程中异步执行，不阻塞调用方</li>
 *     <li>对外提供 {@link #subscribe(Long)} 进度流（SSE 推送用），支持中途订阅 / 刷新页面后重连（重放最近 1 条事件）</li>
 *     <li>同一 appId 新构建启动（或生成任务重新发起）时，自动取消旧构建，避免并发读写同一项目目录</li>
 *     <li>终态任务保留一段时间（默认 10 分钟）供迟到的订阅者查询结果，之后惰性清理</li>
 * </ul>
 */
@Slf4j
@Component
public class BuildProgressManager {

    /**
     * 终态任务保留时长（毫秒）：期间内重新订阅仍能拿到最终结果
     */
    private static final long TERMINAL_RETENTION_MS = 10 * 60 * 1000L;

    private final VueProjectBuilder vueProjectBuilder;

    /**
     * appId -> 构建任务
     */
    private final Map<Long, BuildTask> tasks = new ConcurrentHashMap<>();

    /**
     * 构建执行器：每个构建任务一个虚拟线程，互不阻塞
     */
    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("vue-build-", 0).factory());

    public BuildProgressManager(VueProjectBuilder vueProjectBuilder) {
        this.vueProjectBuilder = vueProjectBuilder;
    }

    /**
     * 异步启动构建（不阻塞调用线程）
     * <p>
     * 同一 appId 若已有进行中的构建，先取消旧构建再启动新构建。
     *
     * @param appId       应用 ID
     * @param projectPath Vue 项目根目录路径
     */
    public void startBuild(Long appId, String projectPath) {
        synchronized (this) {
            evictExpiredTasks();
            BuildTask previous = tasks.get(appId);
            if (previous != null) {
                // 新构建会重写项目文件，必须终止旧的 npm 进程，避免并发读写同一目录
                previous.cancel("检测到新的构建任务，当前构建已取消");
            }
            BuildTask task = new BuildTask(appId);
            tasks.put(appId, task);
            log.info("构建任务已提交: appId={}, projectPath={}", appId, projectPath);
            executor.execute(() -> executeBuild(task, projectPath));
        }
    }

    /**
     * 取消指定应用的进行中构建（若存在）
     *
     * @param appId 应用 ID
     */
    public void cancelBuild(Long appId) {
        synchronized (this) {
            BuildTask task = tasks.get(appId);
            if (task != null) {
                task.cancel("应用发起了新的生成任务，当前构建已取消");
            }
        }
    }

    /**
     * 订阅指定应用的构建进度流
     * <ul>
     *     <li>有进行中 / 保留期内的任务：先重放最近 1 条事件，再实时推送，直至终态后结束</li>
     *     <li>没有任务：立即返回一条 idle 终态事件并结束</li>
     * </ul>
     *
     * @param appId 应用 ID
     * @return 构建进度消息流
     */
    public Flux<BuildProgressMessage> subscribe(Long appId) {
        BuildTask task;
        synchronized (this) {
            evictExpiredTasks();
            task = tasks.get(appId);
        }
        if (task == null) {
            return Flux.just(BuildProgressMessage.idle(appId));
        }
        return task.flux();
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

    /**
     * 执行构建并推送终态（无论成功、失败还是异常都保证发出终态事件）
     */
    private void executeBuild(BuildTask task, String projectPath) {
        long appId = task.getAppId();
        try {
            boolean success = vueProjectBuilder.buildProject(projectPath, task.asListener());
            if (success) {
                task.emitDone(BuildStatusEnum.SUCCESS, "项目构建成功，预览已就绪");
                log.info("Vue 项目构建成功: appId={}", appId);
            } else {
                task.emitDone(BuildStatusEnum.FAILED, "项目构建失败，请查看构建日志");
                log.error("Vue 项目构建失败: appId={}", appId);
            }
        } catch (Throwable t) {
            log.error("Vue 项目构建异常: appId={}", appId, t);
            task.emitDone(BuildStatusEnum.FAILED, "构建异常: " + t.getMessage());
        }
    }

    /**
     * 服务关闭时：取消所有进行中的构建并关闭执行器
     */
    @PreDestroy
    public void shutdown() {
        synchronized (this) {
            for (BuildTask task : tasks.values()) {
                task.cancel("服务已关闭，构建终止");
            }
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
