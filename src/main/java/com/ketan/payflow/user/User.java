package com.ketan.payflow.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    private UUID id;
    private String email;
    private String passwordHash;
    private Instant createdAt;

    public static User create(String email, String passwordHash) {
        User u = new User();
        u.id = UUID.randomUUID();
        u.email = email;
        u.passwordHash = passwordHash;
        u.createdAt = Instant.now();
        return u;
    }
}
