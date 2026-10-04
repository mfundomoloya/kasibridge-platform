package com.kasibridge.trader_profile.config;

import com.kasibridge.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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

    @Value(
            "${kasibridge.security.cors.allowed-origins:"
                    + "http://localhost:5173}"
    )
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf ->
                        csrf.disable()
                )

                .cors(
                        Customizer.withDefaults()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth ->
                        auth
                                /*
                                 * Browser preflight requests do not
                                 * contain the JWT and must be processed
                                 * before normal authentication.
                                 */
                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()

                                /*
                                 * Public operational endpoints.
                                 */
                                .requestMatchers(
                                        "/actuator/health",
                                        "/actuator/info",
                                        "/error"
                                )
                                .permitAll()

                                /*
                                 * Internal service-to-service lookup.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/traders/internal/**"
                                )
                                .hasRole("SYSTEM")

                                /*
                                 * Trader-owned profile endpoints.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/traders/me"
                                )
                                .hasRole("TRADER")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/traders/me"
                                )
                                .hasRole("TRADER")

                                /*
                                 * A trader can create the profile that
                                 * will be linked to the authenticated
                                 * user ID.
                                 *
                                 * A platform administrator may also
                                 * create a profile through this endpoint.
                                 */
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/traders"
                                )
                                .hasAnyRole(
                                        "TRADER",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * All remaining trader-profile APIs are
                                 * administrative operations.
                                 *
                                 * This includes listings, ID-based
                                 * retrieval, status changes, account
                                 * linking, updates and deletion.
                                 */
                                .requestMatchers(
                                        "/api/v1/traders/**"
                                )
                                .hasRole("PLATFORM_ADMIN")

                                /*
                                 * Any endpoint not explicitly listed
                                 * above still requires authentication.
                                 */
                                .anyRequest()
                                .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource
    corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        List<String> origins =
                Arrays.stream(
                                allowedOrigins.split(",")
                        )
                        .map(String::trim)
                        .filter(origin ->
                                !origin.isBlank()
                        )
                        .toList();

        configuration.setAllowedOrigins(
                origins
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
                        HttpHeaders.AUTHORIZATION,
                        HttpHeaders.CONTENT_TYPE,
                        HttpHeaders.ACCEPT,
                        HttpHeaders.ORIGIN
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        HttpHeaders.AUTHORIZATION
                )
        );

        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/api/v1/traders/**",
                configuration
        );

        return source;
    }
}