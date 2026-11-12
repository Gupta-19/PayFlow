package com.ketan.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

    @Test
    void fractionalAmount_returns400() {
        Account a = newAccount();
        ResponseEntity<Map<String, Object>> res = post("/wallets/me/topup", a.token(),
                "topup-" + UUID.randomUUID(), "{\"amount\":100.5}");   // raw JSON, not a Map
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(balance(a)).isZero();
    }

    @Test
    void amountAboveMax_returns422() {
        Account a = newAccount();
        ResponseEntity<Map<String, Object>> res = post("/wallets/me/topup", a.token(),
                "topup-" + UUID.randomUUID(), Map.of("amount", 10_000_001L));
        assertThat(res.getStatusCode().value()).isEqualTo(422);
        assertThat(res.getBody().get("type")).isEqualTo("/errors/amount-too-large");
        assertThat(balance(a)).isZero();
    }

    @Test
    void missingIdempotencyKey_returns400() {
        Account a = newAccount();
        ResponseEntity<Map<String, Object>> res = post("/wallets/me/topup", a.token(), null, Map.of("amount", 100));
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(balance(a)).isZero();
    }

    @Test
    void tooLongIdempotencyKey_returns400() {
        Account a = newAccount();
        ResponseEntity<Map<String, Object>> res = post("/wallets/me/topup", a.token(), "k".repeat(101),
                Map.of("amount", 100));
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(res.getBody().get("type")).isEqualTo("/errors/invalid-idempotency-key");
        assertThat(balance(a)).isZero();
    }

    /** LedgerService.move() must stay the only code that changes a balance. */
    @Test
    void onlyLedgerServiceChangesBalances() throws IOException {
        Path root = Path.of("src/main/java");
        Path ledgerService = root.resolve("com/ketan/payflow/ledger/LedgerService.java");
        Path wallet = root.resolve("com/ketan/payflow/wallet/Wallet.java");
        Pattern call = Pattern.compile("\\.(credit|debit)\\(");

        List<Path> offenders;
        try (Stream<Path> files = Files.walk(root)) {
            offenders = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.equals(ledgerService) && !p.equals(wallet))
                    .filter(p -> {
                        try {
                            return call.matcher(Files.readString(p)).find();
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .toList();
        }
        assertThat(Files.exists(ledgerService)).isTrue();
        assertThat(offenders).as("files calling credit()/debit() outside LedgerService").isEmpty();
    }
}
