package com.ketan.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;

class IdempotencyTest extends ApiTestBase {

    @Autowired StringRedisTemplate redis;

    @Test
    void samePaymentTwice_chargedOnce() {
        Account a = newAccount();
        Account b = newAccount();
        topUp(a, 10_000, "topup-" + UUID.randomUUID());
        String key = "pay-" + UUID.randomUUID();

        ResponseEntity<Map<String, Object>> first = payEntity(a, b.walletId(), 2_500, key);
        ResponseEntity<Map<String, Object>> second = payEntity(a, b.walletId(), 2_500, key);

        assertThat(first.getStatusCode().value()).isEqualTo(201);
        assertThat(first.getHeaders().getFirst("Idempotent-Replayed")).isNull();
        assertThat(second.getStatusCode().value()).isEqualTo(201);
        assertThat(second.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true");
        assertThat(second.getBody().get("id")).isEqualTo(first.getBody().get("id"));
        assertThat(balance(a)).isEqualTo(7_500);
        assertThat(balance(b)).isEqualTo(2_500);
    }

    @Test
    void sameKeyDifferentBody_returns422() {
        Account a = newAccount();
        Account b = newAccount();
        topUp(a, 10_000, "topup-" + UUID.randomUUID());
        String key = "pay-" + UUID.randomUUID();

        assertThat(pay(a, b.walletId(), 2_500, key)).isEqualTo(201);
        ResponseEntity<Map<String, Object>> reused = payEntity(a, b.walletId(), 3_000, key);
        assertThat(reused.getStatusCode().value()).isEqualTo(422);
        assertThat(reused.getBody().get("type")).isEqualTo("/errors/idempotency-key-reused");
        assertThat(balance(a)).isEqualTo(7_500);
        assertThat(balance(b)).isEqualTo(2_500);
    }

    @Test
    void failedPaymentReplays422() {
        Account a = newAccount();
        Account b = newAccount();
        topUp(a, 1_000, "topup-" + UUID.randomUUID());
        String key = "pay-" + UUID.randomUUID();

        ResponseEntity<Map<String, Object>> first = payEntity(a, b.walletId(), 5_000, key);
        assertThat(first.getStatusCode().value()).isEqualTo(422);
        assertThat(first.getBody().get("type")).isEqualTo("/errors/insufficient-funds");
        assertThat(balance(a)).isEqualTo(1_000);

        topUp(a, 10_000, "topup-" + UUID.randomUUID());   // now it would succeed, but the key's answer is final
        ResponseEntity<Map<String, Object>> retry = payEntity(a, b.walletId(), 5_000, key);
        assertThat(retry.getStatusCode().value()).isEqualTo(422);
        assertThat(retry.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true");
        assertThat(retry.getBody().get("type")).isEqualTo("/errors/insufficient-funds");
        assertThat(retry.getBody().get("detail")).isEqualTo(first.getBody().get("detail"));
        assertThat(balance(a)).isEqualTo(11_000);   // only the two top-ups; the replay moved nothing
        assertThat(balance(b)).isZero();
    }

    /** Simulates Redis losing this key (only this test's key, so other state is untouched). */
    private void forgetKey(String key) {
        Set<String> keys = redis.keys("idem:*:" + key);
        assertThat(keys).hasSize(1);
        redis.delete(keys);
    }

    private ResponseEntity<Map<String, Object>> topUpEntity(Account account, long amount, String key) {
        return post("/wallets/me/topup", account.token(), key, Map.of("amount", amount));
    }

    @Test
    void topUpRedisLost_dbBackstopReturnsOriginal() {
        Account a = newAccount();
        String key = "topup-" + UUID.randomUUID();

        ResponseEntity<Map<String, Object>> first = topUpEntity(a, 5_000, key);
        assertThat(first.getStatusCode().value()).isEqualTo(201);
        forgetKey(key);

        ResponseEntity<Map<String, Object>> retry = topUpEntity(a, 5_000, key);
        assertThat(retry.getStatusCode().value()).isEqualTo(201);
        assertThat(retry.getHeaders().getFirst("Idempotent-Replayed")).isNull();   // answered by the DB backstop
        assertThat(retry.getBody().get("id")).isEqualTo(first.getBody().get("id"));
        assertThat(balance(a)).isEqualTo(5_000);
    }

    @Test
    void topUpRedisLost_differentAmount_returns422() {
        Account a = newAccount();
        String key = "topup-" + UUID.randomUUID();

        assertThat(topUpEntity(a, 5_000, key).getStatusCode().value()).isEqualTo(201);
        forgetKey(key);

        ResponseEntity<Map<String, Object>> reused = topUpEntity(a, 6_000, key);
        assertThat(reused.getStatusCode().value()).isEqualTo(422);
        assertThat(reused.getBody().get("type")).isEqualTo("/errors/idempotency-key-reused");
        assertThat(balance(a)).isEqualTo(5_000);
    }

    @Test
    void paymentRedisLost_differentAmount_returns422() {
        Account a = newAccount();
        Account b = newAccount();
        topUp(a, 10_000, "topup-" + UUID.randomUUID());
        String key = "pay-" + UUID.randomUUID();

        assertThat(pay(a, b.walletId(), 2_500, key)).isEqualTo(201);
        forgetKey(key);

        ResponseEntity<Map<String, Object>> reused = payEntity(a, b.walletId(), 3_000, key);
        assertThat(reused.getStatusCode().value()).isEqualTo(422);
        assertThat(reused.getBody().get("type")).isEqualTo("/errors/idempotency-key-reused");
        assertThat(balance(a)).isEqualTo(7_500);
        assertThat(balance(b)).isEqualTo(2_500);
    }

    @Test
    void redisFlushed_dbBackstopReturnsOriginal() {
        Account a = newAccount();
        Account b = newAccount();
        topUp(a, 10_000, "topup-" + UUID.randomUUID());
        String key = "pay-" + UUID.randomUUID();

        ResponseEntity<Map<String, Object>> first = payEntity(a, b.walletId(), 2_500, key);
        assertThat(first.getStatusCode().value()).isEqualTo(201);

        forgetKey(key);

        ResponseEntity<Map<String, Object>> retry = payEntity(a, b.walletId(), 2_500, key);   // JWT still valid
        assertThat(retry.getStatusCode().value()).isEqualTo(201);
        assertThat(retry.getHeaders().getFirst("Idempotent-Replayed")).isNull();   // answered by the DB backstop
        assertThat(retry.getBody().get("id")).isEqualTo(first.getBody().get("id"));
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM payments p JOIN users u ON u.id = p.created_by
                WHERE u.email = ? AND p.idempotency_key = ?""", Integer.class, a.email(), key)).isEqualTo(1);
        assertThat(balance(a)).isEqualTo(7_500);
        assertThat(balance(b)).isEqualTo(2_500);
    }
}
