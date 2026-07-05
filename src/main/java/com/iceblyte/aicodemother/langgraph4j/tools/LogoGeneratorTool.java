package com.iceblyte.aicodemother.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.iceblyte.aicodemother.langgraph4j.model.ImageResource;
import com.iceblyte.aicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import com.iceblyte.aicodemother.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Logo 图片生成工具
 */
@Slf4j
@Component
public class LogoGeneratorTool {

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.image-model:wan2.2-t2i-flash}")
    private String imageModel;

    @Resource
    private CosManager cosManager;

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            // 构建 Logo 设计提示词
            String logoPrompt = String.format("生成 Logo，Logo 中禁止包含任何文字！Logo 介绍：%s", description);
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(imageModel)
                    .prompt(logoPrompt)
                    .size("512*512")
                    .n(1) // 生成 1 张足够，因为 AI 不知道哪张最好
                    .build();
            ImageSynthesis imageSynthesis = new ImageSynthesis();
            ImageSynthesisResult result = imageSynthesis.call(param);
            if (result != null && result.getOutput() != null && result.getOutput().getResults() != null) {
                List<Map<String, String>> results = result.getOutput().getResults();
                for (Map<String, String> imageResult : results) {
                    String aliOssUrl = imageResult.get("url");
                    if (StrUtil.isBlank(aliOssUrl)) {
                        continue;
                    }
                    // 下载阿里云图片到本地临时文件
                    File tempLogoFile = downloadImageFromAliOss(aliOssUrl);
                    if (tempLogoFile == null || !tempLogoFile.exists() || tempLogoFile.length() == 0) {
                        log.warn("阿里云Logo图片下载失败，url:{}", aliOssUrl);
                        continue;
                    }
                    try {
                        // 上传至腾讯COS，路径统一放入/logo/目录
                        String fileName = tempLogoFile.getName();
                        String keyName = String.format("/logo/%s/%s",
                                RandomUtil.randomString(5), fileName);
                        String cosUrl = cosManager.uploadFile(keyName, tempLogoFile);
                        if (StrUtil.isNotBlank(cosUrl)) {
                            logoList.add(ImageResource.builder()
                                    .category(ImageCategoryEnum.LOGO)
                                    .description(description)
                                    .url(cosUrl)
                                    .build());
                        }
                    } finally {
                        // 无论上传成功失败，都清理本地临时文件
                        FileUtil.del(tempLogoFile);
                    }
                }
            }
        } catch (Exception e) {
            log.error("生成 Logo 失败: {}", e.getMessage(), e);
        }
        return logoList;
    }

    /**
     * 从阿里云OSS远程链接下载图片到本地临时文件
     * @param aliOssUrl 阿里云图片链接
     * @return 本地临时图片文件
     */
    private File downloadImageFromAliOss(String aliOssUrl) {
        // 创建png临时文件存储logo
        File tempImage = FileUtil.createTempFile("logo_temp_", ".png", true);
        // hutool HttpUtil 下载远程文件到本地File
        long downloadBytes = HttpUtil.downloadFile(aliOssUrl, tempImage);
        // 校验下载结果，避免空白文件
        if (downloadBytes <= 0) {
            log.warn("远程 Logo 图片下载字节数为0，链接：{}", aliOssUrl);
            FileUtil.del(tempImage);
            return null;
        }
        return tempImage;
    }
}
