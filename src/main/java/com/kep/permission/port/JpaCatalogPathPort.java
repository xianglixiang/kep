package com.kep.permission.port;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!local-mock")
class JpaCatalogPathPort implements CatalogPathPort {

    private final JdbcTemplate jdbc;

    JpaCatalogPathPort(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Long> ancestorIds(long catalogNodeId) {
        String path = jdbc.queryForObject(
            "SELECT path FROM catalog_node WHERE id = ?", String.class, catalogNodeId);
        if (path == null) return List.of();
        String trimmed = path.replaceAll("^/|/$", "");
        if (trimmed.isEmpty()) return List.of();
        List<Long> ids = new ArrayList<>();
        for (String s : trimmed.split("/")) {
            try { ids.add(Long.parseLong(s)); } catch (NumberFormatException ignore) {}
        }
        return ids;
    }
}
