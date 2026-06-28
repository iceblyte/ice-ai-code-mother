package com.iceblyte.aicodemother.service;

import com.iceblyte.aicodemother.model.dto.app.AppQueryRequest;
import com.iceblyte.aicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.iceblyte.aicodemother.model.entity.App;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/iceblyte">程序员iceblyte</a>
 */
public interface AppService extends IService<App> {

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
