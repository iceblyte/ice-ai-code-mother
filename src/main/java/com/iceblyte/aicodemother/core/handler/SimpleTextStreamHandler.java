package com.iceblyte.aicodemother.core.handler;

import com.iceblyte.aicodemother.common.SseErrorEventUtils;
import com.iceblyte.aicodemother.model.entity.User;
import com.iceblyte.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.iceblyte.aicodemother.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 简单文本流处理器
 * 处理 HTML 和 MULTI_FILE 类型的流式响应
 */
@Slf4j
public class SimpleTextStreamHandler {

    /**
     * 处理传统流（HTML, MULTI_FILE）
     * 直接收集完整的文本响应
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> {
                    // 收集AI响应内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    // 原始异常只记录到日志（完整堆栈），库与前端只见固定友好文案，防止信息泄露
                    log.error("AI 流式回复失败, appId: {}", appId, error);
                    try {
                        // 以 ERROR 类型入库：loadChatHistoryToMemory 白名单只回灌 USER/AI，错误消息不会污染 AI 记忆
                        chatHistoryService.addChatMessage(appId, SseErrorEventUtils.STREAM_ERROR_MESSAGE,
                                ChatHistoryMessageTypeEnum.ERROR.getValue(), loginUser.getId());
                    } catch (Exception e) {
                        // 存库失败不能再向下游抛二级异常，避免掩盖原始错误信号
                        log.error("错误消息入库失败, appId: {}", appId, e);
                    }
                });
    }
}
