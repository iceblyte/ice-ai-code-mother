package com.iceblyte.aicodemother.core.generation;

import com.iceblyte.aicodemother.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GenerationProgressManager 单元测试（不依赖 Spring 上下文 / 数据库 / AI 服务）
 * 核心验证：生成由服务端内部订阅独立驱动（无 SSE 观察者也执行完毕），回看者先收快照再接实时增量。
 */
class GenerationProgressManagerTest {

    private static final long APP_ID = 1L;

    private GenerationProgressManager manager;

    @BeforeEach
    void setUp() {
        manager = new GenerationProgressManager();
    }

    /**
     * 核心语义：即使没有任何 SSE 观察者订阅，源 Flux 也被服务端内部订阅驱动执行完毕
     * （离开页面不中断生成、对话历史照常入库的前提）
     */
    @Test
    void trackShouldDriveSourceWithoutAnyObserver() {
        List<String> consumed = new CopyOnWriteArrayList<>();
        Flux<String> source = Flux.just("a", "b", "c").doOnNext(consumed::add);
        Flux<String> observer = manager.track(APP_ID, source);
        // 不订阅 observer，源也应已被内部驱动消费
        Assertions.assertEquals(List.of("a", "b", "c"), consumed);
        // 迟到的观察者：已累积内容合并为一条快照 chunk
        StepVerifier.create(observer)
                .expectNext("abc")
                .verifyComplete();
    }

    /**
     * 生成中途回看（subscribe 路径）：先收到合并快照（已生成部分），再实时接收后续 chunk，衔接精确不重不漏
     */
    @Test
    void subscribeShouldReplaySnapshotThenLiveChunks() {
        Sinks.Many<String> upstream = Sinks.many().unicast().onBackpressureBuffer();
        manager.track(APP_ID, upstream.asFlux());
        upstream.tryEmitNext("a");
        upstream.tryEmitNext("b");
        // 此刻（已生成 "ab"）通过 subscribe 回看：快照 "ab" + 后续增量
        GenerationProgressManager.Subscription sub = manager.subscribe(APP_ID);
        Assertions.assertEquals(GenerationProgressManager.State.RUNNING, sub.state());
        StepVerifier.create(sub.flux())
                .then(() -> {
                    upstream.tryEmitNext("c");
                    upstream.tryEmitComplete();
                })
                .expectNext("ab", "c")
                .verifyComplete();
    }

    /**
     * 同一应用已有进行中的生成时，拒绝新任务（避免并发生成互相覆盖）
     */
    @Test
    void trackShouldRejectConcurrentGeneration() {
        Sinks.Many<String> upstream = Sinks.many().unicast().onBackpressureBuffer();
        manager.track(APP_ID, upstream.asFlux());
        Assertions.assertThrows(BusinessException.class,
                () -> manager.track(APP_ID, Flux.just("x")));
    }

    /**
     * 源 Flux 失败：错误传播给观察者（由 Controller 层 onErrorResume 统一转 business-error 事件），
     * 任务进入终态
     */
    @Test
    void errorShouldPropagateToObserver() {
        Flux<String> observer = manager.track(APP_ID, Flux.error(new RuntimeException("boom")));
        StepVerifier.create(observer)
                .expectErrorMessage("boom")
                .verify();
        // 失败终态后订阅：FINISHED（前端重拉历史即可看到错误占位消息）
        Assertions.assertEquals(GenerationProgressManager.State.FINISHED,
                manager.subscribe(APP_ID).state());
    }

    /**
     * 无生成任务时订阅：IDLE
     */
    @Test
    void subscribeWithoutTaskShouldBeIdle() {
        GenerationProgressManager.Subscription sub = manager.subscribe(999L);
        Assertions.assertEquals(GenerationProgressManager.State.IDLE, sub.state());
        StepVerifier.create(sub.flux()).verifyComplete();
    }

    /**
     * 生成已完成（保留期内）订阅：FINISHED（不重放 chunk，前端重新拉历史）
     */
    @Test
    void finishedTaskSubscribeShouldBeFinished() {
        manager.track(APP_ID, Flux.just("a"));
        // 同步源已驱动至完成
        GenerationProgressManager.Subscription sub = manager.subscribe(APP_ID);
        Assertions.assertEquals(GenerationProgressManager.State.FINISHED, sub.state());
    }

    /**
     * 生成进行中订阅：RUNNING 且观察流可用
     */
    @Test
    void runningTaskSubscribeShouldBeRunning() {
        Sinks.Many<String> upstream = Sinks.many().unicast().onBackpressureBuffer();
        manager.track(APP_ID, upstream.asFlux());
        upstream.tryEmitNext("x");
        GenerationProgressManager.Subscription sub = manager.subscribe(APP_ID);
        Assertions.assertEquals(GenerationProgressManager.State.RUNNING, sub.state());
        StepVerifier.create(sub.flux())
                .then(() -> upstream.tryEmitComplete())
                .expectNext("x")
                .verifyComplete();
    }
}
