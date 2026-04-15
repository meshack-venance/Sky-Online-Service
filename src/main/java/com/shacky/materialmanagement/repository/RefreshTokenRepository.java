package com.shacky.materialmanagement.repository;

import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenIdAndRevokedFalse(String tokenId);
    List<RefreshToken> findAllByCustomerAndRevokedFalse(Customer customer);
}
