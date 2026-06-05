package com.kep;

import com.kep.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class V3MigrationIT extends IntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void flyway_v3_creates_knowledge_tables() {
        for (String table : new String[]{"knowledge", "knowledge_version"}) {
            Integer c = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = ?",
                Integer.class, table);
            assertThat(c).as("table %s exists", table).isEqualTo(1);
        }
    }
}
