package com.iceblyte.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppVersionCompareRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 旧版本标识
     */
    private String oldVersionKey;

    /**
     * 新版本标识
     */
    private String newVersionKey;

    /**
     * 文件路径
     */
    private String filePath;

    private static final long serialVersionUID = 1L;
}
