package com.kep.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * M1 阶段：暂不接入真实鉴权（仍由 SecurityFilter 读 X-User-Id / X-Tenant-Id）。
 * 仅占位 SecurityFilterChain，使默认 basic-auth 不拦截测试与本地开发。
 * M2+ 将替换为基于 SecurityContext 的授权规则。
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain permitAll(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(reg -> reg.anyRequest().permitAll());
        return http.build();
    }
}
