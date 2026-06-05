package com.kep;

import com.kep.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class V4MigrationIT extends IntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void flyway_v4_creates_edit_lock_table() {
        Integer c = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'edit_lock'",
            Integer.class);
        assertThat(c).isEqualTo(1);
        Integer uk = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.table_constraints " +
            "WHERE table_name='edit_lock' AND constraint_type='UNIQUE'",
            Integer.class);
        assertThat(uk).as("edit_lock has UNIQUE constraint").isGreaterThanOrEqualTo(1);
    }
}
