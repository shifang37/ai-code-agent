package com.tzy.aicodeagent.service;

import cn.hutool.core.util.RandomUtil;
import com.tzy.aicodeagent.exception.BusinessException;
import com.tzy.aicodeagent.exception.ErrorCode;
import com.tzy.aicodeagent.model.entity.User;
import com.tzy.aicodeagent.model.vo.LoginUserVO;
import com.tzy.aicodeagent.model.vo.UserVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务功能测试（数据库写入随事务回滚，不污染数据）
 */
@SpringBootTest
@Transactional
class UserServiceTest {

    @Resource
    private UserService userService;

    private String randomAccount() {
        return "t_" + RandomUtil.randomString(10);
    }

    // region 注册

    @Test
    @DisplayName("注册成功：返回ID，默认昵称/角色正确，密码已加密")
    void registerSuccess() {
        String account = randomAccount();
        long userId = userService.userRegister(account, "password123", "password123");
        assertTrue(userId > 0);
        User saved = userService.getById(userId);
        assertNotNull(saved);
        assertEquals(account, saved.getUserAccount());
        assertEquals("无名", saved.getUserName());
        assertEquals("user", saved.getUserRole());
        assertNotEquals("password123", saved.getUserPassword());
        assertEquals(userService.getEncryptPassword("password123"), saved.getUserPassword());
    }

    @Test
    @DisplayName("注册失败：账号少于4位")
    void registerShortAccount() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.userRegister("abc", "password123", "password123"));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("注册失败：密码少于8位")
    void registerShortPassword() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.userRegister(randomAccount(), "1234567", "1234567"));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("注册失败：两次密码不一致")
    void registerPasswordMismatch() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.userRegister(randomAccount(), "password123", "password456"));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("注册失败：账号重复")
    void registerDuplicateAccount() {
        String account = randomAccount();
        userService.userRegister(account, "password123", "password123");
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.userRegister(account, "password123", "password123"));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        assertTrue(e.getMessage().contains("重复"));
    }

    // endregion

    // region 登录 / 登录态 / 登出

    @Test
    @DisplayName("登录成功：返回脱敏VO并写入会话")
    void loginSuccess() {
        String account = randomAccount();
        userService.userRegister(account, "password123", "password123");
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginUserVO vo = userService.userLogin(account, "password123", request);
        assertNotNull(vo);
        assertEquals(account, vo.getUserAccount());
        assertNotNull(vo.getId());
        // 会话中可取回登录用户
        User loginUser = userService.getLoginUser(request);
        assertEquals(vo.getId(), loginUser.getId());
    }

    @Test
    @DisplayName("登录失败：密码错误")
    void loginWrongPassword() {
        String account = randomAccount();
        userService.userRegister(account, "password123", "password123");
        MockHttpServletRequest request = new MockHttpServletRequest();
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.userLogin(account, "wrongpass123", request));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("未登录获取登录用户：抛未登录异常")
    void getLoginUserWithoutLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.getLoginUser(request));
        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("登出成功后登录态失效")
    void logoutSuccess() {
        String account = randomAccount();
        userService.userRegister(account, "password123", "password123");
        MockHttpServletRequest request = new MockHttpServletRequest();
        userService.userLogin(account, "password123", request);
        assertTrue(userService.userLogout(request));
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.getLoginUser(request));
        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), e.getCode());
    }

    @Test
    @DisplayName("未登录时登出：抛操作失败异常")
    void logoutWithoutLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.userLogout(request));
        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), e.getCode());
    }

    // endregion

    // region 加密与脱敏

    @Test
    @DisplayName("密码加密：加盐MD5且结果稳定")
    void encryptPassword() {
        String encrypted = userService.getEncryptPassword("password123");
        assertEquals(DigestUtils.md5DigestAsHex(("yupi" + "password123").getBytes()), encrypted);
        assertEquals(encrypted, userService.getEncryptPassword("password123"));
    }

    @Test
    @DisplayName("getUserVO：拷贝基础字段（VO中不含密码字段）")
    void getUserVODesensitized() {
        String account = randomAccount();
        long userId = userService.userRegister(account, "password123", "password123");
        User user = userService.getById(userId);
        UserVO vo = userService.getUserVO(user);
        assertNotNull(vo);
        assertEquals(user.getId(), vo.getId());
        assertEquals(account, vo.getUserAccount());
        assertNull(userService.getUserVO(null));
    }

    // endregion
}
