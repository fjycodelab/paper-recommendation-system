package com.lencode.paper.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.lencode.paper.auth.dto.LoginRequest;
import com.lencode.paper.auth.dto.RegisterRequest;
import com.lencode.paper.auth.service.AuthTokenService;
import com.lencode.paper.auth.vo.AdminPingResponse;
import com.lencode.paper.auth.vo.LoginResponse;
import com.lencode.paper.auth.vo.UserResponse;
import com.lencode.paper.common.response.ApiError;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.behavior.consumer-auto-startup=false")
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void registersUserInMysqlAndRejectsDuplicate() {
        assumeTrue(hasText(System.getenv("APP_DB_USERNAME")), "APP_DB_USERNAME is required for local integration check");
        assumeTrue(System.getenv("APP_DB_PASSWORD") != null, "APP_DB_PASSWORD is required for local integration check");

        String username = "t3_" + UUID.randomUUID().toString().replace("-", "");
        String password = "pass123";
        String url = "http://127.0.0.1:" + port + "/api/auth/register";
        try {
            ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                    url,
                    new RegisterRequest(username, password),
                    UserResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUsername()).isEqualTo(username);
            assertThat(response.getBody().getRole()).isEqualTo("USER");
            assertThat(response.getBody().getStatus()).isEqualTo("ACTIVE");

            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT password_hash, role, status FROM users WHERE username = ?",
                    username
            );
            String storedHash = String.valueOf(row.get("password_hash"));
            assertThat(storedHash).isNotEqualTo(password);
            assertThat(passwordEncoder.matches(password, storedHash)).isTrue();
            assertThat(row.get("role")).isEqualTo("USER");
            assertThat(row.get("status")).isEqualTo("ACTIVE");

            ResponseEntity<ApiError> duplicateResponse = restTemplate.postForEntity(
                    url,
                    new RegisterRequest(username, password),
                    ApiError.class
            );
            assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(duplicateResponse.getBody()).isNotNull();
            assertThat(duplicateResponse.getBody().getMessage()).isEqualTo("账号已存在");
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE username = ?", username);
        }
    }

    @Test
    void logsInSeedAdminAndReadsCurrentUserFromRedisToken() {
        assumeTrue(hasText(System.getenv("APP_DB_USERNAME")), "APP_DB_USERNAME is required for local integration check");
        assumeTrue(System.getenv("APP_DB_PASSWORD") != null, "APP_DB_PASSWORD is required for local integration check");

        String loginUrl = "http://127.0.0.1:" + port + "/api/auth/login";
        String meUrl = "http://127.0.0.1:" + port + "/api/auth/me";
        String token = null;

        try {
            ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                    loginUrl,
                    new LoginRequest("fjy", "123456"),
                    LoginResponse.class
            );

            assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(loginResponse.getBody()).isNotNull();
            assertThat(loginResponse.getBody().getToken()).isNotBlank();
            assertThat(loginResponse.getBody().getExpiresInSeconds()).isEqualTo(AuthTokenService.TOKEN_TTL_SECONDS);
            assertThat(loginResponse.getBody().getUser().getUsername()).isEqualTo("fjy");
            assertThat(loginResponse.getBody().getUser().getRole()).isEqualTo("ADMIN");

            token = loginResponse.getBody().getToken();
            Long ttl = redisTemplate.getExpire(tokenKey(token), TimeUnit.SECONDS);
            assertThat(ttl).isNotNull();
            assertThat(ttl).isBetween(86000L, AuthTokenService.TOKEN_TTL_SECONDS);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<UserResponse> meResponse = restTemplate.exchange(
                    meUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    UserResponse.class
            );
            assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(meResponse.getBody()).isNotNull();
            assertThat(meResponse.getBody().getUsername()).isEqualTo("fjy");
            assertThat(meResponse.getBody().getRole()).isEqualTo("ADMIN");

            HttpHeaders wrongHeaders = new HttpHeaders();
            wrongHeaders.setBearerAuth("bad-token-for-t4-" + UUID.randomUUID().toString().replace("-", ""));
            ResponseEntity<ApiError> wrongTokenResponse = restTemplate.exchange(
                    meUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(wrongHeaders),
                    ApiError.class
            );
            assertThat(wrongTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

            ResponseEntity<ApiError> missingTokenResponse = restTemplate.getForEntity(meUrl, ApiError.class);
            assertThat(missingTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        } finally {
            if (token != null) {
                redisTemplate.delete(tokenKey(token));
            }
        }
    }

    @Test
    void protectsAdminPingByRole() {
        assumeTrue(hasText(System.getenv("APP_DB_USERNAME")), "APP_DB_USERNAME is required for local integration check");
        assumeTrue(System.getenv("APP_DB_PASSWORD") != null, "APP_DB_PASSWORD is required for local integration check");

        String registerUrl = "http://127.0.0.1:" + port + "/api/auth/register";
        String loginUrl = "http://127.0.0.1:" + port + "/api/auth/login";
        String adminPingUrl = "http://127.0.0.1:" + port + "/api/admin/ping";
        String username = "t5_" + UUID.randomUUID().toString().replace("-", "");
        String adminToken = null;
        String userToken = null;

        try {
            adminToken = loginAndGetToken(loginUrl, "fjy", "123456");
            HttpHeaders adminHeaders = new HttpHeaders();
            adminHeaders.setBearerAuth(adminToken);
            ResponseEntity<AdminPingResponse> adminResponse = restTemplate.exchange(
                    adminPingUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(adminHeaders),
                    AdminPingResponse.class
            );
            assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(adminResponse.getBody()).isNotNull();
            assertThat(adminResponse.getBody().isOk()).isTrue();

            ResponseEntity<UserResponse> registerResponse = restTemplate.postForEntity(
                    registerUrl,
                    new RegisterRequest(username, "pass123"),
                    UserResponse.class
            );
            assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            userToken = loginAndGetToken(loginUrl, username, "pass123");
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(userToken);
            ResponseEntity<ApiError> userResponse = restTemplate.exchange(
                    adminPingUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(userHeaders),
                    ApiError.class
            );
            assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(userResponse.getBody()).isNotNull();
            assertThat(userResponse.getBody().getMessage()).isEqualTo("无权限");

            ResponseEntity<ApiError> missingTokenResponse = restTemplate.getForEntity(adminPingUrl, ApiError.class);
            assertThat(missingTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        } finally {
            if (adminToken != null) {
                redisTemplate.delete(tokenKey(adminToken));
            }
            if (userToken != null) {
                redisTemplate.delete(tokenKey(userToken));
            }
            jdbcTemplate.update("DELETE FROM users WHERE username = ?", username);
        }
    }

    private String loginAndGetToken(String loginUrl, String username, String password) {
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                loginUrl,
                new LoginRequest(username, password),
                LoginResponse.class
        );
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getToken()).isNotBlank();
        return loginResponse.getBody().getToken();
    }

    private static String tokenKey(String token) {
        return "auth:token:" + token;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

