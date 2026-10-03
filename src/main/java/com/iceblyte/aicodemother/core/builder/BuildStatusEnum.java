package com.iceblyte.aicodemother.core.builder;

import lombok.Getter;

/**
 * Vue 项目构建状态枚举
 */
@Getter
public enum BuildStatusEnum {

    RUNNING("running", "进行中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    CANCELLED("cancelled", "已取消"),
    IDLE("idle", "无任务");

    private final String value;
    private final String text;

    BuildStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     */
    public static BuildStatusEnum getEnumByValue(String value) {
        for (BuildStatusEnum statusEnum : values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
