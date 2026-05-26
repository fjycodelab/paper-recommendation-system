package com.lencode.paper.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.behavior.consumer-auto-startup=false")
class HealthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void reportsRealMysqlAndRedisWhenLocalConfigIsProvided() {
        assumeTrue(hasText(System.getenv("APP_DB_USERNAME")), "APP_DB_USERNAME is required for local integration check");
        assumeTrue(System.getenv("APP_DB_PASSWORD") != null, "APP_DB_PASSWORD is required for local integration check");

        HealthResponse response = restTemplate.getForObject(
                "http://127.0.0.1:" + port + "/api/health",
                HealthResponse.class
        );

        assertThat(response.getMysql()).isEqualTo("UP");
        assertThat(response.getRedis()).isEqualTo("UP");
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

