package com.iceblyte.aicodemother.model.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ChatHistoryMessageTypeEnum 单元测试（不依赖 Spring 上下文 / 数据库）
 */
class ChatHistoryMessageTypeEnumTest {

    /**
     * 所有合法消息类型（含新增的 ERROR）都能按 value 识别
     */
    @Test
    void getEnumByValueShouldRecognizeAllValues() {
        Assertions.assertEquals(ChatHistoryMessageTypeEnum.USER, ChatHistoryMessageTypeEnum.getEnumByValue("user"));
        Assertions.assertEquals(ChatHistoryMessageTypeEnum.AI, ChatHistoryMessageTypeEnum.getEnumByValue("ai"));
        Assertions.assertEquals(ChatHistoryMessageTypeEnum.ERROR, ChatHistoryMessageTypeEnum.getEnumByValue("error"));
    }

    /**
     * 未知值 / 空值返回 null
     */
    @Test
    void getEnumByValueShouldReturnNullForUnknown() {
        Assertions.assertNull(ChatHistoryMessageTypeEnum.getEnumByValue("unknown"));
        Assertions.assertNull(ChatHistoryMessageTypeEnum.getEnumByValue(null));
        Assertions.assertNull(ChatHistoryMessageTypeEnum.getEnumByValue(""));
    }

    /**
     * ERROR 的 value 必须能存入 chat_history.messageType varchar(32)
     */
    @Test
    void errorValueShouldBeStorableInVarchar32() {
        Assertions.assertTrue(ChatHistoryMessageTypeEnum.ERROR.getValue().length() <= 32,
                "ERROR.value 超出 DB 列宽 varchar(32)");
    }
}
