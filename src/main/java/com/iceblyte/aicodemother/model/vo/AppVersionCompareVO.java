package com.iceblyte.aicodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AppVersionCompareVO implements Serializable {

    /**
     * 可对比文件路径列表
     */
    private List<String> fileList;

    /**
     * 当前对比的文件路径
     */
    private String filePath;

    /**
     * 旧版本文件内容
     */
    private String oldContent;

    /**
     * 新版本文件内容
     */
    private String newContent;

    /**
     * 新增行数
     */
    private Integer additions;

    /**
     * 删除行数
     */
    private Integer removals;

    private static final long serialVersionUID = 1L;
}
