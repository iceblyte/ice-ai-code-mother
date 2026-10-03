package com.iceblyte.aicodemother.core.builder;

import lombok.Data;

/**
 * 构建进度消息（SSE 推送协议）
 * <p>
 * type 取值：
 * <ul>
 *     <li>progress - 阶段进度变更（含 phase + status）</li>
 *     <li>log - 构建过程日志行（含 line）</li>
 *     <li>done - 终态事件（含整体 status：success / failed / cancelled / idle），此后流结束</li>
 * </ul>
 */
@Data
public class BuildProgressMessage {

    /**
     * 消息类型：阶段进度
     */
    public static final String TYPE_PROGRESS = "progress";

    /**
     * 消息类型：构建日志
     */
    public static final String TYPE_LOG = "log";

    /**
     * 消息类型：终态（流结束）
     */
    public static final String TYPE_DONE = "done";

    /**
     * 消息类型
     */
    private String type;

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 构建阶段（progress / log 消息有效）
     */
    private String phase;

    /**
     * 状态（progress 消息为 running / success / failed；done 消息为 success / failed / cancelled / idle）
     */
    private String status;

    /**
     * 人类可读的进度描述
     */
    private String message;

    /**
     * 构建日志原文（log 消息有效）
     */
    private String line;

    /**
     * 事件时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 构造阶段进度消息
     */
    public static BuildProgressMessage progress(long appId, BuildPhaseEnum phase, BuildStatusEnum status, String message) {
        BuildProgressMessage msg = new BuildProgressMessage();
        msg.setType(TYPE_PROGRESS);
        msg.setAppId(appId);
        msg.setPhase(phase.getValue());
        msg.setStatus(status.getValue());
        msg.setMessage(message);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    /**
     * 构造构建日志消息
     */
    public static BuildProgressMessage log(long appId, String line) {
        BuildProgressMessage msg = new BuildProgressMessage();
        msg.setType(TYPE_LOG);
        msg.setAppId(appId);
        msg.setLine(line);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    /**
     * 构造终态消息
     */
    public static BuildProgressMessage done(long appId, BuildStatusEnum status, String message) {
        BuildProgressMessage msg = new BuildProgressMessage();
        msg.setType(TYPE_DONE);
        msg.setAppId(appId);
        msg.setStatus(status.getValue());
        msg.setMessage(message);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    /**
     * 构造"当前无构建任务"终态消息
     */
    public static BuildProgressMessage idle(long appId) {
        return done(appId, BuildStatusEnum.IDLE, "当前没有进行中的构建任务");
    }
}
