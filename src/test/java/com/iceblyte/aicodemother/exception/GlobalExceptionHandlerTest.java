package com.iceblyte.aicodemother.exception;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.iceblyte.aicodemother.common.BaseResponse;
import com.iceblyte.aicodemother.common.SseErrorEventUtils;
import jakarta.servlet.ServletOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * GlobalExceptionHandler 单元测试（不依赖 Spring 上下文 / 数据库）
 * 覆盖 SSE 错误兜底路径：流开始前正常写帧、流已开始（响应已提交 / OutputStream 已被取用）时静默放弃，
 * 修复此前处理器自身抛 IllegalStateException 导致连锁报错的问题。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private MockHttpServletRequest sseRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/event-stream");
        request.setRequestURI("/api/app/chat/gen/code");
        return request;
    }

    private MockHttpServletResponse bind(MockHttpServletRequest request) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        return response;
    }

    /**
     * 流开始前的 SSE 异常：写出 business-error + done 帧，Content-Type 正确，返回 null
     */
    @Test
    void sseRequestBeforeStreamStartShouldWriteBusinessErrorAndDoneFrames() throws IOException {
        MockHttpServletResponse response = bind(sseRequest());
        BaseResponse<?> result = handler.businessExceptionHandler(
                new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID无效"));
        Assertions.assertNull(result, "SSE 请求应返回 null（已手工写帧）");
        String content = response.getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertTrue(content.contains("event: business-error"), "缺少 business-error 帧: " + content);
        // data 必须是含 message 的合法 JSON（前端 JSON.parse(data).message 契约）
        String dataLine = content.lines()
                .filter(line -> line.startsWith("data: "))
                .findFirst().orElseThrow(() -> new AssertionError("缺少 data 行"));
        JSONObject data = JSONUtil.parseObj(dataLine.substring("data: ".length()));
        Assertions.assertEquals("应用ID无效", data.getStr("message"));
        Assertions.assertTrue(content.contains("event: done"), "缺少 done 帧: " + content);
        Assertions.assertTrue(response.getContentType().startsWith("text/event-stream"),
                "Content-Type 应为 text/event-stream: " + response.getContentType());
    }

    /**
     * 响应已提交（流已开始）：静默放弃写帧且不抛异常——修复前此场景会写半截或自爆
     */
    @Test
    void committedResponseShouldNotThrowAndWriteNothing() {
        MockHttpServletResponse response = bind(sseRequest());
        response.setCommitted(true);
        Assertions.assertDoesNotThrow(() -> handler.businessExceptionHandler(
                new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误")));
        Assertions.assertEquals(0, response.getContentAsByteArray().length,
                "已提交的响应不应再写入任何内容");
    }

    /**
     * OutputStream 已被 SSE 桥取用（流已推流）：getWriter 路径会抛 IllegalStateException，
     * 修复后必须静默——这是线上连锁异常 getOutputStream() has already been called 的测试级复现
     */
    @Test
    void outputStreamAlreadyUsedShouldNotThrow() throws IOException {
        MockHttpServletResponse response = bind(sseRequest());
        // 模拟 SSE 桥已开始推流：取用 OutputStream 并 flush（响应随之 committed）
        response.getOutputStream().write("data: {}\n\n".getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
        Assertions.assertDoesNotThrow(() ->
                handler.runtimeExceptionHandler(new RuntimeException("db error")));
    }

    /**
     * 获取 OutputStream 本身抛异常时也被兜底，不向外传播
     */
    @Test
    void outputStreamFailureShouldNotThrow() {
        MockHttpServletRequest request = sseRequest();
        MockHttpServletResponse response = new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                throw new IllegalStateException("getOutputStream() has already been called for this response");
            }
        };
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        Assertions.assertDoesNotThrow(() -> handler.businessExceptionHandler(
                new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误")));
    }

    /**
     * Accept 头不是 SSE 但 URI 匹配时仍走 SSE 兜底（URI 判断不回归）
     */
    @Test
    void uriMatchShouldTriggerSseHandling() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/plain");
        request.setRequestURI("/api/app/chat/gen/code");
        MockHttpServletResponse response = bind(request);
        BaseResponse<?> result = handler.businessExceptionHandler(
                new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID无效"));
        Assertions.assertNull(result);
        Assertions.assertTrue(response.getContentAsString(StandardCharsets.UTF_8)
                .contains("event: business-error"));
    }

    /**
     * 非 SSE 请求：返回标准 BaseResponse，响应体无 SSE 内容（普通请求零回归）
     */
    @Test
    void nonSseRequestShouldReturnBaseResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json");
        request.setRequestURI("/api/user/get/login");
        MockHttpServletResponse response = bind(request);
        BaseResponse<?> result = handler.businessExceptionHandler(
                new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误"));
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), result.getCode());
        Assertions.assertEquals(0, response.getContentAsByteArray().length);
    }

    /**
     * RuntimeException 在 SSE 请求下：帧内 message 为「系统错误」，不泄露内部异常信息
     */
    @Test
    void runtimeExceptionOnSseShouldMaskInternalMessage() throws IOException {
        MockHttpServletResponse response = bind(sseRequest());
        BaseResponse<?> result = handler.runtimeExceptionHandler(new RuntimeException("internal-secret-detail"));
        Assertions.assertNull(result);
        String content = response.getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertFalse(content.contains("internal-secret-detail"), "帧内容泄露了内部异常信息");
        String dataLine = content.lines()
                .filter(line -> line.startsWith("data: "))
                .findFirst().orElseThrow(() -> new AssertionError("缺少 data 行"));
        JSONObject data = JSONUtil.parseObj(dataLine.substring("data: ".length()));
        Assertions.assertEquals("系统错误", data.getStr("message"));
    }
}
