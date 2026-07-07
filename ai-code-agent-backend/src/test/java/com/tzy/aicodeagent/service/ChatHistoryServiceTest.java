package com.tzy.aicodeagent.service;

import cn.hutool.core.util.RandomUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.tzy.aicodeagent.exception.BusinessException;
import com.tzy.aicodeagent.exception.ErrorCode;
import com.tzy.aicodeagent.model.entity.App;
import com.tzy.aicodeagent.model.entity.ChatHistory;
import com.tzy.aicodeagent.model.entity.User;
import com.tzy.aicodeagent.model.enums.ChatHistoryMessageTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对话历史服务功能测试（数据随事务回滚）
 */
@SpringBootTest
@Transactional
class ChatHistoryServiceTest {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /** 直接入库创建用户（绕过注册流程） */
    private User createUser(String role) {
        User user = User.builder()
                .userAccount("t_" + RandomUtil.randomString(10))
                .userPassword(userService.getEncryptPassword("password123"))
                .userName("测试用户")
                .userRole(role)
                .build();
        assertTrue(userService.save(user));
        return user;
    }

    /** 直接入库创建应用（绕过 AI 类型路由） */
    private App createApp(Long userId) {
        App app = App.builder()
                .appName("测试应用")
                .initPrompt("做一个测试页面")
                .codeGenType("html")
                .userId(userId)
                .build();
        assertTrue(appService.save(app));
        return app;
    }

    @Test
    @DisplayName("新增对话消息：user 与 ai 类型均成功入库")
    void addChatMessageSuccess() {
        User user = createUser("user");
        App app = createApp(user.getId());
        assertTrue(chatHistoryService.addChatMessage(app.getId(), "帮我做个页面",
                ChatHistoryMessageTypeEnum.USER.getValue(), user.getId()));
        assertTrue(chatHistoryService.addChatMessage(app.getId(), "好的，已生成",
                ChatHistoryMessageTypeEnum.AI.getValue(), user.getId()));
        long count = chatHistoryService.count(QueryWrapper.create().eq("appId", app.getId()));
        assertEquals(2, count);
    }

    @Test
    @DisplayName("新增对话消息：非法消息类型被拒绝")
    void addChatMessageInvalidType() {
        User user = createUser("user");
        App app = createApp(user.getId());
        BusinessException e = assertThrows(BusinessException.class,
                () -> chatHistoryService.addChatMessage(app.getId(), "消息", "system", user.getId()));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("新增对话消息：空参数被拒绝")
    void addChatMessageInvalidParams() {
        User user = createUser("user");
        App app = createApp(user.getId());
        assertThrows(BusinessException.class,
                () -> chatHistoryService.addChatMessage(null, "消息", "user", user.getId()));
        assertThrows(BusinessException.class,
                () -> chatHistoryService.addChatMessage(app.getId(), " ", "user", user.getId()));
        assertThrows(BusinessException.class,
                () -> chatHistoryService.addChatMessage(app.getId(), "消息", "user", null));
    }

    @Test
    @DisplayName("查询应用对话历史：创建者可查，按时间降序")
    void listAppChatHistoryAsCreator() {
        User user = createUser("user");
        App app = createApp(user.getId());
        chatHistoryService.addChatMessage(app.getId(), "第一条", "user", user.getId());
        chatHistoryService.addChatMessage(app.getId(), "第二条", "ai", user.getId());
        Page<ChatHistory> page = chatHistoryService.listAppChatHistoryByPage(app.getId(), 10, null, user);
        List<ChatHistory> records = page.getRecords();
        assertEquals(2, records.size());
        // 默认按 createTime 降序
        assertFalse(records.get(0).getCreateTime().isBefore(records.get(1).getCreateTime()));
    }

    @Test
    @DisplayName("查询应用对话历史：非创建者无权限，管理员可查")
    void listAppChatHistoryPermission() {
        User owner = createUser("user");
        User other = createUser("user");
        User admin = createUser("admin");
        App app = createApp(owner.getId());
        chatHistoryService.addChatMessage(app.getId(), "一条消息", "user", owner.getId());
        // 非创建者
        BusinessException e = assertThrows(BusinessException.class,
                () -> chatHistoryService.listAppChatHistoryByPage(app.getId(), 10, null, other));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), e.getCode());
        // 管理员
        Page<ChatHistory> page = chatHistoryService.listAppChatHistoryByPage(app.getId(), 10, null, admin);
        assertEquals(1, page.getRecords().size());
    }

    @Test
    @DisplayName("查询应用对话历史：pageSize 超出 1-50 被拒绝")
    void listAppChatHistoryInvalidPageSize() {
        User user = createUser("user");
        App app = createApp(user.getId());
        assertThrows(BusinessException.class,
                () -> chatHistoryService.listAppChatHistoryByPage(app.getId(), 51, null, user));
        assertThrows(BusinessException.class,
                () -> chatHistoryService.listAppChatHistoryByPage(app.getId(), 0, null, user));
    }

    @Test
    @DisplayName("查询应用对话历史：应用不存在报 40400")
    void listAppChatHistoryAppNotFound() {
        User user = createUser("user");
        BusinessException e = assertThrows(BusinessException.class,
                () -> chatHistoryService.listAppChatHistoryByPage(999999999999L, 10, null, user));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("按应用ID删除对话历史")
    void deleteByAppId() {
        User user = createUser("user");
        App app = createApp(user.getId());
        chatHistoryService.addChatMessage(app.getId(), "第一条", "user", user.getId());
        chatHistoryService.addChatMessage(app.getId(), "第二条", "ai", user.getId());
        assertTrue(chatHistoryService.deleteByAppId(app.getId()));
        long count = chatHistoryService.count(QueryWrapper.create().eq("appId", app.getId()));
        assertEquals(0, count);
    }
}
