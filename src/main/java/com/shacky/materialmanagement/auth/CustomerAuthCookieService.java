package com.shacky.materialmanagement.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class CustomerAuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE = "customer_access_token";
    public static final String REFRESH_TOKEN_COOKIE = "customer_refresh_token";

    private final JwtProperties jwtProperties;

    public CustomerAuthCookieService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public void writeAccessToken(HttpServletResponse response, String token) {
        addCookie(response, ACCESS_TOKEN_COOKIE, token,
                (int) (jwtProperties.getAccessTokenExpirationMinutes() * 60));
    }

    public void writeRefreshToken(HttpServletResponse response, String token) {
        addCookie(response, REFRESH_TOKEN_COOKIE, token,
                (int) (jwtProperties.getRefreshTokenExpirationDays() * 24 * 60 * 60));
    }

    public void clearTokens(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", 0);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", 0);
    }

    public String readAccessToken(HttpServletRequest request) {
        return readCookie(request, ACCESS_TOKEN_COOKIE);
    }

    public String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, REFRESH_TOKEN_COOKIE);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(jwtProperties.isCookieSecure());
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
