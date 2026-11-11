package com.ketan.payflow.common;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {}

    // The user id always comes from the verified token, never from the request body
    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
