package com.kep.shared.security;

import com.kep.shared.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 从请求头读取租户与用户并填充 SecurityContext/TenantContext。
 * M1 用头承载（X-User-Id + X-Tenant-Id）；M2+ 替换为真实 Spring Security。
 * 替代 M0 的 TenantFilter，行为向后兼容（同时仍写 TenantContext）。
 */
@Component
@Order(1)
public class SecurityFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String USER_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String tenantId = request.getHeader(TENANT_HEADER);
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.set(tenantId);
            }
            String userId = request.getHeader(USER_HEADER);
            if (userId != null && !userId.isBlank()) {
                SecurityContext.setCurrentUserId(Long.parseLong(userId));
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContext.clear();
        }
    }
}
