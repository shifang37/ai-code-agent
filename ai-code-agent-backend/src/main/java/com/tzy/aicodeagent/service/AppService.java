package com.tzy.aicodeagent.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.tzy.aicodeagent.model.dto.app.AppQueryRequest;
import com.tzy.aicodeagent.model.entity.App;
import com.tzy.aicodeagent.model.entity.User;
import com.tzy.aicodeagent.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
public interface AppService extends IService<App> {

    /**
     * 查询app关联信息
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 构造查询对象
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 分页查询
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署服务
     * @param appId
     * @param loginUser
     * @return
     */
    String deployApp(Long appId, User loginUser);
}
