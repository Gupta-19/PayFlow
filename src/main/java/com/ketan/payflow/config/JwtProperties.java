package com.ketan.payflow.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        RSAPrivateKey privateKey,   // Spring Security converts the PEM file into a key object
        RSAPublicKey publicKey) {}
