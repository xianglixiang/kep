package com.kep;

import com.kep.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class V2MigrationIT extends IntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void flyway_v2_creates_auth_tables_and_catalog_node_path() {
        for (String table : new String[]{"app_user", "org_unit", "user_org", "acl", "review_request"}) {
            Integer c = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = ?",
                Integer.class, table);
            assertThat(c).as("table %s exists", table).isEqualTo(1);
        }
        // catalog_node.path 列已存在
        Integer pathCol = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.columns WHERE table_name='catalog_node' AND column_name='path'",
            Integer.class);
        assertThat(pathCol).isEqualTo(1);
    }
}
