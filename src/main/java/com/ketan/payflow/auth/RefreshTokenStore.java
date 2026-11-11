package com.ketan.payflow.auth;

import java.util.Optional;
import java.util.UUID;

import com.ketan.payflow.common.Hashing;
import com.ketan.payflow.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {
    private final StringRedisTemplate redis;
    private final JwtProperties props;

    public void save(String token, UUID userId) {
        redis.opsForValue().set(key(token), userId.toString(), props.refreshTokenTtl());
    }

    /** Returns the owner and deletes the token in one atomic GETDEL, so each token works once. */
    public Optional<UUID> consume(String token) {
        String userId = redis.opsForValue().getAndDelete(key(token));
        return Optional.ofNullable(userId).map(UUID::fromString);
    }

    public void revoke(String token) {
        redis.delete(key(token));
    }

    private String key(String token) {
        return "refresh:" + Hashing.sha256Hex(token);
    }
}
