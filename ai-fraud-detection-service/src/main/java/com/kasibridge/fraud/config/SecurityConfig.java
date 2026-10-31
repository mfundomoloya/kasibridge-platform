package com.kasibridge.fraud.config;

import com.kasibridge.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter
            jwtAuthenticationFilter;

    @Value("${kasibridge.security.cors.allowed-origins:"
                    + "http://localhost:5173}"
    )
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource
    corsConfigurationSource() {
        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                Arrays.stream(
                                allowedOrigins.split(",")
                        )
                        .map(String::trim)
                        .filter(
                                origin ->
                                        !origin.isBlank()
                        )
                        .toList()
        );

        configuration.setAllowedMethods(
                List.of(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name()
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Location"
                )
        );

        /*
         * KasiBridge authenticates this API with
         * bearer tokens rather than browser cookies.
         */
        configuration.setAllowCredentials(
                false
        );

        configuration.setMaxAge(
                3600L
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .cors(
                        Customizer.withDefaults()
                )
                .csrf(
                        csrf ->
                                csrf.disable()
                )
                .sessionManagement(
                        session ->
                                session
                                        .sessionCreationPolicy(
                                                SessionCreationPolicy
                                                        .STATELESS
                                        )
                )
                .authorizeHttpRequests(
                        authorization ->
                                authorization
                                        /*
                                         * Browser CORS preflight requests
                                         * must be processed before JWT
                                         * authentication is required.
                                         */
                                        .requestMatchers(
                                                HttpMethod.OPTIONS,
                                                "/**"
                                        )
                                        .permitAll()

                                        /*
                                         * Public documentation and health
                                         * endpoints.
                                         */
                                        .requestMatchers(
                                                "/api/v1/auth/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/v3/api-docs/**",
                                                "/v3/api-docs.yaml",
                                                "/actuator/health",
                                                "/actuator/info",
                                                "/error"
                                        )
                                        .permitAll()

                                        /*
                                         * Internal service-to-service
                                         * fraud analysis.
                                         */
                                        .requestMatchers(
                                                "/api/v1/fraud/analyze"
                                        )
                                        .hasAnyRole(
                                                "SYSTEM",
                                                "PLATFORM_ADMIN"
                                        )

                                        /*
                                         * Invoice prediction and persisted
                                         * assessment creation.
                                         */
                                        .requestMatchers(
                                                "/api/v1/fraud/invoices/predict",
                                                "/api/v1/fraud/invoices/assess"
                                        )
                                        .hasAnyRole(
                                                "SYSTEM",
                                                "ANALYST",
                                                "PLATFORM_ADMIN"
                                        )

                                        /*
                                         * Assessment queue, dashboard,
                                         * and individual assessment
                                         * retrieval.
                                         */
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/v1/fraud/assessments",
                                                "/api/v1/fraud/assessments/*"
                                        )
                                        .hasAnyRole(
                                                "ANALYST",
                                                "PLATFORM_ADMIN"
                                        )

                                        /*
                                         * Analyst and administrator final
                                         * review decisions.
                                         */
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/v1/fraud/assessments/*/confirm",
                                                "/api/v1/fraud/assessments/*/false-positive"
                                        )
                                        .hasAnyRole(
                                                "ANALYST",
                                                "PLATFORM_ADMIN"
                                        )

                                        /*
                                         * Assessment escalation remains an
                                         * administrator-only action.
                                         */
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/v1/fraud/assessments/*/escalate"
                                        )
                                        .hasRole(
                                                "PLATFORM_ADMIN"
                                        )

                                        /*
                                         * Existing fraud-alert escalation.
                                         */
                                        .requestMatchers(
                                                "/api/v1/fraud/alerts/*/escalate"
                                        )
                                        .hasRole(
                                                "PLATFORM_ADMIN"
                                        )

                                        /*
                                         * Existing fraud-alert review and
                                         * dismissal actions.
                                         */
                                        .requestMatchers(
                                                "/api/v1/fraud/alerts/*/review",
                                                "/api/v1/fraud/alerts/*/dismiss"
                                        )
                                        .hasAnyRole(
                                                "ANALYST",
                                                "PLATFORM_ADMIN"
                                        )

                                        /*
                                         * Remaining fraud read, search, and
                                         * dashboard endpoints.
                                         */
                                        .requestMatchers(
                                                "/api/v1/fraud/**"
                                        )
                                        .hasAnyRole(
                                                "ANALYST",
                                                "PLATFORM_ADMIN"
                                        )

                                        /*
                                         * Everything else requires a valid
                                         * authenticated principal.
                                         */
                                        .anyRequest()
                                        .authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter
                                .class
                );

        return http.build();
    }
}