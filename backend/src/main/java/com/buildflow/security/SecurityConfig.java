package com.buildflow.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(
    SecurityProperties.class
)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(
            12
        );
    }

    @Bean
    public SecurityFilterChain
        securityFilterChain(
            HttpSecurity http,
            OpaqueSessionAuthenticationFilter
                authenticationFilter,
            JsonAuthenticationEntryPoint
                authenticationEntryPoint,
            JsonAccessDeniedHandler
                accessDeniedHandler,
            CorsConfigurationSource
                corsConfigurationSource,
            SecurityProperties properties
        ) throws Exception {

        CookieCsrfTokenRepository
            csrfRepository =
                CookieCsrfTokenRepository
                    .withHttpOnlyFalse();

        csrfRepository.setCookieName(
            "BF-XSRF-TOKEN"
        );

        csrfRepository.setHeaderName(
            "X-XSRF-TOKEN"
        );

        csrfRepository.setCookiePath(
            "/"
        );

        csrfRepository
            .setCookieCustomizer(
                builder ->
                    builder
                        .sameSite("Lax")
                        .secure(
                            properties
                                .cookieSecure()
                        )
            );

        CsrfTokenRequestAttributeHandler
            csrfRequestHandler =
                new CsrfTokenRequestAttributeHandler();

        http
            .cors(
                cors ->
                    cors.configurationSource(
                        corsConfigurationSource
                    )
            )
            .csrf(
                csrf ->
                    csrf
                        .csrfTokenRepository(
                            csrfRepository
                        )
                        .csrfTokenRequestHandler(
                            csrfRequestHandler
                        )
            )
            .sessionManagement(
                session ->
                    session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                    )
            )
            .httpBasic(
                AbstractHttpConfigurer::disable
            )
            .formLogin(
                AbstractHttpConfigurer::disable
            )
            .logout(
                AbstractHttpConfigurer::disable
            )
            .exceptionHandling(
                exceptions ->
                    exceptions
                        .authenticationEntryPoint(
                            authenticationEntryPoint
                        )
                        .accessDeniedHandler(
                            accessDeniedHandler
                        )
            )
            .authorizeHttpRequests(
                authorization ->
                    authorization
                        .requestMatchers(
                            HttpMethod.OPTIONS,
                            "/**"
                        )
                        .permitAll()

                        .requestMatchers(
                            HttpMethod.GET,
                            "/api/v1/system/health",
                            "/actuator/health",
                            "/api/v1/auth/csrf"
                        )
                        .permitAll()

                        .requestMatchers(
                            HttpMethod.POST,
                            "/api/v1/auth/register",
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/logout",
                            "/api/v1/auth/forgot-password",
                            "/api/v1/auth/reset-password",
                            "/api/v1/auth/verify-email"
                        )
                        .permitAll()

                        .anyRequest()
                        .authenticated()
            )
            .addFilterBefore(
                authenticationFilter,
                AnonymousAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource
        corsConfigurationSource(
            SecurityProperties properties
        ) {

        CorsConfiguration configuration =
            new CorsConfiguration();

        configuration.setAllowedOrigins(
            List.of(
                properties.frontendUrl()
            )
        );

        configuration.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
            )
        );

        configuration.setAllowedHeaders(
            List.of(
                "Content-Type",
                "Accept",
                "X-XSRF-TOKEN",
                "X-Organization-Id",
                "X-Trace-Id"
            )
        );

        configuration.setExposedHeaders(
            List.of(
                "X-Trace-Id"
            )
        );

        configuration.setAllowCredentials(
            true
        );

        configuration.setMaxAge(
            3600L
        );

        UrlBasedCorsConfigurationSource
            source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return source;
    }
}
