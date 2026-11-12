package com.ketan.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class PaymentTest extends ApiTestBase {

    private static String key() {
        return "k-" + UUID.randomUUID();
    }

    private int ledgerRows(UUID walletId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entries WHERE wallet_id = ?", Integer.class, walletId);
    }

    @Test
    void transfer_movesMoney_twoLedgerEntries() {
        Account a = newAccount();
        Account b = newAccount();
        topUp(a, 10_000, key());

        ResponseEntity<Map<String, Object>> res = payEntity(a, b.walletId(), 2_500, key());
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        assertThat(res.getBody().get("type")).isEqualTo("TRANSFER");
        assertThat(res.getBody().get("status")).isEqualTo("COMPLETED");
        assertThat(balance(a)).isEqualTo(7_500);
        assertThat(balance(b)).isEqualTo(2_500);

        UUID paymentId = UUID.fromString((String) res.getBody().get("id"));
        List<Map<String, Object>> entries = jdbc.queryForList(
                "SELECT wallet_id, direction, amount FROM ledger_entries WHERE payment_id = ? ORDER BY direction",
                paymentId);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).get("direction")).isEqualTo("CREDIT");
        assertThat(entries.get(0).get("wallet_id")).isEqualTo(b.walletId());
        assertThat(((Number) entries.get(0).get("amount")).longValue()).isEqualTo(2_500);
        assertThat(entries.get(1).get("direction")).isEqualTo("DEBIT");
        assertThat(entries.get(1).get("wallet_id")).isEqualTo(a.walletId());
        assertThat(((Number) entries.get(1).get("amount")).longValue()).isEqualTo(2_500);
    }

    @Test
    void insufficientFunds_returns422_nothingChanges() {
        Account a = newAccount();
        Account b = newAccount();
        topUp(a, 1_000, key());

        ResponseEntity<Map<String, Object>> res = payEntity(a, b.walletId(), 5_000, key());
        assertThat(res.getStatusCode().value()).isEqualTo(422);
        assertThat(res.getBody().get("type")).isEqualTo("/errors/insufficient-funds");

        assertThat(balance(a)).isEqualTo(1_000);
        assertThat(balance(b)).isZero();
        assertThat(ledgerRows(a.walletId())).isEqualTo(1);   // only the top-up
        assertThat(ledgerRows(b.walletId())).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE from_wallet_id = ? AND type = 'TRANSFER'",
                Integer.class, a.walletId())).isZero();
    }

    @Test
    void selfPayment_returns400() {
        Account a = newAccount();
        topUp(a, 1_000, key());
        ResponseEntity<Map<String, Object>> res = payEntity(a, a.walletId(), 100, key());
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(res.getBody().get("type")).isEqualTo("/errors/self-payment");
        assertThat(balance(a)).isEqualTo(1_000);
    }

    @Test
    void unknownRecipient_returns404() {
        Account a = newAccount();
        topUp(a, 1_000, key());
        ResponseEntity<Map<String, Object>> res = payEntity(a, UUID.randomUUID(), 100, key());
        assertThat(res.getStatusCode().value()).isEqualTo(404);
        assertThat(res.getBody().get("type")).isEqualTo("/errors/recipient-not-found");

        UUID systemWallet = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertThat(pay(a, systemWallet, 100, key())).isEqualTo(404);   // the SYSTEM wallet can't be paid
        assertThat(balance(a)).isEqualTo(1_000);
    }

    @Test
    void metrics_countPaymentsByOutcome() {
        Account a = newAccount();
        Account b = newAccount();
        topUp(a, 1_000, key());
        double completedBefore = paymentCount("TRANSFER", "completed");
        double insufficientBefore = paymentCount("TRANSFER", "insufficient_funds");

        assertThat(pay(a, b.walletId(), 500, key())).isEqualTo(201);
        assertThat(pay(a, b.walletId(), 5_000, key())).isEqualTo(422);

        assertThat(paymentCount("TRANSFER", "completed") - completedBefore).isEqualTo(1.0);
        assertThat(paymentCount("TRANSFER", "insufficient_funds") - insufficientBefore).isEqualTo(1.0);

        ResponseEntity<String> res = http.get().uri("/actuator/prometheus").retrieve().toEntity(String.class);   // public
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody())
                .containsPattern("payflow_payments_total\\{[^}]*outcome=\"completed\"[^}]*type=\"TRANSFER\"")
                .containsPattern("payflow_payments_total\\{[^}]*outcome=\"insufficient_funds\"[^}]*type=\"TRANSFER\"");
    }

    @Test
    void otherUsersPayment_returns404() {
        Account a = newAccount();
        Account b = newAccount();
        Account c = newAccount();
        topUp(a, 1_000, key());
        ResponseEntity<Map<String, Object>> res = payEntity(a, b.walletId(), 100, key());
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        String paymentId = (String) res.getBody().get("id");

        assertThat(get("/payments/" + paymentId, a.token()).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/payments/" + paymentId, b.token()).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Map<String, Object>> other = get("/payments/" + paymentId, c.token());
        assertThat(other.getStatusCode().value()).isEqualTo(404);
        assertThat(other.getBody().get("type")).isEqualTo("/errors/payment-not-found");
    }
}
