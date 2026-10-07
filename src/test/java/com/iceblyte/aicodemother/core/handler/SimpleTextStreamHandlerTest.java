package com.iceblyte.aicodemother.core.handler;

import com.iceblyte.aicodemother.common.SseErrorEventUtils;
import com.iceblyte.aicodemother.model.entity.User;
import com.iceblyte.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.iceblyte.aicodemother.service.ChatHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * SimpleTextStreamHandler 单元测试（不依赖 Spring 上下文 / 数据库 / AI 服务）
 */
class SimpleTextStreamHandlerTest {

    private static final long APP_ID = 1L;

    private ChatHistoryService chatHistoryService;

    private SimpleTextStreamHandler handler;

    private User loginUser;

    @BeforeEach
    void setUp() {
        chatHistoryService = Mockito.mock(ChatHistoryService.class);
        handler = new SimpleTextStreamHandler();
        loginUser = User.builder().id(1L).build();
    }

    /**
     * 正常流：chunk 透传，完成后完整 AI 回复以 AI 类型入库（核心功能零回归）
     */
    @Test
    void completeShouldSaveFullAiMessage() {
        Flux<String> result = handler.handle(Flux.just("你好", "世界"), chatHistoryService, APP_ID, loginUser);
        StepVerifier.create(result)
                .expectNext("你好", "世界")
                .verifyComplete();
        Mockito.verify(chatHistoryService).addChatMessage(APP_ID, "你好世界",
                ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
    }

    /**
     * 错误流：入库 ERROR 类型 + 固定友好文案，原始异常信息不得入库
     */
    @Test
    void errorShouldSaveErrorTypeMessageWithFriendlyText() {
        Flux<String> result = handler.handle(
                Flux.error(new RuntimeException("secret-internal-detail")),
                chatHistoryService, APP_ID, loginUser);
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        Mockito.verify(chatHistoryService).addChatMessage(APP_ID,
                SseErrorEventUtils.STREAM_ERROR_MESSAGE,
                ChatHistoryMessageTypeEnum.ERROR.getValue(), loginUser.getId());
        Mockito.verify(chatHistoryService, Mockito.never()).addChatMessage(Mockito.anyLong(),
                Mockito.contains("secret-internal-detail"), Mockito.anyString(), Mockito.anyLong());
    }

    /**
     * 存库失败不能掩盖原始错误：下游收到的必须仍是原始异常
     */
    @Test
    void saveFailureShouldNotMaskOriginalError() {
        Mockito.when(chatHistoryService.addChatMessage(Mockito.anyLong(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyLong()))
                .thenThrow(new RuntimeException("db down"));
        Flux<String> result = handler.handle(
                Flux.error(new RuntimeException("ai fail")),
                chatHistoryService, APP_ID, loginUser);
        StepVerifier.create(result)
                .expectErrorMatches(error -> "ai fail".equals(error.getMessage()))
                .verify();
    }
}
