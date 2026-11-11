package com.ketan.payflow.auth.dto;

import java.util.UUID;

public record SignupResponse(UUID userId, UUID walletId) {}
