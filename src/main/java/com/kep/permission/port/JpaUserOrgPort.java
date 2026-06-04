package com.kep.permission.port;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 一次 SQL 拉用户的所有 org_unit 祖先：
 *   user_org 直接列出 user 的直属 org；每个直属 org 用 path 解析所有祖先。
 *   合并去重。
 */
@Component
@Profile("!local-mock")
class JpaUserOrgPort implements UserOrgPort {

    private final JdbcTemplate jdbc;

    JpaUserOrgPort(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Set<Long> ancestorOrgIds(long userId) {
        List<Long> direct = jdbc.queryForList(
            "SELECT org_unit_id FROM user_org WHERE user_id = ?", Long.class, userId);
        if (direct.isEmpty()) return Set.of();

        Set<Long> result = new HashSet<>(direct);
        for (Long ouId : direct) {
            String path = jdbc.queryForObject(
                "SELECT path FROM org_unit WHERE id = ?", String.class, ouId);
            if (path == null) continue;
            String trimmed = path.replaceAll("^/|/$", "");
            for (String s : trimmed.split("/")) {
                try { result.add(Long.parseLong(s)); } catch (NumberFormatException ignore) {}
            }
        }
        return result;
    }
}
