package com.ketan.payflow.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(UUID paymentId, UUID fromWalletId, UUID toWalletId,
                                    long amount, Instant completedAt) {}
