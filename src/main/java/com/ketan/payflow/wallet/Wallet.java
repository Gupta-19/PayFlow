package com.ketan.payflow.wallet;

import java.time.Instant;
import java.util.UUID;

import com.ketan.payflow.common.InsufficientFundsException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {
    public static final UUID SYSTEM_WALLET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;
    private UUID userId;              // null for the SYSTEM wallet
    @Enumerated(EnumType.STRING)
    private WalletKind kind;          // USER or SYSTEM
    private long balance;             // paise
    private String currency;
    private Instant updatedAt;

    public static Wallet forUser(UUID userId) {
        Wallet w = new Wallet();
        w.id = UUID.randomUUID();
        w.userId = userId;
        w.kind = WalletKind.USER;
        w.currency = "INR";
        w.updatedAt = Instant.now();
        return w;
    }

    // Only LedgerService.move() may call credit() and debit()
    public void credit(long amount) {
        balance += amount;
        updatedAt = Instant.now();
    }

    public void debit(long amount) {
        if (kind == WalletKind.USER && balance < amount) {   // the SYSTEM wallet may go negative
            throw new InsufficientFundsException(balance, amount);
        }
        balance -= amount;
        updatedAt = Instant.now();
    }
}
