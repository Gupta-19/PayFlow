package com.ketan.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMetrics   // keep the Prometheus endpoint on in tests (on the base class, so all tests share one context)
abstract class ApiTestBase {
    static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};
    static final String PASSWORD = "password123";

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired MeterRegistry meters;
    RestClient http;

    record Account(String token, UUID walletId, String email, String refreshToken) {}

    @BeforeEach
    void setUpClient() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> {})   // never throw; tests assert the status
                .build();
    }

    Account newAccount() {
        String email = UUID.randomUUID() + "@test.com";   // unique, so tests never clash
        Map<String, Object> signup = post("/auth/signup", null, null,
                Map.of("email", email, "password", PASSWORD)).getBody();
        Map<String, Object> login = post("/auth/login", null, null,
                Map.of("email", email, "password", PASSWORD)).getBody();
        return new Account((String) login.get("accessToken"), UUID.fromString((String) signup.get("walletId")),
                email, (String) login.get("refreshToken"));
    }

    /** POST with optional bearer token and Idempotency-Key; returns status, headers and JSON body. */
    ResponseEntity<Map<String, Object>> post(String path, String token, String idempotencyKey, Object body) {
        return http.post().uri(path)
                .headers(h -> {
                    if (token != null) h.setBearerAuth(token);
                    if (idempotencyKey != null) h.set("Idempotency-Key", idempotencyKey);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().toEntity(JSON);
    }

    ResponseEntity<Map<String, Object>> get(String path, String token) {
        return http.get().uri(path)
                .headers(h -> {
                    if (token != null) h.setBearerAuth(token);
                })
                .retrieve().toEntity(JSON);
    }

    ResponseEntity<Map<String, Object>> payEntity(Account from, UUID toWallet, long amount, String key) {
        return post("/payments", from.token(), key, Map.of("toWalletId", toWallet, "amount", amount));
    }

    int pay(Account from, UUID toWallet, long amount, String key) {
        return payEntity(from, toWallet, amount, key).getStatusCode().value();
    }

    int topUp(Account account, long amount, String key) {
        return post("/wallets/me/topup", account.token(), key, Map.of("amount", amount)).getStatusCode().value();
    }

    long balance(Account account) {
        ResponseEntity<Map<String, Object>> res = get("/wallets/me", account.token());
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        return ((Number) res.getBody().get("balance")).longValue();
    }

    /** Current value of the payflow.payments counter (tests run one at a time, so deltas are exact). */
    double paymentCount(String type, String outcome) {
        return meters.counter("payflow.payments", "type", type, "outcome", outcome).count();
    }

    @AfterEach
    void ledgerStillBalances() {   // checked after every single test
        assertThat(jdbc.queryForObject("SELECT SUM(balance) FROM wallets", Long.class)).isZero();
        Integer mismatched = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                  SELECT w.id FROM wallets w LEFT JOIN ledger_entries l ON l.wallet_id = w.id
                  GROUP BY w.id, w.balance
                  HAVING w.balance <> COALESCE(SUM(CASE l.direction WHEN 'CREDIT' THEN l.amount ELSE -l.amount END), 0)
                ) bad""", Integer.class);
        assertThat(mismatched).isZero();
    }
}
