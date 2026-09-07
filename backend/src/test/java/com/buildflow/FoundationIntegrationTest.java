package com.buildflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class FoundationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:17-alpine");

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrationWasApplied() {

        Integer count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM buildflow_schema_metadata
                WHERE id = 1
                """,
                Integer.class
            );

        assertThat(count).isEqualTo(1);
    }

    @Test
    void applicationHealthEndpointWorks() throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        "http://localhost:"
                            + port
                            + "/api/v1/system/health"
                    )
                )
                .GET()
                .build();

        HttpResponse<String> response =
            client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
        assertThat(
            response.headers().firstValue("X-Trace-Id")
        ).isPresent();
    }

    @Test
    void actuatorHealthWorks() throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        "http://localhost:"
                            + port
                            + "/actuator/health"
                    )
                )
                .GET()
                .build();

        HttpResponse<String> response =
            client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }
}
