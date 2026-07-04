package com.kasibridge.fraud.config;


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

    private final JwtAuthenticationFilter  jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(crsf -> crsf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        //public endpoints
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info").permitAll()

                        //internal service-to-service fraud analysis
                        .requestMatchers("/api/v1/fraud/analyze")
                        .hasAnyRole("SYSTEM", "PLATFORM_ADMIN")

                        //escalation is platform admin only
                        .requestMatchers("/api/v1/fraud/alerts/*/escalate")
                        .hasAnyRole("PLATFORM_ADMIN")

                        //review and dismiss are analyst/admin actions
                        .requestMatchers(
                                "/api/v1/fraud/alerts/*/review",
                                "/api/v1/fraud/alerts/*/dismiss")
                        .hasAnyRole("ANALYST","PLATFORM_ADMIN")

                        //fraud read/search/dashboard endpoints
                        .requestMatchers("/api/v1/fraud/**")
                        .hasAnyRole("ANALYST","PLATFORM_ADMIN")

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
