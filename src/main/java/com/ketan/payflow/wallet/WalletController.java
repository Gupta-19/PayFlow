package com.ketan.payflow.wallet;

import java.util.UUID;

import com.ketan.payflow.common.CurrentUser;
import com.ketan.payflow.common.PageResponse;
import com.ketan.payflow.idempotency.IdempotencyService;
import com.ketan.payflow.wallet.dto.LedgerEntryResponse;
import com.ketan.payflow.wallet.dto.TopUpRequest;
import com.ketan.payflow.wallet.dto.WalletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets/me")
@RequiredArgsConstructor
class WalletController {
    private final WalletService walletService;
    private final IdempotencyService idempotency;

    @GetMapping
    WalletResponse get(@AuthenticationPrincipal Jwt jwt) {
        return walletService.get(CurrentUser.id(jwt));
    }

    @GetMapping("/transactions")
    PageResponse<LedgerEntryResponse> history(@AuthenticationPrincipal Jwt jwt,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return walletService.history(CurrentUser.id(jwt), page, size);
    }

    @PostMapping("/topup")
    ResponseEntity<?> topUp(@AuthenticationPrincipal Jwt jwt,
                            @RequestHeader("Idempotency-Key") String key,
                            @Valid @RequestBody TopUpRequest req,
                            HttpServletRequest http) {
        UUID userId = CurrentUser.id(jwt);
        return idempotency.execute(userId, key, http.getRequestURI(), req, HttpStatus.CREATED,
                () -> walletService.topUp(userId, req.amount(), key));
    }
}
