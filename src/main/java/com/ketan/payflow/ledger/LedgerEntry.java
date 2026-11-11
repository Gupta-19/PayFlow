package com.ketan.payflow.ledger;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry {
    @Id
    private UUID id;
    private UUID paymentId;
    private UUID walletId;
    @Enumerated(EnumType.STRING)
    private Direction direction;      // DEBIT or CREDIT
    private long amount;
    private Instant createdAt;

    public static LedgerEntry debit(UUID paymentId, UUID walletId, long amount) {
        return of(paymentId, walletId, Direction.DEBIT, amount);
    }

    public static LedgerEntry credit(UUID paymentId, UUID walletId, long amount) {
        return of(paymentId, walletId, Direction.CREDIT, amount);
    }

    private static LedgerEntry of(UUID paymentId, UUID walletId, Direction direction, long amount) {
        LedgerEntry e = new LedgerEntry();
        e.id = UUID.randomUUID();
        e.paymentId = paymentId;
        e.walletId = walletId;
        e.direction = direction;
        e.amount = amount;
        e.createdAt = Instant.now();
        return e;
    }
}
