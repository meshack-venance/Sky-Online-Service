package com.shacky.materialmanagement.auth;

import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.entity.RefreshToken;
import com.shacky.materialmanagement.service.CustomerService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomerJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final CustomerService customerService;
    private final RefreshTokenService refreshTokenService;
    private final CustomerAuthCookieService customerAuthCookieService;

    public CustomerJwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            CustomerService customerService,
            RefreshTokenService refreshTokenService,
            CustomerAuthCookieService customerAuthCookieService
    ) {
        this.jwtTokenService = jwtTokenService;
        this.customerService = customerService;
        this.refreshTokenService = refreshTokenService;
        this.customerAuthCookieService = customerAuthCookieService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFromCookies(request, response);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateFromCookies(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = customerAuthCookieService.readAccessToken(request);
        if (accessToken != null && jwtTokenService.isValidAccessToken(accessToken)) {
            Claims claims = jwtTokenService.parse(accessToken);
            authenticateCustomer(jwtTokenService.extractCustomerId(claims));
            return;
        }

        String refreshTokenValue = customerAuthCookieService.readRefreshToken(request);
        if (refreshTokenValue == null || !jwtTokenService.isValidRefreshToken(refreshTokenValue)) {
            return;
        }

        Claims claims = jwtTokenService.parse(refreshTokenValue);
        Long customerId = jwtTokenService.extractCustomerId(claims);
        String tokenId = claims.getId();

        Optional<RefreshToken> refreshToken = refreshTokenService.findActiveByTokenId(tokenId);
        if (refreshToken.isEmpty() || !refreshToken.get().getCustomer().getId().equals(customerId)) {
            customerAuthCookieService.clearTokens(response);
            return;
        }

        Customer customer = refreshToken.get().getCustomer();
        JwtTokenService.RefreshTokenPayload replacementRefreshToken = jwtTokenService.generateRefreshToken(customer);
        refreshTokenService.revoke(refreshToken.get());
        refreshTokenService.create(customer, replacementRefreshToken);
        customerAuthCookieService.writeAccessToken(response, jwtTokenService.generateAccessToken(customer));
        customerAuthCookieService.writeRefreshToken(response, replacementRefreshToken.token());
        authenticateCustomer(customer.getId());
    }

    private void authenticateCustomer(Long customerId) {
        customerService.getCustomerById(customerId).ifPresent(customer -> {
            CustomerPrincipal principal = new CustomerPrincipal(customer);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });
    }
}
