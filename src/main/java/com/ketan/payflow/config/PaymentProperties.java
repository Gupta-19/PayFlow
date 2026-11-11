package com.ketan.payflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payments")
public record PaymentProperties(long maxAmount) {}
