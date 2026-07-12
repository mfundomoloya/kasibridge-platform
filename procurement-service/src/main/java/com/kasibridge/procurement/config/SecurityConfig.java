package com.kasibridge.procurement.config;

import com.kasibridge.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                                // Public documentation and health endpoints
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/v3/api-docs.yaml",
                                        "/actuator/health",
                                        "/actuator/info",
                                        "/error"
                                ).permitAll()

                                // Tender creation and specification management
                                .requestMatchers(
                                        "/api/v1/tenders",
                                        "/api/v1/tenders/*/publish",
                                        "/api/v1/tenders/*/specification/**"
                                )
                                           .hasAnyRole(
                                                   "SPECIFICATION_OFFICER", "PLATFORM_ADMIN"
                )

                                //committee assignment / admin oversight
                                .requestMatchers(
                                       "/api/v1/tenders/*/committee/**"
                )
                                .hasAnyRole(
                                "PLATFORM_ADMIN", "SPECIFICATION_OFFICER"
                )

                                //bid submission by traders
                                .requestMatchers(
                                    "/api/v1/tenders/*/bids/**"
                )
                                .hasAnyRole(
                                        "TRADER", "SYSTEM", "PLATFORM_ADMIN"
                )

                                //evaluation endpoints
                                .requestMatchers(
                                    "/api/v1/tenders/*/evaluation/**"
                )
                                .hasAnyRole(
                                        "EVALUATOR", "PLATFORM_ADMIN"
                )

                                //adjudication endpoints
                                .requestMatchers(
                                        "/api/v1/tenders/*/adjudication/**"
                )
                                .hasAnyRole("ADJUDICATOR", "PLATFORM_ADMIN"
                )

                                //general tender read access
                                .requestMatchers(
                                            "/api/v1/tenders/**"
                )
                                .hasAnyRole(
                                            "TRADER", "SPECIFICATION_OFFICER", "EVALUATOR", "ADJUDICATOR", "SYSTEM", "PLATFORM_ADMIN"
                )

                                //everything else requires authentication
                                .anyRequest().authenticated()
                )

                .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}