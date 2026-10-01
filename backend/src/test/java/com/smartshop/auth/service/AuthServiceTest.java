package com.smartshop.auth.service;

import com.smartshop.auth.dto.AuthResponse;
import com.smartshop.auth.dto.LoginRequest;
import com.smartshop.auth.dto.RefreshTokenRequest;
import com.smartshop.auth.entity.RefreshToken;
import com.smartshop.auth.repository.RefreshTokenRepository;
import com.smartshop.security.JwtProperties;
import com.smartshop.security.JwtService;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import com.smartshop.user.mapper.UserMapper;
import com.smartshop.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
    private static final JwtProperties JWT_PROPERTIES =
            new JwtProperties("unit-test-secret-0123456789-abcdefghijklmnop", Duration.ofMinutes(15), Duration.ofDays(7));

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, userRepository, refreshTokenRepository, jwtService,
                JWT_PROPERTIES, Mappers.getMapper(UserMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User("sami", "sami@shop.tn", "hash", "Sami", "Ben Ali", Role.SELLER);
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    void login_withValidCredentials_issuesAccessAndHashedRefreshToken() {
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated("sami", null, List.of()));
        when(userRepository.findByUsername("sami")).thenReturn(Optional.of(user));
        stubAccessToken();

        AuthResponse response = authService.login(new LoginRequest("sami", "Secret123!"));

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.user().username()).isEqualTo("sami");
        assertThat(saved.getValue().getTokenHash())
                .isEqualTo(RefreshTokens.hash(response.refreshToken()))
                .isNotEqualTo(response.refreshToken());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void login_withBadCredentials_propagatesAndIssuesNothing() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("sami", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_withValidToken_rotatesToken() {
        RefreshToken current = new RefreshToken(user, RefreshTokens.hash("raw"), NOW.plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokens.hash("raw"))).thenReturn(Optional.of(current));
        stubAccessToken();

        AuthResponse response = authService.refresh(new RefreshTokenRequest("raw"));

        assertThat(current.getRevokedAt()).isEqualTo(NOW);
        assertThat(response.refreshToken()).isNotEqualTo("raw");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_withUnknownToken_isRejected() {
        when(refreshTokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("unknown")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_withExpiredToken_isRejected() {
        RefreshToken expired = new RefreshToken(user, RefreshTokens.hash("raw"), NOW);
        when(refreshTokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("raw")))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_forDisabledUser_isRejected() {
        user.disable();
        RefreshToken token = new RefreshToken(user, RefreshTokens.hash("raw"), NOW.plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("raw")))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_withRevokedToken_revokesAllUserSessions() {
        RefreshToken reused = new RefreshToken(user, RefreshTokens.hash("raw"), NOW.plus(Duration.ofDays(1)));
        reused.revoke(NOW.minus(Duration.ofMinutes(5)));
        when(refreshTokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(reused));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("raw")))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokenRepository).revokeAllActiveByUserId(1L, NOW);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_revokesToken() {
        RefreshToken token = new RefreshToken(user, RefreshTokens.hash("raw"), NOW.plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokens.hash("raw"))).thenReturn(Optional.of(token));

        authService.logout(new RefreshTokenRequest("raw"));

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void logout_withUnknownToken_doesNotFail() {
        when(refreshTokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        authService.logout(new RefreshTokenRequest("unknown"));
    }

    private void stubAccessToken() {
        when(jwtService.generateAccessToken(user))
                .thenReturn(new JwtService.AccessToken("access-token", NOW.plus(Duration.ofMinutes(15))));
    }
}
