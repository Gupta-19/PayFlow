package com.ketan.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class WalletTest extends ApiTestBase {

    @Test
    @SuppressWarnings("unchecked")
    void topUp_creditsWalletAndLedger() {
        Account a = newAccount();
        assertThat(balance(a)).isZero();

        assertThat(topUp(a, 50_000, "topup-" + UUID.randomUUID())).isEqualTo(201);
        assertThat(balance(a)).isEqualTo(50_000);

        ResponseEntity<Map<String, Object>> history = get("/wallets/me/transactions", a.token());
        assertThat(history.getStatusCode().value()).isEqualTo(200);
        List<Map<String, Object>> items = (List<Map<String, Object>>) history.getBody().get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("direction")).isEqualTo("CREDIT");
        assertThat(((Number) items.get(0).get("amount")).longValue()).isEqualTo(50_000);
        assertThat(((Number) history.getBody().get("totalItems")).longValue()).isEqualTo(1);
    }
}
