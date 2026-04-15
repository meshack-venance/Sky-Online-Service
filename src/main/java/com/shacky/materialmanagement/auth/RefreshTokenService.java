package com.shacky.materialmanagement.auth;

import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.entity.RefreshToken;
import com.shacky.materialmanagement.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken create(Customer customer, JwtTokenService.RefreshTokenPayload payload) {
        revokeAllForCustomer(customer);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCustomer(customer);
        refreshToken.setTokenId(payload.tokenId());
        refreshToken.setExpiresAt(payload.expiresAt());
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findActiveByTokenId(String tokenId) {
        return refreshTokenRepository.findByTokenIdAndRevokedFalse(tokenId)
                .filter(token -> token.getExpiresAt() != null && token.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Transactional
    public void revokeAllForCustomer(Customer customer) {
        refreshTokenRepository.findAllByCustomerAndRevokedFalse(customer)
                .forEach(token -> token.setRevoked(true));
    }

    @Transactional
    public void revoke(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
    }
}
