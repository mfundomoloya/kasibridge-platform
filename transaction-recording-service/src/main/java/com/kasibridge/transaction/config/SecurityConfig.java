package com.kasibridge.transaction.config;

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
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth

                            .requestMatchers(
                                    "/actuator/health",
                                    "/actuator/info",
                                    "/error"
                            ).permitAll()

                            .requestMatchers("/api/v1/transactions/**")
                            .hasAnyRole("TRADER",
                                    "SYSTEM",
                                    "PLATFORM_ADMIN")
                    )

                    .addFilterBefore(
                            jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class
                    );
            return http.build();
    }
}
