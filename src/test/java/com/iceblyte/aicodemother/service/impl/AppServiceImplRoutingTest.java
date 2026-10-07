package com.iceblyte.aicodemother.service.impl;

import com.iceblyte.aicodemother.ai.AiCodeGenTypeRoutingService;
import com.iceblyte.aicodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.iceblyte.aicodemother.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AppServiceImpl 路由兜底单元测试（不依赖 Spring 上下文 / 数据库 / AI 服务）
 * 验证：AI 路由失败或返回空时，创建应用不再直接报错，而是兜底为保守的 HTML 类型。
 */
@ExtendWith(MockitoExtension.class)
class AppServiceImplRoutingTest {

    @Mock
    private AiCodeGenTypeRoutingServiceFactory routingServiceFactory;

    @Mock
    private AiCodeGenTypeRoutingService routingService;

    @InjectMocks
    private AppServiceImpl appService;

    /**
     * 路由正常：返回 AI 选择的类型（核心功能零回归）
     */
    @Test
    void routeSuccessShouldUseAiSelectedType() {
        Mockito.when(routingServiceFactory.createAiCodeGenTypeRoutingService()).thenReturn(routingService);
        Mockito.when(routingService.routeCodeGenType(Mockito.anyString())).thenReturn(CodeGenTypeEnum.VUE_PROJECT);
        Assertions.assertEquals(CodeGenTypeEnum.VUE_PROJECT,
                appService.routeCodeGenTypeWithFallback("做一个博客系统"));
    }

    /**
     * 路由服务抛异常（key 失效 / 网络抖动 / 解析失败）：兜底 HTML 而不向外抛
     */
    @Test
    void routeExceptionShouldFallbackToHtml() {
        Mockito.when(routingServiceFactory.createAiCodeGenTypeRoutingService()).thenReturn(routingService);
        Mockito.when(routingService.routeCodeGenType(Mockito.anyString()))
                .thenThrow(new RuntimeException("dashscope api key invalid"));
        Assertions.assertEquals(CodeGenTypeEnum.HTML,
                appService.routeCodeGenTypeWithFallback("做一个博客系统"));
    }

    /**
     * 路由返回 null：兜底 HTML
     */
    @Test
    void routeNullShouldFallbackToHtml() {
        Mockito.when(routingServiceFactory.createAiCodeGenTypeRoutingService()).thenReturn(routingService);
        Mockito.when(routingService.routeCodeGenType(Mockito.anyString())).thenReturn(null);
        Assertions.assertEquals(CodeGenTypeEnum.HTML,
                appService.routeCodeGenTypeWithFallback("做一个博客系统"));
    }
}
