package com.ketan.payflow.payment.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PayRequest(
        @NotNull UUID toWalletId,
        @Positive long amount,
        @Size(max = 140) String note) {}
