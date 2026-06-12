package com.supermarket.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 为业务控制器（controller 包）统一添加 /api 前缀。
 * 不影响静态资源访问路径，也不影响 ErrorController 等系统控制器。
 *
 * 效果：
 * - 开发模式：前端 /api/user/login → Vite proxy → localhost:8080/api/user/login → 控制器
 * - 生产模式：前端 /api/user/login → Spring Boot 直接处理 → 控制器
 */
@Configuration
public class ApiPrefixConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 只对业务控制器添加 /api 前缀，排除系统控制器和配置类
        configurer.addPathPrefix("/api",
            c -> c.getName().startsWith("com.supermarket.backend.controller"));
    }
}
