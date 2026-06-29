package com.iceblyte.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用部署请求
 *
 * @author iceblyte
 */
@Data
public class AppDeployRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    private static final long serialVersionUID = 1L;
}
