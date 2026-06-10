package com.kep;

import com.kep.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class V5MigrationIT extends IntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void flyway_v5_creates_knowledge_title_index() {
        Integer c = jdbc.queryForObject(
            "SELECT count(*) FROM pg_indexes WHERE tablename = 'knowledge' AND indexname = 'idx_knowledge_tenant_title'",
            Integer.class);
        assertThat(c).as("idx_knowledge_tenant_title exists").isEqualTo(1);
    }
}
