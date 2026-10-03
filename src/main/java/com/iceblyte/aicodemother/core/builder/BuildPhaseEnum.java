package com.iceblyte.aicodemother.core.builder;

import lombok.Getter;

/**
 * Vue 项目构建阶段枚举
 */
@Getter
public enum BuildPhaseEnum {

    PREPARE("prepare", "准备"),
    INSTALL("install", "安装依赖"),
    BUILD("build", "构建项目");

    private final String value;
    private final String text;

    BuildPhaseEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     */
    public static BuildPhaseEnum getEnumByValue(String value) {
        for (BuildPhaseEnum phaseEnum : values()) {
            if (phaseEnum.getValue().equals(value)) {
                return phaseEnum;
            }
        }
        return null;
    }
}
