package com.iceblyte.aicodemother.common;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * SseErrorEventUtils 单元测试（不依赖 Spring 上下文 / 数据库）
 * 断言 business-error 事件与前端 AppChatPage.vue 的契约不被破坏。
 */
class SseErrorEventUtilsTest {

    /**
     * data 必须是合法 JSON 且含 error/code/message 字段——前端 JSON.parse(data) 后取 message 字段
     */
    @Test
    void buildBusinessErrorDataShouldBeValidJsonWithRequiredFields() {
        String data = SseErrorEventUtils.buildBusinessErrorData(50000, "生成过程中出现错误");
        JSONObject obj = JSONUtil.parseObj(data);
        Assertions.assertTrue(obj.getBool("error"));
        Assertions.assertEquals(50000, obj.getInt("code"));
        Assertions.assertEquals("生成过程中出现错误", obj.getStr("message"));
    }

    /**
     * message 中的换行必须被 JSON 转义——裸换行会破坏 SSE 帧结构
     */
    @Test
    void buildBusinessErrorDataShouldEscapeNewlines() {
        String data = SseErrorEventUtils.buildBusinessErrorData(50000, "第一行\n第二行");
        Assertions.assertFalse(data.contains("\n"), "SSE data 中出现裸换行，帧结构被破坏");
        JSONObject obj = JSONUtil.parseObj(data);
        Assertions.assertEquals("第一行\n第二行", obj.getStr("message"));
    }

    /**
     * 常量必须与前端契约一致：事件名是前端 addEventListener 的硬编码监听名；固定文案不允许含换行
     */
    @Test
    void constantsShouldMatchFrontendContract() {
        Assertions.assertEquals("business-error", SseErrorEventUtils.BUSINESS_ERROR_EVENT,
                "事件名改动会直接破坏前端错误展示");
        Assertions.assertFalse(SseErrorEventUtils.STREAM_ERROR_MESSAGE.contains("\n"));
    }
}
