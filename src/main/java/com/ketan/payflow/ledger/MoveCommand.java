package com.ketan.payflow.ledger;

import java.util.UUID;

import com.ketan.payflow.payment.PaymentType;

public record MoveCommand(
        PaymentType type,
        UUID fromWalletId,
        UUID toWalletId,
        long amount,
        UUID createdBy,
        String idempotencyKey,
        String note) {}
