package com.ketan.payflow.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import com.ketan.payflow.auth.dto.LoginRequest;
import com.ketan.payflow.auth.dto.SignupRequest;
import com.ketan.payflow.auth.dto.SignupResponse;
import com.ketan.payflow.auth.dto.TokenResponse;
import com.ketan.payflow.common.EmailTakenException;
import com.ketan.payflow.common.InvalidCredentialsException;
import com.ketan.payflow.common.PasswordTooLongException;
import com.ketan.payflow.config.JwtProperties;
import com.ketan.payflow.user.User;
import com.ketan.payflow.user.UserRepository;
import com.ketan.payflow.wallet.Wallet;
import com.ketan.payflow.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final RefreshTokenStore refreshTokens;
    private final JwtProperties props;

    @Transactional
    public SignupResponse signup(SignupRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        // BCrypt only uses the first 72 bytes; @Size counts chars, so multi-byte passwords need this check
        if (req.password().getBytes(StandardCharsets.UTF_8).length > 72) throw new PasswordTooLongException();
        if (users.existsByEmail(email)) throw new EmailTakenException();
        User user = User.create(email, passwordEncoder.encode(req.password()));
        try {
            users.saveAndFlush(user);   // the UNIQUE constraint catches two signups racing
        } catch (DataIntegrityViolationException e) {
            throw new EmailTakenException();
        }
        Wallet wallet = wallets.save(Wallet.forUser(user.getId()));   // every user gets a wallet
        return new SignupResponse(user.getId(), wallet.getId());
    }

    public TokenResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().trim().toLowerCase(Locale.ROOT))
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);   // same error for unknown email and wrong password
        return issueTokens(user);
    }

    public TokenResponse refresh(String refreshToken) {
        UUID userId = refreshTokens.consume(refreshToken).orElseThrow(InvalidCredentialsException::new);
        User user = users.findById(userId).orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user);   // rotation: the old refresh token is already gone
    }

    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    private TokenResponse issueTokens(User user) {
        String access = tokens.issueAccessToken(user);
        String refresh = tokens.newRefreshToken();
        refreshTokens.save(refresh, user.getId());
        return new TokenResponse(access, refresh, props.accessTokenTtl().toSeconds());
    }
}
