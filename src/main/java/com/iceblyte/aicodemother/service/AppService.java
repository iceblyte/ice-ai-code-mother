package com.iceblyte.aicodemother.service;

import com.iceblyte.aicodemother.model.dto.app.AppQueryRequest;
import com.iceblyte.aicodemother.model.dto.app.AppVersionCompareRequest;
import com.iceblyte.aicodemother.model.entity.User;
import com.iceblyte.aicodemother.model.vo.AppVersionCompareVO;
import com.iceblyte.aicodemother.model.vo.AppVersionVO;
import com.iceblyte.aicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.iceblyte.aicodemother.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/iceblyte">程序员iceblyte</a>
 */
public interface AppService extends IService<App> {

    /**
     * 通过对话生成应用代码
     *
     * @param appId     应用id
     * @param message   消息
     * @param loginUser 登录用户
     * @return 代码
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     *
     * @param appId     应用id
     * @param loginUser 登录用户
     * @return 应用部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 获取应用代码版本列表
     *
     * @param appId     应用 id
     * @param loginUser 登录用户
     * @return 版本列表
     */
    List<AppVersionVO> listAppVersions(Long appId, User loginUser);

    /**
     * 对比应用代码版本
     *
     * @param request   对比请求
     * @param loginUser 登录用户
     * @return 对比结果
     */
    AppVersionCompareVO compareAppVersion(AppVersionCompareRequest request, User loginUser);

    /**
     * 获取应用封装类
     *
     * @param app 应用
     * @return 应用封装类
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装类列表
     *
     * @param appList 应用列表
     * @return 应用封装类列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 构造应用查询条件
     *
      * @param appQueryRequest 应用查询条件
      * @return 查询条件包装器
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
}
