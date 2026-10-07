package com.iceblyte.aicodemother.service.impl;

import com.iceblyte.aicodemother.model.entity.ChatHistory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatHistoryServiceImpl 记忆回灌预算裁剪单元测试（纯静态函数，不依赖 Spring 上下文 / 数据库）
 */
class ChatHistoryServiceImplTest {

    private static ChatHistory history(String message) {
        return ChatHistory.builder().message(message).build();
    }

    /**
     * 空输入返回空列表
     */
    @Test
    void emptyInputShouldReturnEmpty() {
        Assertions.assertTrue(ChatHistoryServiceImpl.trimHistoryByCharBudget(new ArrayList<>()).isEmpty());
    }

    /**
     * 预算内全量保留，且输入倒序（新→旧）必须反转为正序（旧→新）
     */
    @Test
    void underBudgetShouldKeepAllAndReverseOrder() {
        List<ChatHistory> input = new ArrayList<>(List.of(history("最新"), history("中间"), history("最旧")));
        List<ChatHistory> result = ChatHistoryServiceImpl.trimHistoryByCharBudget(input);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("最旧", result.get(0).getMessage());
        Assertions.assertEquals("中间", result.get(1).getMessage());
        Assertions.assertEquals("最新", result.get(2).getMessage());
    }

    /**
     * 超出总预算：丢弃最旧的消息，保留最新的（倒序累计语义）
     */
    @Test
    void overBudgetShouldDropOldestKeepRecent() {
        // 每条 40_000 字符：最新两条合计 80_000 在预算内，第三条（最旧）加入后 120_000 超预算 → 丢弃
        String chunk = "x".repeat(40_000);
        List<ChatHistory> input = new ArrayList<>(List.of(history(chunk), history(chunk), history(chunk)));
        List<ChatHistory> result = ChatHistoryServiceImpl.trimHistoryByCharBudget(input);
        Assertions.assertEquals(2, result.size());
    }

    /**
     * 最新一条消息必须始终保留（即使它本身就很大）
     */
    @Test
    void latestMessageShouldAlwaysBeKept() {
        String huge = "x".repeat(80_000);
        List<ChatHistory> result = ChatHistoryServiceImpl.trimHistoryByCharBudget(new ArrayList<>(List.of(history(huge))));
        Assertions.assertEquals(1, result.size());
    }

    /**
     * 单条超长：截断到单条上限并追加提示尾标，尾标计入预算
     */
    @Test
    void singleMessageOverLimitShouldTruncateWithSuffix() {
        String huge = "x".repeat(60_000);
        List<ChatHistory> result = ChatHistoryServiceImpl.trimHistoryByCharBudget(new ArrayList<>(List.of(history(huge))));
        String trimmed = result.get(0).getMessage();
        Assertions.assertTrue(trimmed.endsWith(ChatHistoryServiceImpl.TRUNCATED_SUFFIX));
        Assertions.assertEquals(
                ChatHistoryServiceImpl.MAX_SINGLE_MESSAGE_CHARS + ChatHistoryServiceImpl.TRUNCATED_SUFFIX.length(),
                trimmed.length());
    }

    /**
     * 累计恰好等于总预算时不丢弃（仅 > 预算才丢弃），语义边界明确
     */
    @Test
    void exactlyAtBoundaryShouldNotDrop() {
        List<ChatHistory> input = new ArrayList<>(List.of(
                history("x".repeat(30_000)),
                history("x".repeat(30_000)),
                history("x".repeat(40_000))));
        Assertions.assertEquals(3, ChatHistoryServiceImpl.trimHistoryByCharBudget(input).size());
    }

    /**
     * null 消息按空串处理，不抛 NPE，且被规范化为空串防止下游 UserMessage.from(null) 出错
     */
    @Test
    void nullMessageShouldBeTreatedAsEmpty() {
        List<ChatHistory> input = new ArrayList<>(List.of(history(null), history("abc")));
        List<ChatHistory> result = ChatHistoryServiceImpl.trimHistoryByCharBudget(input);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("abc", result.get(0).getMessage());
        Assertions.assertEquals("", result.get(1).getMessage());
    }
}
