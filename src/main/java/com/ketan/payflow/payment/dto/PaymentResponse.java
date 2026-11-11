package com.ketan.payflow.payment.dto;

import java.time.Instant;
import java.util.UUID;

import com.ketan.payflow.payment.Payment;

public record PaymentResponse(UUID id, String type, UUID fromWalletId, UUID toWalletId,
                              long amount, String status, String note, Instant createdAt) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getType().name(), p.getFromWalletId(), p.getToWalletId(),
                p.getAmount(), p.getStatus().name(), p.getNote(), p.getCreatedAt());
    }
}
