package com.smartshop.auth.service;

import com.smartshop.auth.dto.AuthResponse;
import com.smartshop.auth.dto.LoginRequest;
import com.smartshop.auth.dto.RefreshTokenRequest;
import com.smartshop.auth.entity.RefreshToken;
import com.smartshop.auth.repository.RefreshTokenRepository;
import com.smartshop.security.JwtProperties;
import com.smartshop.security.JwtService;
import com.smartshop.user.entity.User;
import com.smartshop.user.mapper.UserMapper;
import com.smartshop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final Clock clock;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));
        return issueTokens(user);
    }

    /**
     * Rotation : le refresh token presente est revoque et remplace. Si un token deja revoque est rejoue,
     * il a probablement ete vole : toutes les sessions de l'utilisateur sont revoquees.
     * Pas de rollback sur echec, pour que cette revocation soit bien enregistree.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponse refresh(RefreshTokenRequest request) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokenRepository.findByTokenHashForUpdate(RefreshTokens.hash(request.refreshToken()))
                .orElseThrow(InvalidRefreshTokenException::new);
        User user = token.getUser();
        if (token.isRevoked()) {
            int revoked = refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now);
            log.warn("Reutilisation d'un refresh token revoque pour '{}' : {} session(s) revoquee(s)",
                    user.getUsername(), revoked);
            throw new InvalidRefreshTokenException();
        }
        if (token.isExpired(now) || !user.isEnabled()) {
            throw new InvalidRefreshTokenException();
        }
        token.revoke(now);
        return issueTokens(user);
    }

    /** Idempotent : un token inconnu ou deja revoque ne provoque pas d'erreur. */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByTokenHashForUpdate(RefreshTokens.hash(request.refreshToken()))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    private AuthResponse issueTokens(User user) {
        JwtService.AccessToken accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = RefreshTokens.generate();
        refreshTokenRepository.save(new RefreshToken(user, RefreshTokens.hash(rawRefreshToken),
                clock.instant().plus(jwtProperties.refreshTokenTtl())));
        return new AuthResponse(accessToken.value(), rawRefreshToken, AuthResponse.BEARER,
                jwtProperties.accessTokenTtl().toSeconds(), userMapper.toResponse(user));
    }
}
