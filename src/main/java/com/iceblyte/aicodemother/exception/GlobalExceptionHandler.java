package com.iceblyte.aicodemother.exception;

import com.iceblyte.aicodemother.common.BaseResponse;
import com.iceblyte.aicodemother.common.ResultUtils;
import com.iceblyte.aicodemother.common.SseErrorEventUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        // 尝试处理 SSE 请求
        if (handleSseError(e.getCode(), e.getMessage())) {
            return null;
        }
        // 对于普通请求，返回标准 JSON 响应
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        // 尝试处理 SSE 请求
        if (handleSseError(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误")) {
            return null;
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * 处理SSE请求的错误响应
     *
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @return true表示是SSE请求并已处理，false表示不是SSE请求
     */
    private boolean handleSseError(int errorCode, String errorMessage) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            return false;
        }
        // 判断是否是SSE请求（通过Accept头或URL路径）
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        if ((accept != null && accept.contains("text/event-stream")) ||
                uri.contains("/chat/gen/code")) {
            // 流已开始推流后响应必然已提交：此时任何写入都会抛 IllegalStateException 导致处理器自爆
            // （并触发 /error 兜底二次报错），只记录日志并放弃写响应。
            // 流内错误由 AppController 的 onErrorResume 以 SSE 事件形式下发，不经此路径。
            if (response.isCommitted()) {
                log.warn("SSE 响应已提交，无法写入错误事件, uri: {}", uri);
                return true;
            }
            try {
                // 设置SSE响应头
                response.setContentType("text/event-stream");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Cache-Control", "no-cache");
                response.setHeader("Connection", "keep-alive");
                // 构造错误消息的SSE格式（与 AppController 流内错误事件同构，前端契约见 SseErrorEventUtils）
                String errorJson = SseErrorEventUtils.buildBusinessErrorData(errorCode, errorMessage);
                // 使用 getOutputStream，与 SSE 推流桥（ReactiveTypeHandler）保持一致的取用方式，
                // 避免 getWriter / getOutputStream 双取用冲突
                ServletOutputStream outputStream = response.getOutputStream();
                // 发送业务错误事件（避免与标准error事件冲突）
                outputStream.write(("event: business-error\ndata: " + errorJson + "\n\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                // 发送结束事件
                outputStream.write("event: done\ndata: {}\n\n".getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                // 表示已处理SSE请求
                return true;
            } catch (Exception e) {
                log.error("Failed to write SSE error response", e);
                // 即使写入失败，也表示这是SSE请求
                return true;
            }
        }
        return false;
    }
}
