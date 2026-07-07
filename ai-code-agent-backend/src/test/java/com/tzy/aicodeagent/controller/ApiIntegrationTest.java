package com.tzy.aicodeagent.controller;

import cn.hutool.core.util.RandomUtil;
import com.tzy.aicodeagent.model.entity.App;
import com.tzy.aicodeagent.model.entity.User;
import com.tzy.aicodeagent.service.AppService;
import com.tzy.aicodeagent.service.ChatHistoryService;
import com.tzy.aicodeagent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 接口全链路功能测试（MockMvc + 真实 MySQL/Redis；数据库写入随事务回滚）
 * <p>按“功能应有的行为”断言 —— 失败的用例即为功能问题清单。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    @Resource
    private ChatHistoryService chatHistoryService;

    private static final String PASSWORD = "password123";

    // region 工具方法

    private String randomAccount() {
        return "t_" + RandomUtil.randomString(10);
    }

    /** 直接入库创建用户并返回（绕过注册接口） */
    private User createUser(String role) {
        User user = User.builder()
                .userAccount(randomAccount())
                .userPassword(userService.getEncryptPassword(PASSWORD))
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

    /** 通过登录接口获取会话 Cookie */
    private Cookie[] login(String account) throws Exception {
        String body = String.format("{\"userAccount\":\"%s\",\"userPassword\":\"%s\"}", account, PASSWORD);
        MvcResult result = mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Cookie[] cookies = result.getResponse().getCookies();
        assertTrue(cookies != null && cookies.length > 0, "登录响应应携带会话 Cookie");
        return cookies;
    }

    // endregion

    @Test
    @DisplayName("健康检查接口返回 ok")
    void health() throws Exception {
        mockMvc.perform(get("/health/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("ok"));
    }

    // region 用户接口

    @Test
    @DisplayName("用户注册-登录-取登录态-登出 全流程")
    void userFullFlow() throws Exception {
        String account = randomAccount();
        String registerBody = String.format(
                "{\"userAccount\":\"%s\",\"userPassword\":\"%s\",\"checkPassword\":\"%s\"}",
                account, PASSWORD, PASSWORD);
        // 注册
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                // 后端 JsonConfig 将 Long 序列化为字符串，防止前端精度丢失
                .andExpect(jsonPath("$.data").isString());
        // 重复注册被拒绝
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(jsonPath("$.code").value(40000));
        // 登录
        Cookie[] cookies = login(account);
        // 获取当前登录用户
        mockMvc.perform(get("/user/get/login").cookie(cookies))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userAccount").value(account));
        // 登出
        mockMvc.perform(post("/user/logout").cookie(cookies))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
        // 登出后取登录态应报未登录
        mockMvc.perform(get("/user/get/login").cookie(cookies))
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    @DisplayName("未登录访问需登录接口返回 40100")
    void notLoginRejected() throws Exception {
        mockMvc.perform(get("/user/get/login"))
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    @DisplayName("普通用户访问管理员接口返回 40101")
    void normalUserForbiddenOnAdminApi() throws Exception {
        User user = createUser("user");
        Cookie[] cookies = login(user.getUserAccount());
        mockMvc.perform(post("/user/list/page/vo").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    @DisplayName("管理员：用户增删改查全流程")
    void adminUserCrud() throws Exception {
        User admin = createUser("admin");
        Cookie[] cookies = login(admin.getUserAccount());
        // 新增用户
        String addBody = String.format("{\"userAccount\":\"%s\",\"userName\":\"新用户\",\"userRole\":\"user\"}",
                randomAccount());
        MvcResult addResult = mockMvc.perform(post("/user/add").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON).content(addBody))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String newUserId = com.jayway.jsonpath.JsonPath.read(
                addResult.getResponse().getContentAsString(), "$.data").toString();
        // 查询
        mockMvc.perform(get("/user/get").cookie(cookies).param("id", newUserId))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userName").value("新用户"));
        // 更新
        mockMvc.perform(post("/user/update").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"id\":%s,\"userName\":\"改名用户\"}", newUserId)))
                .andExpect(jsonPath("$.code").value(0));
        // 分页列表
        mockMvc.perform(post("/user/list/page/vo").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
        // 删除
        mockMvc.perform(post("/user/delete").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"id\":%s}", newUserId)))
                .andExpect(jsonPath("$.code").value(0));
    }

    // endregion

    // region 应用接口

    @Test
    @DisplayName("应用：详情-更新-我的列表-删除 全流程（本人操作）")
    void appOwnerFlow() throws Exception {
        User user = createUser("user");
        App app = createApp(user.getId());
        Cookie[] cookies = login(user.getUserAccount());
        // 详情
        mockMvc.perform(get("/app/get/vo").cookie(cookies).param("id", app.getId().toString()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.appName").value("测试应用"))
                .andExpect(jsonPath("$.data.user.id").value(user.getId()));
        // 更新名称
        mockMvc.perform(post("/app/update").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"id\":%d,\"appName\":\"新名字\"}", app.getId())))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/app/get/vo").cookie(cookies).param("id", app.getId().toString()))
                .andExpect(jsonPath("$.data.appName").value("新名字"));
        // 我的应用列表
        mockMvc.perform(post("/app/my/list/page/vo").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].id").value(app.getId()));
        // 删除
        mockMvc.perform(post("/app/delete").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"id\":%d}", app.getId())))
                .andExpect(jsonPath("$.code").value(0));
        // 删除后查详情应 40400
        mockMvc.perform(get("/app/get/vo").cookie(cookies).param("id", app.getId().toString()))
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    @DisplayName("应用：非本人更新/删除被拒绝")
    void appNotOwnerRejected() throws Exception {
        User owner = createUser("user");
        User other = createUser("user");
        App app = createApp(owner.getId());
        Cookie[] cookies = login(other.getUserAccount());
        mockMvc.perform(post("/app/update").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"id\":%d,\"appName\":\"劫持\"}", app.getId())))
                .andExpect(jsonPath("$.code").value(40101));
        mockMvc.perform(post("/app/delete").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"id\":%d}", app.getId())))
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    @DisplayName("精选应用列表接口可正常访问")
    void goodAppList() throws Exception {
        mockMvc.perform(post("/app/good/list/page/vo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("管理员设置/取消精选后，精选列表立即可见变化（缓存即时失效）")
    void featureAppRefreshesGoodList() throws Exception {
        User owner = createUser("user");
        User admin = createUser("admin");
        App app = createApp(owner.getId());
        Cookie[] cookies = login(admin.getUserAccount());
        String listBody = "{\"pageNum\":1,\"pageSize\":20}";
        String containsApp = String.format("$.data.records[?(@.id == '%d')]", app.getId());
        // 预热缓存：此时列表不含该应用
        mockMvc.perform(post("/app/good/list/page/vo")
                        .contentType(MediaType.APPLICATION_JSON).content(listBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath(containsApp).isEmpty());
        // 管理员设为精选
        mockMvc.perform(post("/app/admin/update").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"id\":%d,\"priority\":99}", app.getId())))
                .andExpect(jsonPath("$.code").value(0));
        // 精选列表应立即包含该应用（若缓存未失效则此处拿到旧缓存而失败）
        mockMvc.perform(post("/app/good/list/page/vo")
                        .contentType(MediaType.APPLICATION_JSON).content(listBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath(containsApp).isNotEmpty());
        // 取消精选后应立即从列表移除（同时清掉本测试写入的缓存，避免污染真实数据）
        mockMvc.perform(post("/app/admin/update").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"id\":%d,\"priority\":0}", app.getId())))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/app/good/list/page/vo")
                        .contentType(MediaType.APPLICATION_JSON).content(listBody))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath(containsApp).isEmpty());
    }

    @Test
    @DisplayName("每页超过 20 条的列表请求被拒绝")
    void pageSizeLimit() throws Exception {
        User user = createUser("user");
        Cookie[] cookies = login(user.getUserAccount());
        mockMvc.perform(post("/app/my/list/page/vo").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":21}"))
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    @DisplayName("管理员：应用查改删全流程")
    void adminAppCrud() throws Exception {
        User owner = createUser("user");
        User admin = createUser("admin");
        App app = createApp(owner.getId());
        Cookie[] cookies = login(admin.getUserAccount());
        // 管理员查详情
        mockMvc.perform(get("/app/admin/get/vo").cookie(cookies).param("id", app.getId().toString()))
                .andExpect(jsonPath("$.code").value(0));
        // 管理员更新（可改优先级）
        mockMvc.perform(post("/app/admin/update").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"id\":%d,\"appName\":\"管理员改名\",\"priority\":99}", app.getId())))
                .andExpect(jsonPath("$.code").value(0));
        // 管理员分页列表
        mockMvc.perform(post("/app/admin/list/page/vo").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"pageNum\":1,\"pageSize\":10,\"id\":%d}", app.getId())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].appName").value("管理员改名"));
        // 管理员删除他人应用
        mockMvc.perform(post("/app/admin/delete").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"id\":%d}", app.getId())))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("部署/下载：代码未生成时分别报 50000 / 40400")
    void deployAndDownloadWithoutCode() throws Exception {
        User user = createUser("user");
        App app = createApp(user.getId());
        Cookie[] cookies = login(user.getUserAccount());
        mockMvc.perform(post("/app/deploy").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"appId\":%d}", app.getId())))
                .andExpect(jsonPath("$.code").value(50000));
        mockMvc.perform(get("/app/download/" + app.getId()).cookie(cookies))
                .andExpect(jsonPath("$.code").value(40400));
    }

    // endregion

    // region 对话历史接口

    @Test
    @DisplayName("对话历史：创建者可查自己应用的历史")
    void chatHistoryForCreator() throws Exception {
        User user = createUser("user");
        App app = createApp(user.getId());
        chatHistoryService.addChatMessage(app.getId(), "帮我做个页面", "user", user.getId());
        chatHistoryService.addChatMessage(app.getId(), "已生成", "ai", user.getId());
        Cookie[] cookies = login(user.getUserAccount());
        mockMvc.perform(get("/chatHistory/app/" + app.getId()).cookie(cookies))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    @Test
    @DisplayName("对话历史：未登录访问返回 40100")
    void chatHistoryNotLogin() throws Exception {
        User user = createUser("user");
        App app = createApp(user.getId());
        mockMvc.perform(get("/chatHistory/app/" + app.getId()))
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    @DisplayName("对话历史：管理员分页查询全部")
    void chatHistoryAdminList() throws Exception {
        User admin = createUser("admin");
        Cookie[] cookies = login(admin.getUserAccount());
        mockMvc.perform(post("/chatHistory/admin/list/page/vo").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    // endregion
}
