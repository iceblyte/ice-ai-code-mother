package com.iceblyte.aicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.iceblyte.aicodemother.model.entity.App;
import com.iceblyte.aicodemother.mapper.AppMapper;
import com.iceblyte.aicodemother.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/iceblyte">程序员iceblyte</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
