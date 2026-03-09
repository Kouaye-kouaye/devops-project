package com.deployfast.taskmanager.security;

import org.junit.jupiter.api.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du composant JWT.
 * Vérifie génération, validation et extraction des claims.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserDetails testUser;

    private static final String SECRET =
            "404D635166546A576E5A7234753778214125442A472D4B6150645367566B5970";
    private static final long EXPIRATION_MS = 3600000L;        // 1h
    private static final long REFRESH_EXPIRATION_MS = 7200000L; // 2h

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpirationMs", REFRESH_EXPIRATION_MS);

        testUser = new User("user@test.io", "hashedpassword",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // ─── Génération ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateAccessToken() — retourne un token non nul et non vide")
    void generateAccessToken_returnsNonBlankToken() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateAccessToken() — deux tokens différents pour le même user")
    void generateAccessToken_generatesDifferentTokensEachTime() throws InterruptedException {
        String token1 = jwtTokenProvider.generateAccessToken(testUser);
        Thread.sleep(10); // Assure que la date est différente
        String token2 = jwtTokenProvider.generateAccessToken(testUser);
        assertThat(token1).isNotEqualTo(token2);
    }

    // ─── Extraction ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUsername() — retourne l'email du user")
    void extractUsername_returnsCorrectEmail() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("user@test.io");
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValid() — retourne true pour un token valide et user correspondant")
    void isTokenValid_withValidTokenAndMatchingUser_returnsTrue() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        assertThat(jwtTokenProvider.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() — retourne false pour un token altéré")
    void isTokenValid_withTamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        String tamperedToken = token + "tampered";
        assertThat(jwtTokenProvider.isTokenValid(tamperedToken, testUser)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() — retourne false si l'email ne correspond pas")
    void isTokenValid_withDifferentUser_returnsFalse() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        UserDetails differentUser = new User("autre@test.io", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertThat(jwtTokenProvider.isTokenValid(token, differentUser)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() — retourne false pour un token expiré")
    void isTokenValid_withExpiredToken_returnsFalse() {
        // Token avec expiration à -1ms = déjà expiré
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1L);
        String expiredToken = jwtTokenProvider.generateAccessToken(testUser);
        assertThat(jwtTokenProvider.isTokenValid(expiredToken, testUser)).isFalse();
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateRefreshToken() — retourne un token valide distinct de l'access token")
    void generateRefreshToken_returnsDistinctValidToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        assertThat(refreshToken).isNotBlank().isNotEqualTo(accessToken);
        assertThat(jwtTokenProvider.extractUsername(refreshToken)).isEqualTo("user@test.io");
    }
}
