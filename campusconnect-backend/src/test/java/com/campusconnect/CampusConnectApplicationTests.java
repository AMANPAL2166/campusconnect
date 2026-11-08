package com.campusconnect;

import com.campusconnect.support.MySqlContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CampusconnectBackendApplication.class)
@Import(MySqlContainerConfig.class)
class CampusConnectApplicationTests {

    @Autowired JdbcTemplate jdbc;

    @Test
    void contextLoads_andFlywayRan() {
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(1);
    }
}