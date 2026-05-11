package com.tzy.aicodeagent.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.tzy.aicodeagent.model.entity.User;
import com.tzy.aicodeagent.mapper.UserMapper;
import com.tzy.aicodeagent.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户 服务层实现。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

}
