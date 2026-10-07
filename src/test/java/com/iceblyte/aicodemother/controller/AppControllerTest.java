package com.iceblyte.aicodemother.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.iceblyte.aicodemother.common.SseErrorEventUtils;
import com.iceblyte.aicodemother.core.generation.GenerationProgressManager;
import com.iceblyte.aicodemother.exception.BusinessException;
import com.iceblyte.aicodemother.exception.ErrorCode;
import com.iceblyte.aicodemother.model.entity.User;
import com.iceblyte.aicodemother.service.AppService;
import com.iceblyte.aicodemother.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockHttpServletRequest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * AppController.chatToGenCode 单元测试（不依赖 Spring 上下文 / 数据库 / AI 服务）
 * 核心断言：流内错误被转换为 business-error SSE 事件后流正常完成（前端能收到 done 帧）。
 */
@ExtendWith(MockitoExtension.class)
class AppControllerTest {

    private static final long APP_ID = 1L;

    @Mock
    private AppService appService;

    @Mock
    private UserService userService;

    @Mock
    private GenerationProgressManager generationProgressManager;

    @InjectMocks
    private AppController appController;

    private User stubLoginUser() {
        User user = User.builder().id(1L).build();
        Mockito.when(userService.getLoginUser(Mockito.any())).thenReturn(user);
        return user;
    }

    /**
     * 核心用例：流内错误 → business-error 帧（友好文案，不含内部异常信息）→ done 帧 → 流正常完成
     */
    @Test
    void streamErrorShouldEmitBusinessErrorThenDoneAndComplete() {
        User user = stubLoginUser();
        Mockito.when(appService.chatToGenCode(Mockito.eq(APP_ID), Mockito.eq("hi"), Mockito.eq(user)))
                .thenReturn(Flux.error(new RuntimeException("secret-internal")));

        Flux<ServerSentEvent<String>> flux =
                appController.chatToGenCode(APP_ID, "hi", new MockHttpServletRequest());

        StepVerifier.create(flux)
                .expectNextMatches(event -> {
                    if (!SseErrorEventUtils.BUSINESS_ERROR_EVENT.equals(event.event())) {
                        return false;
                    }
                    JSONObject data = JSONUtil.parseObj(event.data());
                    return ErrorCode.SYSTEM_ERROR.getCode() == data.getInt("code")
                            && SseErrorEventUtils.STREAM_ERROR_MESSAGE.equals(data.getStr("message"))
                            && !event.data().contains("secret-internal");
                })
                .expectNextMatches(event -> "done".equals(event.event()))
                .verifyComplete();
    }

    /**
     * 已发出的 chunk 在错误后保持完整：data 帧 → business-error → done
     */
    @Test
    void partialContentThenErrorShouldKeepEmittedChunks() {
        User user = stubLoginUser();
        Mockito.when(appService.chatToGenCode(Mockito.eq(APP_ID), Mockito.eq("hi"), Mockito.eq(user)))
                .thenReturn(Flux.concat(Flux.just("chunk1"), Flux.error(new RuntimeException("boom"))));

        Flux<ServerSentEvent<String>> flux =
                appController.chatToGenCode(APP_ID, "hi", new MockHttpServletRequest());

        StepVerifier.create(flux)
                .expectNextMatches(event -> event.event() == null
                        && "chunk1".equals(JSONUtil.parseObj(event.data()).getStr("d")))
                .expectNextMatches(event -> SseErrorEventUtils.BUSINESS_ERROR_EVENT.equals(event.event()))
                .expectNextMatches(event -> "done".equals(event.event()))
                .verifyComplete();
    }

    /**
     * 正常流零回归：只有 data 帧 + done 帧，不出现 business-error
     */
    @Test
    void normalStreamShouldEmitDataFramesAndDoneOnly() {
        User user = stubLoginUser();
        Mockito.when(appService.chatToGenCode(Mockito.eq(APP_ID), Mockito.eq("hi"), Mockito.eq(user)))
                .thenReturn(Flux.just("a", "b"));

        Flux<ServerSentEvent<String>> flux =
                appController.chatToGenCode(APP_ID, "hi", new MockHttpServletRequest());

        StepVerifier.create(flux)
                .expectNextMatches(event -> event.event() == null
                        && "a".equals(JSONUtil.parseObj(event.data()).getStr("d")))
                .expectNextMatches(event -> event.event() == null
                        && "b".equals(JSONUtil.parseObj(event.data()).getStr("d")))
                .expectNextMatches(event -> "done".equals(event.event()))
                .verifyComplete();
    }

    /**
     * 非法参数在流构建前同步抛 BusinessException（仍走 GlobalExceptionHandler，行为不变）
     */
    @Test
    void invalidParamsShouldThrowBusinessExceptionBeforeStream() {
        Assertions.assertThrows(BusinessException.class,
                () -> appController.chatToGenCode(0L, "hi", new MockHttpServletRequest()));
    }

    /**
     * 生成进度订阅：无任务 → 一帧 idle 事件后结束
     */
    @Test
    void progressIdleShouldEmitIdleEvent() {
        stubLoginUser();
        Mockito.when(generationProgressManager.subscribe(APP_ID))
                .thenReturn(new GenerationProgressManager.Subscription(
                        GenerationProgressManager.State.IDLE, Flux.empty()));
        Flux<ServerSentEvent<String>> flux =
                appController.subscribeGenerationProgress(APP_ID, new MockHttpServletRequest());
        StepVerifier.create(flux)
                .expectNextMatches(event -> "idle".equals(event.event()))
                .verifyComplete();
    }

    /**
     * 生成进度订阅：已完成（保留期）→ 一帧 finished 事件后结束（前端据此重拉历史）
     */
    @Test
    void progressFinishedShouldEmitFinishedEvent() {
        stubLoginUser();
        Mockito.when(generationProgressManager.subscribe(APP_ID))
                .thenReturn(new GenerationProgressManager.Subscription(
                        GenerationProgressManager.State.FINISHED, Flux.empty()));
        Flux<ServerSentEvent<String>> flux =
                appController.subscribeGenerationProgress(APP_ID, new MockHttpServletRequest());
        StepVerifier.create(flux)
                .expectNextMatches(event -> "finished".equals(event.event()))
                .verifyComplete();
    }

    /**
     * 生成进度订阅：进行中 → 快照 data 帧（{"d": ...} 与生成流同构）→ done 帧 → 完成
     */
    @Test
    void progressRunningShouldEmitSnapshotDataThenDone() {
        stubLoginUser();
        Mockito.when(generationProgressManager.subscribe(APP_ID))
                .thenReturn(new GenerationProgressManager.Subscription(
                        GenerationProgressManager.State.RUNNING, Flux.just("snapshot-content")));
        Flux<ServerSentEvent<String>> flux =
                appController.subscribeGenerationProgress(APP_ID, new MockHttpServletRequest());
        StepVerifier.create(flux)
                .expectNextMatches(event -> event.event() == null
                        && "snapshot-content".equals(JSONUtil.parseObj(event.data()).getStr("d")))
                .expectNextMatches(event -> "done".equals(event.event()))
                .verifyComplete();
    }
}
