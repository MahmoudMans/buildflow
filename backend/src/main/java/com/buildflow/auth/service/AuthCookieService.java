package com.buildflow.auth.service;

import com.buildflow.security.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    public static final String ACCESS_COOKIE =
        "BF_ACCESS";

    public static final String REFRESH_COOKIE =
        "BF_REFRESH";

    private final SecurityProperties properties;

    public AuthCookieService(
        SecurityProperties properties
    ) {
        this.properties = properties;
    }

    public void writeSession(
        HttpServletResponse response,
        SessionService.IssuedSession session
    ) {
        writeCookie(
            response,
            ACCESS_COOKIE,
            session.accessToken(),
            properties.accessTokenTtl()
        );

        writeCookie(
            response,
            REFRESH_COOKIE,
            session.refreshToken(),
            properties.refreshTokenTtl()
        );
    }

    public void clearSession(
        HttpServletResponse response
    ) {
        clearCookie(
            response,
            ACCESS_COOKIE
        );

        clearCookie(
            response,
            REFRESH_COOKIE
        );
    }

    public String readAccessToken(
        HttpServletRequest request
    ) {
        return readCookie(
            request,
            ACCESS_COOKIE
        );
    }

    public String readRefreshToken(
        HttpServletRequest request
    ) {
        return readCookie(
            request,
            REFRESH_COOKIE
        );
    }

    private String readCookie(
        HttpServletRequest request,
        String name
    ) {
        Cookie[] cookies =
            request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (
                name.equals(
                    cookie.getName()
                )
            ) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void writeCookie(
        HttpServletResponse response,
        String name,
        String value,
        Duration maxAge
    ) {
        ResponseCookie cookie =
            ResponseCookie
                .from(name, value)
                .httpOnly(true)
                .secure(
                    properties.cookieSecure()
                )
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookie.toString()
        );
    }

    private void clearCookie(
        HttpServletResponse response,
        String name
    ) {
        ResponseCookie cookie =
            ResponseCookie
                .from(name, "")
                .httpOnly(true)
                .secure(
                    properties.cookieSecure()
                )
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookie.toString()
        );
    }
}
