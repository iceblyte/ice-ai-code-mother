package com.iceblyte.aicodemother.service;

import com.iceblyte.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.iceblyte.aicodemother.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.iceblyte.aicodemother.model.entity.ChatHistory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/iceblyte">程序员iceblyte</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话历史。
     *
     * @param appId     应用ID。
     * @param message   消息内容。
     * @param messageType 消息类型。
     * @param userId    用户ID。
     * @return 是否添加成功。
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用ID删除对话历史。
     *
     * @param appId 应用ID。
     * @return 是否删除成功。
     */
    boolean deleteByAppId(Long appId);

    /**
     * 根据应用ID分页获取应用对话历史。
     *
     * @param appId       应用ID。
     * @param pageSize    每页大小。
     * @param lastCreateTime 最后创建时间。
     * @param loginUser 登录用户。
     * @return 对话历史分页列表。
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 获取查询包装类。
     *
     * @param chatHistoryQueryRequest 查询条件。
     * @return 查询包装类。
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
