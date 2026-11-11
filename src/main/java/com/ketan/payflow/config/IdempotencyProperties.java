package com.ketan.payflow.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.idempotency")
public record IdempotencyProperties(Duration inProgressTtl, Duration completedTtl) {}
