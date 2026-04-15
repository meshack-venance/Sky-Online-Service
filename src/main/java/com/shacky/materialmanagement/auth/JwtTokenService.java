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
    private SecretKey secretKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void init() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.auth.jwt.secret must be configured and at least 32 characters long");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
                .signWith(secretKey)
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
                .signWith(secretKey)
                .compact();

        return new RefreshTokenPayload(tokenId, token, LocalDateTime.ofInstant(expiration, ZoneOffset.UTC));
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseIgnoringExpiration(String token) {
        try {
            return parse(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public boolean isValidAccessToken(String token) {
        return isValidTokenType(token, "access");
    }

    public boolean isValidRefreshToken(String token) {
        return isValidTokenType(token, "refresh");
    }

    public boolean isExpired(String token) {
        try {
            parse(token);
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

    private boolean isValidTokenType(String token, String expectedType) {
        try {
            Claims claims = parse(token);
            return expectedType.equals(claims.get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    public record RefreshTokenPayload(String tokenId, String token, LocalDateTime expiresAt) {
    }
}
