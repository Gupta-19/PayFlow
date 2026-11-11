package com.ketan.payflow.wallet.dto;

import java.time.Instant;
import java.util.UUID;

import com.ketan.payflow.ledger.LedgerEntry;

public record LedgerEntryResponse(UUID paymentId, String direction, long amount, Instant createdAt) {
    public static LedgerEntryResponse from(LedgerEntry e) {
        return new LedgerEntryResponse(e.getPaymentId(), e.getDirection().name(), e.getAmount(), e.getCreatedAt());
    }
}
