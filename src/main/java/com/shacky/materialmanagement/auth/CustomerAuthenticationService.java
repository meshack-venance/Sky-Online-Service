package com.shacky.materialmanagement.auth;

import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.service.CustomerService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerAuthenticationService {

    private final CustomerService customerService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final CustomerAuthCookieService customerAuthCookieService;

    public CustomerAuthenticationService(
            CustomerService customerService,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            CustomerAuthCookieService customerAuthCookieService
    ) {
        this.customerService = customerService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.customerAuthCookieService = customerAuthCookieService;
    }

    public Optional<Customer> authenticate(String identifier, String rawPassword, HttpServletResponse response) {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim();

        Optional<Customer> customer;
        try {
            customer = customerService.findByPhoneNumber(Integer.parseInt(normalizedIdentifier));
        } catch (NumberFormatException e) {
            customer = customerService.findByEmail(normalizedIdentifier);
        }

        if (customer.isPresent() && customerService.passwordMatches(customer.get(), rawPassword)) {
            issueTokens(customer.get(), response);
            return customer;
        }

        customerAuthCookieService.clearTokens(response);
        return Optional.empty();
    }

    public void logout(Customer customer, HttpServletResponse response) {
        refreshTokenService.revokeAllForCustomer(customer);
        customerAuthCookieService.clearTokens(response);
    }

    public void clearTokens(HttpServletResponse response) {
        customerAuthCookieService.clearTokens(response);
    }

    private void issueTokens(Customer customer, HttpServletResponse response) {
        String accessToken = jwtTokenService.generateAccessToken(customer);
        JwtTokenService.RefreshTokenPayload refreshToken = jwtTokenService.generateRefreshToken(customer);
        refreshTokenService.create(customer, refreshToken);
        customerAuthCookieService.writeAccessToken(response, accessToken);
        customerAuthCookieService.writeRefreshToken(response, refreshToken.token());
    }
}
