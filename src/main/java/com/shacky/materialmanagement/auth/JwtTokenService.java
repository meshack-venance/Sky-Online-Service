package com.shacky.materialmanagement.auth;

import com.shacky.materialmanagement.entity.Customer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private SecretKey accessSecretKey;
    private SecretKey refreshSecretKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void init() {
        this.accessSecretKey = toKey(jwtProperties.getAccessSecret(), "app.auth.jwt.access-secret");
        this.refreshSecretKey = toKey(jwtProperties.getRefreshSecret(), "app.auth.jwt.refresh-secret");
    }

    public String generateAccessToken(Customer customer) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.getAccessTokenExpirationMinutes() * 60);

        return Jwts.builder()
                .subject(String.valueOf(customer.getId()))
                .claim("type", "access")
                .claim("email", customer.getEmail())
                .claim("phoneNumber", customer.getPhoneNumber())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(accessSecretKey)
                .compact();
    }

    public RefreshTokenPayload generateRefreshToken(Customer customer) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.getRefreshTokenExpirationDays() * 24 * 60 * 60);
        String tokenId = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .subject(String.valueOf(customer.getId()))
                .id(tokenId)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(refreshSecretKey)
                .compact();

        return new RefreshTokenPayload(tokenId, token, LocalDateTime.ofInstant(expiration, ZoneOffset.UTC));
    }

    public Claims parse(String token) {
        return parseAccessToken(token);
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(accessSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(refreshSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseAccessTokenIgnoringExpiration(String token) {
        try {
            return parseAccessToken(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public Claims parseRefreshTokenIgnoringExpiration(String token) {
        try {
            return parseRefreshToken(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public boolean isValidAccessToken(String token) {
        return isValidTokenType(token, "access", accessSecretKey);
    }

    public boolean isValidRefreshToken(String token) {
        return isValidTokenType(token, "refresh", refreshSecretKey);
    }

    public boolean isExpired(String token) {
        try {
            parseAccessToken(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return true;
        }
    }

    public Long extractCustomerId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    private boolean isValidTokenType(String token, String expectedType, SecretKey secretKey) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return expectedType.equals(claims.get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    private SecretKey toKey(String secret, String propertyName) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(propertyName + " must be configured and at least 32 characters long");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public record RefreshTokenPayload(String tokenId, String token, LocalDateTime expiresAt) {
    }
}
