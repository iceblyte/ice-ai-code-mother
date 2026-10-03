package com.iceblyte.aicodemother.core.builder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BuildProgressManager 单元测试（不依赖 Spring 上下文 / 数据库 / npm）
 */
class BuildProgressManagerTest {

    private static final long APP_ID = 100L;

    private static final String PROJECT_PATH = "/fake/vue_project_100";

    private VueProjectBuilder builder;

    private BuildProgressManager manager;

    @BeforeEach
    void setUp() {
        builder = Mockito.mock(VueProjectBuilder.class);
        manager = new BuildProgressManager(builder);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    /**
     * 无构建任务时订阅：应立即收到 idle 终态事件并结束
     */
    @Test
    void subscribeWithoutTaskShouldEmitIdle() {
        StepVerifier.create(manager.subscribe(APP_ID))
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_DONE.equals(msg.getType())
                        && BuildStatusEnum.IDLE.getValue().equals(msg.getStatus()))
                .verifyComplete();
    }

    /**
     * 构建成功：订阅者先重放最近 1 条事件，再实时接收进度，最后收到 done(success) 并结束
     */
    @Test
    void startBuildSuccessShouldPushProgressAndDone() throws InterruptedException {
        CountDownLatch prepareEmitted = new CountDownLatch(1);
        CountDownLatch releaseBuild = new CountDownLatch(1);
        Mockito.when(builder.buildProject(Mockito.eq(PROJECT_PATH), Mockito.any(BuildProgressListener.class)))
                .thenAnswer(invocation -> {
                    BuildProgressListener listener = invocation.getArgument(1);
                    listener.onPhase(BuildPhaseEnum.PREPARE, BuildStatusEnum.RUNNING, "正在检查项目目录...");
                    prepareEmitted.countDown();
                    // 阻塞构建线程，保证订阅发生在 PREPARE 事件之后、后续事件之前（确定性）
                    if (!releaseBuild.await(10, TimeUnit.SECONDS)) {
                        return false;
                    }
                    listener.onLog("fake npm log line");
                    listener.onPhase(BuildPhaseEnum.INSTALL, BuildStatusEnum.SUCCESS, "依赖安装完成");
                    return true;
                });

        manager.startBuild(APP_ID, PROJECT_PATH);
        Assertions.assertTrue(prepareEmitted.await(5, TimeUnit.SECONDS), "PREPARE 事件应已发出");

        StepVerifier.create(manager.subscribe(APP_ID))
                // 重放缓冲区中的最近 1 条事件
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_PROGRESS.equals(msg.getType())
                        && BuildPhaseEnum.PREPARE.getValue().equals(msg.getPhase())
                        && BuildStatusEnum.RUNNING.getValue().equals(msg.getStatus()))
                // 放行构建线程，后续事件实时推送
                .then(releaseBuild::countDown)
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_LOG.equals(msg.getType())
                        && "fake npm log line".equals(msg.getLine()))
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_PROGRESS.equals(msg.getType())
                        && BuildPhaseEnum.INSTALL.getValue().equals(msg.getPhase()))
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_DONE.equals(msg.getType())
                        && BuildStatusEnum.SUCCESS.getValue().equals(msg.getStatus()))
                .expectComplete().verify(Duration.ofSeconds(10));
    }

    /**
     * 构建失败：应收到 done(failed) 终态事件并结束
     */
    @Test
    void startBuildFailureShouldEmitFailedDone() {
        Mockito.when(builder.buildProject(Mockito.eq(PROJECT_PATH), Mockito.any(BuildProgressListener.class)))
                .thenAnswer(invocation -> {
                    BuildProgressListener listener = invocation.getArgument(1);
                    listener.onPhase(BuildPhaseEnum.PREPARE, BuildStatusEnum.FAILED, "项目目录不存在");
                    return false;
                });

        manager.startBuild(APP_ID, PROJECT_PATH);

        StepVerifier.create(manager.subscribe(APP_ID))
                .thenConsumeWhile(msg -> !BuildProgressMessage.TYPE_DONE.equals(msg.getType()))
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_DONE.equals(msg.getType())
                        && BuildStatusEnum.FAILED.getValue().equals(msg.getStatus()))
                .expectComplete().verify(Duration.ofSeconds(10));
    }

    /**
     * 构建抛出异常：也应收到 done(failed) 终态事件（保证流一定会结束）
     */
    @Test
    void startBuildExceptionShouldEmitFailedDone() {
        Mockito.when(builder.buildProject(Mockito.eq(PROJECT_PATH), Mockito.any(BuildProgressListener.class)))
                .thenThrow(new RuntimeException("boom"));

        manager.startBuild(APP_ID, PROJECT_PATH);

        StepVerifier.create(manager.subscribe(APP_ID))
                .thenConsumeWhile(msg -> !BuildProgressMessage.TYPE_DONE.equals(msg.getType()))
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_DONE.equals(msg.getType())
                        && BuildStatusEnum.FAILED.getValue().equals(msg.getStatus())
                        && msg.getMessage().contains("boom"))
                .expectComplete().verify(Duration.ofSeconds(10));
    }

    /**
     * 同一应用重复发起构建：旧构建应被取消（订阅旧任务收到 done(cancelled)），新构建正常完成
     */
    @Test
    void startBuildTwiceShouldCancelPreviousBuild() throws InterruptedException {
        CountDownLatch firstBuildStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstBuild = new CountDownLatch(1);
        AtomicReference<BuildProgressListener> firstListener = new AtomicReference<>();

        Mockito.when(builder.buildProject(Mockito.eq(PROJECT_PATH), Mockito.any(BuildProgressListener.class)))
                .thenAnswer(invocation -> {
                    BuildProgressListener listener = invocation.getArgument(1);
                    if (firstListener.compareAndSet(null, listener)) {
                        // 第一次构建：挂起等待，模拟耗时构建
                        firstBuildStarted.countDown();
                        releaseFirstBuild.await(10, TimeUnit.SECONDS);
                        return true;
                    }
                    // 第二次构建：直接成功
                    return true;
                });

        manager.startBuild(APP_ID, PROJECT_PATH);
        Assertions.assertTrue(firstBuildStarted.await(5, TimeUnit.SECONDS), "第一个构建任务应已启动");
        // 先订阅第一个任务（拿住旧任务的流），再触发第二次构建
        Flux<BuildProgressMessage> firstTaskFlux = manager.subscribe(APP_ID);
        manager.startBuild(APP_ID, PROJECT_PATH);

        StepVerifier.create(firstTaskFlux)
                .thenConsumeWhile(msg -> !BuildProgressMessage.TYPE_DONE.equals(msg.getType()))
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_DONE.equals(msg.getType())
                        && BuildStatusEnum.CANCELLED.getValue().equals(msg.getStatus()))
                .expectComplete().verify(Duration.ofSeconds(10));

        // 新构建正常完成
        StepVerifier.create(manager.subscribe(APP_ID))
                .thenConsumeWhile(msg -> !BuildProgressMessage.TYPE_DONE.equals(msg.getType()))
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_DONE.equals(msg.getType())
                        && BuildStatusEnum.SUCCESS.getValue().equals(msg.getStatus()))
                .expectComplete().verify(Duration.ofSeconds(10));

        // 释放第一个构建线程，避免线程泄漏
        releaseFirstBuild.countDown();
    }

    /**
     * 构建结束后（保留期内）重新订阅：应重放 done(success) 终态事件并结束
     */
    @Test
    void subscribeAfterDoneShouldReplayTerminalEvent() {
        Mockito.when(builder.buildProject(Mockito.eq(PROJECT_PATH), Mockito.any(BuildProgressListener.class)))
                .thenReturn(true);

        manager.startBuild(APP_ID, PROJECT_PATH);
        // 第一次订阅：等终态落定
        StepVerifier.create(manager.subscribe(APP_ID))
                .thenConsumeWhile(msg -> !BuildProgressMessage.TYPE_DONE.equals(msg.getType()))
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_DONE.equals(msg.getType()))
                .expectComplete().verify(Duration.ofSeconds(10));

        // 第二次订阅（模拟页面刷新后重连）：重放终态事件
        StepVerifier.create(manager.subscribe(APP_ID))
                .expectNextMatches(msg -> BuildProgressMessage.TYPE_DONE.equals(msg.getType())
                        && BuildStatusEnum.SUCCESS.getValue().equals(msg.getStatus()))
                .expectComplete().verify(Duration.ofSeconds(10));
    }
}
