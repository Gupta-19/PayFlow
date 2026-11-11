package com.ketan.payflow.payment;

import java.util.UUID;

import com.ketan.payflow.common.CurrentUser;
import com.ketan.payflow.idempotency.IdempotencyService;
import com.ketan.payflow.payment.dto.PayRequest;
import com.ketan.payflow.payment.dto.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
class PaymentController {
    private final PaymentService payments;
    private final IdempotencyService idempotency;

    @PostMapping
    ResponseEntity<?> pay(@AuthenticationPrincipal Jwt jwt,
                          @RequestHeader("Idempotency-Key") String key,
                          @Valid @RequestBody PayRequest req,
                          HttpServletRequest http) {
        UUID userId = CurrentUser.id(jwt);
        return idempotency.execute(userId, key, http.getRequestURI(), req, HttpStatus.CREATED,
                () -> payments.pay(userId, req, key));
    }

    @GetMapping("/{id}")
    PaymentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return payments.get(CurrentUser.id(jwt), id);
    }
}
