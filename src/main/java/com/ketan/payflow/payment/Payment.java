package com.ketan.payflow.payment;

import java.time.Instant;
import java.util.UUID;

import com.ketan.payflow.ledger.MoveCommand;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    private PaymentType type;         // TOPUP or TRANSFER
    private UUID fromWalletId;
    private UUID toWalletId;
    private long amount;              // paise
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;     // COMPLETED
    private String note;
    private UUID createdBy;
    private String idempotencyKey;
    private Instant createdAt;

    public static Payment completed(MoveCommand cmd) {
        Payment p = new Payment();
        p.id = UUID.randomUUID();
        p.type = cmd.type();
        p.fromWalletId = cmd.fromWalletId();
        p.toWalletId = cmd.toWalletId();
        p.amount = cmd.amount();
        p.status = PaymentStatus.COMPLETED;
        p.note = cmd.note();
        p.createdBy = cmd.createdBy();
        p.idempotencyKey = cmd.idempotencyKey();
        p.createdAt = Instant.now();
        return p;
    }
}
