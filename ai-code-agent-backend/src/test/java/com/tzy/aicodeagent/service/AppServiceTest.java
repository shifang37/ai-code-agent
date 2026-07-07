package com.tzy.aicodeagent.service;

import cn.hutool.core.util.RandomUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.tzy.aicodeagent.exception.BusinessException;
import com.tzy.aicodeagent.exception.ErrorCode;
import com.tzy.aicodeagent.model.entity.App;
import com.tzy.aicodeagent.model.entity.User;
import com.tzy.aicodeagent.model.vo.AppVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 应用服务功能测试（不触发真实 AI 生成；数据随事务回滚）
 */
@SpringBootTest
@Transactional
class AppServiceTest {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;

    private User createUser() {
        User user = User.builder()
                .userAccount("t_" + RandomUtil.randomString(10))
                .userPassword(userService.getEncryptPassword("password123"))
                .userName("测试用户")
                .userRole("user")
                .build();
        assertTrue(userService.save(user));
        return user;
    }

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
    @DisplayName("getAppVO：包含创建者用户信息")
    void getAppVOIncludesUser() {
        User user = createUser();
        App app = createApp(user.getId());
        AppVO vo = appService.getAppVO(app);
        assertNotNull(vo);
        assertEquals(app.getId(), vo.getId());
        assertEquals("测试应用", vo.getAppName());
        assertNotNull(vo.getUser());
        assertEquals(user.getId(), vo.getUser().getId());
        assertNull(appService.getAppVO(null));
    }

    @Test
    @DisplayName("getAppVOList：批量封装并关联用户")
    void getAppVOListBatch() {
        User user = createUser();
        App app1 = createApp(user.getId());
        App app2 = createApp(user.getId());
        List<AppVO> voList = appService.getAppVOList(List.of(app1, app2));
        assertEquals(2, voList.size());
        voList.forEach(vo -> {
            assertNotNull(vo.getUser());
            assertEquals(user.getId(), vo.getUser().getId());
        });
    }

    @Test
    @DisplayName("查询自己的应用列表：QueryWrapper 按 userId 过滤")
    void queryOwnApps() {
        User user = createUser();
        createApp(user.getId());
        createApp(user.getId());
        long count = appService.count(QueryWrapper.create().eq("userId", user.getId()));
        assertEquals(2, count);
    }

    // region chatToGenCode 参数与权限校验（不会真正调用 AI）

    @Test
    @DisplayName("生成代码：应用ID非法被拒绝")
    void chatToGenCodeInvalidAppId() {
        User user = createUser();
        assertThrows(BusinessException.class,
                () -> appService.chatToGenCode(null, "做个页面", user));
        assertThrows(BusinessException.class,
                () -> appService.chatToGenCode(-1L, "做个页面", user));
    }

    @Test
    @DisplayName("生成代码：消息为空被拒绝")
    void chatToGenCodeBlankMessage() {
        User user = createUser();
        App app = createApp(user.getId());
        BusinessException e = assertThrows(BusinessException.class,
                () -> appService.chatToGenCode(app.getId(), " ", user));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("生成代码：应用不存在报 40400")
    void chatToGenCodeAppNotFound() {
        User user = createUser();
        BusinessException e = assertThrows(BusinessException.class,
                () -> appService.chatToGenCode(999999999999L, "做个页面", user));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("生成代码：非应用创建者无权限")
    void chatToGenCodeNotOwner() {
        User owner = createUser();
        User other = createUser();
        App app = createApp(owner.getId());
        BusinessException e = assertThrows(BusinessException.class,
                () -> appService.chatToGenCode(app.getId(), "做个页面", other));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), e.getCode());
    }

    // endregion

    // region deployApp 校验

    @Test
    @DisplayName("部署应用：应用不存在报 40400")
    void deployAppNotFound() {
        User user = createUser();
        BusinessException e = assertThrows(BusinessException.class,
                () -> appService.deployApp(999999999999L, user));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("部署应用：非创建者无权限")
    void deployAppNotOwner() {
        User owner = createUser();
        User other = createUser();
        App app = createApp(owner.getId());
        BusinessException e = assertThrows(BusinessException.class,
                () -> appService.deployApp(app.getId(), other));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("部署应用：代码尚未生成时报系统错误")
    void deployAppNoCodeDir() {
        User user = createUser();
        App app = createApp(user.getId());
        BusinessException e = assertThrows(BusinessException.class,
                () -> appService.deployApp(app.getId(), user));
        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), e.getCode());
        assertTrue(e.getMessage().contains("应用代码不存在"));
    }

    // endregion

    @Test
    @DisplayName("删除应用：级联删除对话历史")
    void removeByIdCascadesChatHistory() {
        User user = createUser();
        App app = createApp(user.getId());
        chatHistoryService.addChatMessage(app.getId(), "第一条", "user", user.getId());
        chatHistoryService.addChatMessage(app.getId(), "第二条", "ai", user.getId());
        assertTrue(appService.removeById(app.getId()));
        assertNull(appService.getById(app.getId()));
        long remaining = chatHistoryService.count(QueryWrapper.create().eq("appId", app.getId()));
        assertEquals(0, remaining);
    }
}
