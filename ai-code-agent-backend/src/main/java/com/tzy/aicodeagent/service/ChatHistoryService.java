package com.tzy.aicodeagent.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.tzy.aicodeagent.model.dto.chathistory.ChatHistoryQueryRequest;
import com.tzy.aicodeagent.model.entity.ChatHistory;
import com.tzy.aicodeagent.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存消息历史
     * @param appId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据id删除对话
     * @param appId
     * @return
     */
    boolean deleteByAppId(Long appId);

    /**
     * 构建查询包装类
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 游标查询
     * @param appId
     * @param pageSize
     * @param lastCreateTime
     * @param loginUser
     * @return
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
