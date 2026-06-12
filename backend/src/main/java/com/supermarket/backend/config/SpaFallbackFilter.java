package com.supermarket.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Vue Router (createWebHistory) 的 SPA 回退过滤器。
 *
 * 当用户直接访问前端路由路径（如 /layout/user-info）时，Spring Boot 没有对应的
 * 静态文件。此过滤器将这类请求转发到 index.html，由 Vue Router 接管路由。
 *
 * 排除规则：
 * - /api/**     → 业务 API，由控制器处理
 * - /assets/**  → 前端构建产物（JS/CSS）
 * - 含 "." 的路径 → 静态文件（favicon.svg, cat_happy.mp4 等）
 * - /index.html → 避免无限转发
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpaFallbackFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 不拦截的路径：根路径、index.html、API、静态资源、带后缀文件
        if ("/".equals(path)
                || "/index.html".equals(path)
                || path.startsWith("/api/")
                || path.startsWith("/assets/")
                || path.contains(".")) {
            chain.doFilter(request, response);
            return;
        }

        // 其余路径（Vue 前端路由）转发到 index.html
        request.getRequestDispatcher("/index.html").forward(request, response);
    }
}
