package com.iceblyte.aicodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AppVersionVO implements Serializable {

    /**
     * 版本标识，current 表示当前版本
     */
    private String versionKey;

    /**
     * 版本展示名称
     */
    private String versionName;

    /**
     * 版本创建时间
     */
    private LocalDateTime createTime;

    /**
     * 是否当前版本
     */
    private Boolean current;

    private static final long serialVersionUID = 1L;
}
