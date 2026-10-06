package com.kasibridge.procurement.config;

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

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value(
            "${kasibridge.security.cors.allowed-origins:http://localhost:5173}"
    )
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth ->
                        auth
                                /*
                                 * CORS preflight requests.
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
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs",
                                        "/v3/api-docs/**",
                                        "/v3/api-docs.yaml",
                                        "/actuator/health",
                                        "/actuator/info",
                                        "/error"
                                )
                                .permitAll()

                                /*
                                 * WhatsApp webhook verification
                                 * and message reception.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/webhooks/whatsapp"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/webhooks/whatsapp"
                                )
                                .permitAll()

                                /*
                                 * Trader-visible tender catalogue.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/tenders/open",
                                        "/api/v1/tenders/open/{id}"
                                )
                                .hasAnyRole(
                                        "TRADER",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Bid submission.
                                 *
                                 * Traders may submit bids but may not
                                 * retrieve the complete tender bid list.
                                 */
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/tenders/{tenderId}/bids"
                                )
                                .hasAnyRole(
                                        "TRADER",
                                        "SYSTEM",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Anonymized bid retrieval.
                                 *
                                 * The controller returns
                                 * AnonymizedBidResponse objects.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/tenders/{tenderId}/bids"
                                )
                                .hasAnyRole(
                                        "EVALUATOR",
                                        "ADJUDICATOR",
                                        "SPECIFICATION_OFFICER",
                                        "SYSTEM",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Individual bid endpoints.
                                 *
                                 * Traders must not receive unrestricted
                                 * access to bids belonging to other
                                 * traders.
                                 */
                                .requestMatchers(
                                        "/api/v1/tenders/{tenderId}/bids/{bidId}"
                                )
                                .hasAnyRole(
                                        "EVALUATOR",
                                        "ADJUDICATOR",
                                        "SYSTEM",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Tender creation and internal listing.
                                 */
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/tenders"
                                )
                                .hasAnyRole(
                                        "SPECIFICATION_OFFICER",
                                        "PLATFORM_ADMIN"
                                )

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/tenders"
                                )
                                .hasAnyRole(
                                        "SPECIFICATION_OFFICER",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Specification and tender-state
                                 * management.
                                 */
                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/tenders/{id}/publish",
                                        "/api/v1/tenders/{id}/close-bidding",
                                        "/api/v1/tenders/{id}/start-evaluation",
                                        "/api/v1/tenders/{id}/start-adjudication"
                                )
                                .hasAnyRole(
                                        "SPECIFICATION_OFFICER",
                                        "PLATFORM_ADMIN"
                                )

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/tenders/{id}/specification/verify"
                                )
                                .hasAnyRole(
                                        "SPECIFICATION_OFFICER",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Internal tender details.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/tenders/{id}",
                                        "/api/v1/tenders/reference/{tenderReference}"
                                )
                                .hasAnyRole(
                                        "SPECIFICATION_OFFICER",
                                        "EVALUATOR",
                                        "ADJUDICATOR",
                                        "SYSTEM",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Committee management.
                                 */
                                .requestMatchers(
                                        "/api/v1/tenders/{id}/committee",
                                        "/api/v1/tenders/{id}/committee/{assignmentId}"
                                )
                                .hasAnyRole(
                                        "PLATFORM_ADMIN",
                                        "SPECIFICATION_OFFICER"
                                )

                                /*
                                 * Evaluation endpoints.
                                 */
                                .requestMatchers(
                                        "/api/v1/tenders/{id}/evaluation",
                                        "/api/v1/tenders/{id}/evaluation/{evaluationId}"
                                )
                                .hasAnyRole(
                                        "EVALUATOR",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Adjudication endpoints.
                                 */
                                .requestMatchers(
                                        "/api/v1/tenders/{id}/adjudication",
                                        "/api/v1/tenders/{id}/adjudication/{decisionId}"
                                )
                                .hasAnyRole(
                                        "ADJUDICATOR",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Procurement audit events.
                                 */
                                .requestMatchers(
                                        "/api/v1/procurement/audit-events",
                                        "/api/v1/procurement/audit-events/{eventId}",
                                        "/api/v1/tenders/{id}/audit-events",
                                        "/api/v1/bids/{bidId}/audit-events"
                                )
                                .hasAnyRole(
                                        "PLATFORM_ADMIN",
                                        "ADJUDICATOR",
                                        "SPECIFICATION_OFFICER"
                                )

                                /*
                                 * Procurement anomaly endpoints.
                                 */
                                .requestMatchers(
                                        "/api/v1/procurement/anomalies",
                                        "/api/v1/procurement/anomalies/{anomalyId}",
                                        "/api/v1/tenders/{id}/anomalies",
                                        "/api/v1/tenders/{id}/anomalies/{anomalyId}",
                                        "/api/v1/tenders/{id}/anomaly-records"
                                )
                                .hasAnyRole(
                                        "PLATFORM_ADMIN",
                                        "ADJUDICATOR",
                                        "SPECIFICATION_OFFICER"
                                )

                                /*
                                 * Ticket assignment.
                                 */
                                .requestMatchers(
                                        "/api/v1/tickets/{id}/assign"
                                )
                                .hasRole(
                                        "PLATFORM_ADMIN"
                                )

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/tickets/unassigned"
                                )
                                .hasRole(
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Assigned-ticket queues.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/tickets/assigned-to-me",
                                        "/api/v1/tickets/assigned-to-me/status/{status}"
                                )
                                .hasAnyRole(
                                        "SPECIFICATION_OFFICER",
                                        "ADJUDICATOR",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * Ticket workflow actions.
                                 */
                                .requestMatchers(
                                        "/api/v1/tickets/{id}/start-review",
                                        "/api/v1/tickets/{id}/respond",
                                        "/api/v1/tickets/{id}/reject",
                                        "/api/v1/tickets/{id}/close",
                                        "/api/v1/tickets/{id}/return-to-queue",
                                        "/api/v1/tickets/{id}/publish-clarification"
                                )
                                .hasAnyRole(
                                        "PLATFORM_ADMIN",
                                        "SPECIFICATION_OFFICER"
                                )

                                /*
                                 * Trader and support-review ticket
                                 * access.
                                 */
                                .requestMatchers(
                                        "/api/v1/tenders/{id}/tickets",
                                        "/api/v1/tickets/my",
                                        "/api/v1/tickets/{id}",
                                        "/api/v1/tenders/{id}/clarifications"
                                )
                                .hasAnyRole(
                                        "TRADER",
                                        "PLATFORM_ADMIN",
                                        "SPECIFICATION_OFFICER"
                                )

                                /*
                                 * In-app notifications.
                                 */
                                .requestMatchers(
                                        "/api/v1/notifications/outbox/in-app",
                                        "/api/v1/notifications/outbox/in-app/unread",
                                        "/api/v1/notifications/outbox/in-app/read",
                                        "/api/v1/notifications/outbox/in-app/{id}/read"
                                )
                                .hasAnyRole(
                                        "PLATFORM_ADMIN",
                                        "ADJUDICATOR",
                                        "SPECIFICATION_OFFICER"
                                )

                                /*
                                 * Notification outbox administration.
                                 */
                                .requestMatchers(
                                        "/api/v1/notifications/outbox",
                                        "/api/v1/notifications/outbox/{id}",
                                        "/api/v1/notifications/outbox/status/{status}",
                                        "/api/v1/notifications/outbox/{id}/sent",
                                        "/api/v1/notifications/outbox/{id}/failed"
                                )
                                .hasAnyRole(
                                        "PLATFORM_ADMIN",
                                        "SPECIFICATION_OFFICER"
                                )

                                /*
                                 * AI assessment workflow actions.
                                 */
                                .requestMatchers(
                                        "/api/v1/ticket-ai-assessments/{id}/approve",
                                        "/api/v1/ticket-ai-assessments/{id}/reject",
                                        "/api/v1/ticket-ai-assessments/{id}/escalate",
                                        "/api/v1/ticket-ai-assessments/{id}/publish-response"
                                )
                                .hasAnyRole(
                                        "SPECIFICATION_OFFICER",
                                        "PLATFORM_ADMIN"
                                )

                                .requestMatchers(
                                        "/api/v1/tickets/{id}/ai-assessment",
                                        "/api/v1/ticket-ai-assessments",
                                        "/api/v1/ticket-ai-assessments/{id}"
                                )
                                .hasAnyRole(
                                        "SPECIFICATION_OFFICER",
                                        "PLATFORM_ADMIN"
                                )

                                /*
                                 * All other endpoints require a valid
                                 * authenticated user.
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
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        List<String> origins = Arrays.stream(
                        allowedOrigins.split(",")
                )
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        configuration.setAllowedOrigins(origins);

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
                "/api/**",
                configuration
        );

        return source;
    }
}