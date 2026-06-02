package com.kep;

import com.kep.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationSmokeIT extends IntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void flyway_creates_catalog_node_table() {
        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'catalog_node'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
