package com.deployfast.taskmanager.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Composant utilitaire JWT :
 * - Génération du token (access + refresh)
 * - Validation
 * - Extraction des claims
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${application.jwt.secret}")
    private String jwtSecret;

    @Value("${application.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${application.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ─── Génération ───────────────────────────────────────────────────────────

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, jwtExpirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = Map.of("type", "refresh");
        return generateToken(claims, userDetails, refreshExpirationMs);
    }

    private String generateToken(Map<String, Object> extraClaims,
                                  UserDetails userDetails,
                                  long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    // ─── Extraction des claims ────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
