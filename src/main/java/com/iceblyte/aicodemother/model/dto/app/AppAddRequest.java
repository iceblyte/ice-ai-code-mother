package com.iceblyte.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加应用请求
 *
 * @author iceblyte
 */

@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    private static final long serialVersionUID = 1L;
}
