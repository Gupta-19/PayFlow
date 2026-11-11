package com.ketan.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class AuthTest extends ApiTestBase {

    @Test
    void signupThenLogin_returnsTokens() {
        String email = UUID.randomUUID() + "@test.com";
        ResponseEntity<Map<String, Object>> signup = post("/auth/signup", null, null,
                Map.of("email", email, "password", PASSWORD));
        assertThat(signup.getStatusCode().value()).isEqualTo(201);
        assertThat(signup.getBody()).containsKeys("userId", "walletId");

        ResponseEntity<Map<String, Object>> login = post("/auth/login", null, null,
                Map.of("email", email, "password", PASSWORD));
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        assertThat((String) login.getBody().get("accessToken")).isNotBlank();
        assertThat((String) login.getBody().get("refreshToken")).isNotBlank();
        assertThat(((Number) login.getBody().get("expiresIn")).longValue()).isEqualTo(900);

        ResponseEntity<Map<String, Object>> me = get("/wallets/me", (String) login.getBody().get("accessToken"));
        assertThat(me.getStatusCode().value()).isEqualTo(200);
        assertThat(me.getBody().get("walletId")).isEqualTo(signup.getBody().get("walletId"));
    }

    @Test
    void duplicateEmail_returns409() {
        Account a = newAccount();
        ResponseEntity<Map<String, Object>> again = post("/auth/signup", null, null,
                Map.of("email", a.email(), "password", PASSWORD));
        assertThat(again.getStatusCode().value()).isEqualTo(409);
        assertThat(again.getBody().get("type")).isEqualTo("/errors/email-taken");
    }

    @Test
    void noToken_returns401() {
        assertThat(get("/wallets/me", null).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void tamperedToken_returns401() {
        Account a = newAccount();
        String token = a.token();
        int i = token.length() - 10;   // a character well inside the signature
        char replacement = token.charAt(i) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, i) + replacement + token.substring(i + 1);

        assertThat(get("/wallets/me", a.token()).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/wallets/me", tampered).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void refresh_worksOnce() {
        Account a = newAccount();
        ResponseEntity<Map<String, Object>> first = post("/auth/refresh", null, null,
                Map.of("refreshToken", a.refreshToken()));
        assertThat(first.getStatusCode().value()).isEqualTo(200);
        assertThat((String) first.getBody().get("accessToken")).isNotBlank();
        assertThat(first.getBody().get("refreshToken")).isNotEqualTo(a.refreshToken());

        ResponseEntity<Map<String, Object>> second = post("/auth/refresh", null, null,
                Map.of("refreshToken", a.refreshToken()));
        assertThat(second.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void logout_revokesRefreshToken() {
        Account a = newAccount();
        ResponseEntity<Map<String, Object>> logout = post("/auth/logout", a.token(), null,
                Map.of("refreshToken", a.refreshToken()));
        assertThat(logout.getStatusCode().value()).isEqualTo(204);

        ResponseEntity<Map<String, Object>> refresh = post("/auth/refresh", null, null,
                Map.of("refreshToken", a.refreshToken()));
        assertThat(refresh.getStatusCode().value()).isEqualTo(401);
    }
}
