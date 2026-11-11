package com.ketan.payflow.wallet.dto;

import jakarta.validation.constraints.Positive;

public record TopUpRequest(@Positive long amount) {}
