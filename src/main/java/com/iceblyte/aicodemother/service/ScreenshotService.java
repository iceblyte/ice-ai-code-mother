package com.iceblyte.aicodemother.service;

/**
 * 截图服务
 */
public interface ScreenshotService {

    /**
     * 生成并上传截图
     *
     * @param webUrl 网页URL
     * @return 截图的访问URL
     */
    String generateAndUploadScreenshot(String webUrl);
}
