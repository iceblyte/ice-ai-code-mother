package com.iceblyte.aicodemother.common;

import cn.hutool.json.JSONUtil;

import java.util.Map;

/**
 * SSE 业务错误事件工具：business-error 事件名与 data JSON 的单一事实源。
 *
 * 兼容性硬约束：前端 AppChatPage.vue 通过 addEventListener('business-error') 监听此事件，
 * 并对 data 做 JSON.parse 后读取 message 字段——改动事件名或字段结构会直接破坏前端错误展示。
 * 由 AppController（流内错误转事件）与 GlobalExceptionHandler（流开始前错误兜底）共同使用，保证两处格式一致。
 *
 * @author <a href="https://github.com/iceblyte">程序员iceblyte</a>
 */
public final class SseErrorEventUtils {

    /**
     * SSE 业务错误事件名（与前端监听名一致，勿改）
     */
    public static final String BUSINESS_ERROR_EVENT = "business-error";

    /**
     * 流内错误对前端的固定文案。
     * 不透传原始异常信息（防信息泄露），完整堆栈只记录到后端日志。
     */
    public static final String STREAM_ERROR_MESSAGE = "AI 生成失败，请稍后重试";

    private SseErrorEventUtils() {
    }

    /**
     * 构造 business-error 事件的 data JSON：{"error":true,"code":...,"message":...}
     *
     * @param errorCode    错误码（见 ErrorCode）
     * @param errorMessage 错误文案（对前端可见，勿传原始异常信息）
     * @return data 部分的 JSON 字符串
     */
    public static String buildBusinessErrorData(int errorCode, String errorMessage) {
        return JSONUtil.toJsonStr(Map.of(
                "error", true,
                "code", errorCode,
                "message", errorMessage
        ));
    }
}
