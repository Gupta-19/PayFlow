package com.ketan.payflow.wallet;

import com.ketan.payflow.common.CurrentUser;
import com.ketan.payflow.common.PageResponse;
import com.ketan.payflow.payment.dto.PaymentResponse;
import com.ketan.payflow.wallet.dto.LedgerEntryResponse;
import com.ketan.payflow.wallet.dto.TopUpRequest;
import com.ketan.payflow.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets/me")
@RequiredArgsConstructor
class WalletController {
    private final WalletService walletService;

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
    @ResponseStatus(HttpStatus.CREATED)
    PaymentResponse topUp(@AuthenticationPrincipal Jwt jwt,
                          @RequestHeader("Idempotency-Key") String idempotencyKey,
                          @Valid @RequestBody TopUpRequest req) {
        return walletService.topUp(CurrentUser.id(jwt), req.amount(), idempotencyKey);
    }
}
