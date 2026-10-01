package com.smartshop.security;

import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final JwtProperties PROPERTIES = new JwtProperties(
            "unit-test-secret-0123456789-abcdefghijklmnop", Duration.ofMinutes(15), Duration.ofDays(7));

    private final JwtConfig jwtConfig = new JwtConfig();
    private final JwtDecoder decoder = jwtConfig.jwtDecoder(PROPERTIES);

    @Test
    void generatedTokenContainsIdentityAndRole() {
        JwtService service = service(Clock.systemUTC());

        JwtService.AccessToken token = service.generateAccessToken(user());
        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo("sami");
        assertThat(jwt.getClaimAsString(JwtService.ROLE_CLAIM)).isEqualTo("SELLER");
        assertThat(jwt.<Long>getClaim(JwtService.USER_ID_CLAIM)).isEqualTo(7L);
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtConfig.ISSUER);
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void expiredTokenIsRejected() {
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        JwtService service = service(Clock.fixed(oneHourAgo, ZoneOffset.UTC));

        String token = service.generateAccessToken(user()).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtProperties otherSecret = new JwtProperties(
                "another-secret-0123456789-abcdefghijklmnopq", Duration.ofMinutes(15), Duration.ofDays(7));
        JwtService forger = new JwtService(jwtConfig.jwtEncoder(otherSecret), otherSecret, Clock.systemUTC());

        String token = forger.generateAccessToken(user()).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private JwtService service(Clock clock) {
        return new JwtService(jwtConfig.jwtEncoder(PROPERTIES), PROPERTIES, clock);
    }

    private static User user() {
        User user = new User("sami", "sami@shop.tn", "hash", "Sami", "Ben Ali", Role.SELLER);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }
}
