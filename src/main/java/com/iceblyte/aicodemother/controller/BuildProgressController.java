package com.iceblyte.aicodemother.controller;

import cn.hutool.json.JSONUtil;
import com.iceblyte.aicodemother.constant.UserConstant;
import com.iceblyte.aicodemother.core.builder.BuildProgressManager;
import com.iceblyte.aicodemother.core.builder.BuildProgressMessage;
import com.iceblyte.aicodemother.exception.BusinessException;
import com.iceblyte.aicodemother.exception.ErrorCode;
import com.iceblyte.aicodemother.exception.ThrowUtils;
import com.iceblyte.aicodemother.model.entity.App;
import com.iceblyte.aicodemother.model.entity.User;
import com.iceblyte.aicodemother.service.AppService;
import com.iceblyte.aicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Vue 项目构建进度控制器
 * <p>
 * 提供构建进度 SSE 订阅接口。前端在 AI 生成完成（或页面加载）后订阅本接口，
 * 实时接收异步构建的阶段进度与日志，收到 done 事件后流自动结束。
 */
@Slf4j
@RestController
@RequestMapping("/build")
public class BuildProgressController {

    /**
     * SSE 心跳间隔（毫秒）：长时间无事件时保持连接活跃，防止代理 / 容器超时断开
     */
    private static final long HEARTBEAT_INTERVAL_MS = 15 * 1000L;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private BuildProgressManager buildProgressManager;

    /**
     * 订阅应用构建进度（SSE 流式响应）
     * <p>
     * 协议：默认事件承载 progress / log 消息（JSON）；终态使用名为 done 的命名事件，
     * 随后服务端结束流。无构建任务时立即推送一条 idle 终态事件并结束。
     *
     * @param appId   应用 ID
     * @param request 请求对象（用于获取登录用户）
     * @return 构建进度事件流
     */
    @GetMapping(value = "/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> subscribeBuildProgress(@RequestParam Long appId,
                                                                HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 无效");
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3. 查询应用并校验权限：仅应用创建者和管理员可以查看构建进度
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId())
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用构建进度");
        }
        // 4. 订阅进度流：progress / log 走默认事件，终态走 done 命名事件
        Flux<ServerSentEvent<String>> dataFlux = buildProgressManager.subscribe(appId)
                .map(msg -> {
                    ServerSentEvent.Builder<String> builder = ServerSentEvent.builder(JSONUtil.toJsonStr(msg));
                    if (BuildProgressMessage.TYPE_DONE.equals(msg.getType())) {
                        builder.event("done");
                    }
                    return builder.build();
                });
        // 5. 合并心跳（SSE 注释行，EventSource 会自动忽略），收到 done 事件后整体结束
        Flux<ServerSentEvent<String>> heartbeatFlux = Flux.interval(Duration.ofMillis(HEARTBEAT_INTERVAL_MS))
                .map(tick -> ServerSentEvent.<String>builder().comment("keep-alive").build());
        return dataFlux.mergeWith(heartbeatFlux)
                .takeUntil(event -> "done".equals(event.event()));
    }
}
