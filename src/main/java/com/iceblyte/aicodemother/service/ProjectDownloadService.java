package com.iceblyte.aicodemother.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 项目下载服务
 */
public interface ProjectDownloadService {

    /**
     * 将项目下载为 zip 压缩包
     *
     * @param projectPath      项目路径
     * @param downloadFileName 下载文件名
     * @param response         HttpServletResponse（自行指定 HTTP 响应头的内容）
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
