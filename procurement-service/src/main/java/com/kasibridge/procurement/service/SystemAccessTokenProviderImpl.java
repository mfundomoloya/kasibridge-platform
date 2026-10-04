package com.kasibridge.procurement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.procurement.dto.SystemLoginRequest;
import com.kasibridge.procurement.dto.SystemTokenResponse;
import com.kasibridge.procurement.exception.SupportTicketException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemAccessTokenProviderImpl
        implements SystemAccessTokenProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kasibridge.services.auth.base-url:http://localhost:8084}")
    private String authBaseUrl;

    @Value("${kasibridge.internal.system.username:procurement-system}")
    private String systemUsername;

    @Value("${kasibridge.internal.system.password:password}")
    private String systemPassword;

    @Value("${kasibridge.internal.system.token-refresh-skew-seconds:60}")
    private long tokenRefreshSkewSeconds;

    private volatile String cachedAccessToken;

    private volatile Instant cachedTokenExpiresAt;

    @Override
    public synchronized String getAccessToken() {
        if (isCachedTokenUsable()) {
            return cachedAccessToken;
        }

        return requestNewAccessToken();
    }

    @Override
    public synchronized void invalidateToken() {
        cachedAccessToken = null;
        cachedTokenExpiresAt = null;

        log.info(
                "Cached internal system access token invalidated"
        );
    }

    private boolean isCachedTokenUsable() {
        if (cachedAccessToken == null
                || cachedAccessToken.isBlank()
                || cachedTokenExpiresAt == null) {
            return false;
        }

        Instant refreshThreshold =
                Instant.now().plusSeconds(tokenRefreshSkewSeconds);

        boolean usable =
                cachedTokenExpiresAt.isAfter(refreshThreshold);

        if (!usable) {
            log.info(
                    "Cached internal system token is expired or approaching expiry. "
                            + "expiresAt={} refreshSkewSeconds={}",
                    cachedTokenExpiresAt,
                    tokenRefreshSkewSeconds
            );
        }

        return usable;
    }

    private String requestNewAccessToken() {
        String loginUrl =
                authBaseUrl + "/api/v1/auth/login";

        SystemLoginRequest request =
                new SystemLoginRequest(
                        systemUsername,
                        systemPassword
                );

        try {
            ResponseEntity<SystemTokenResponse> response =
                    restTemplate.postForEntity(
                            loginUrl,
                            request,
                            SystemTokenResponse.class
                    );

            SystemTokenResponse body =
                    response.getBody();

            validateTokenResponse(body);

            String accessToken =
                    body.getToken().trim();

            Instant expiresAt =
                    extractExpiration(accessToken);

            cachedAccessToken = accessToken;
            cachedTokenExpiresAt = expiresAt;

            log.info(
                    "System access token obtained: username={} expiresAt={}",
                    body.getUsername(),
                    expiresAt
            );

            return cachedAccessToken;

        } catch (SupportTicketException ex) {
            invalidateToken();
            throw ex;

        } catch (Exception ex) {
            invalidateToken();

            log.error(
                    "Unable to obtain system access token from auth-service",
                    ex
            );

            throw new SupportTicketException(
                    "Unable to authenticate internal procurement service."
            );
        }
    }

    private void validateTokenResponse(
            SystemTokenResponse response
    ) {
        if (response == null) {
            throw new SupportTicketException(
                    "Auth service returned an empty system authentication response."
            );
        }

        if (response.getToken() == null
                || response.getToken().isBlank()) {
            throw new SupportTicketException(
                    "Auth service returned no system access token."
            );
        }

        if (response.getRoles() == null
                || !response.getRoles().contains("ROLE_SYSTEM")) {
            throw new SupportTicketException(
                    "Internal service account does not have ROLE_SYSTEM."
            );
        }
    }

    private Instant extractExpiration(
            String accessToken
    ) {
        try {
            String[] tokenParts =
                    accessToken.split("\\.");

            if (tokenParts.length != 3) {
                throw new SupportTicketException(
                        "Internal system access token is not a valid JWT."
                );
            }

            byte[] decodedPayload =
                    Base64.getUrlDecoder()
                            .decode(tokenParts[1]);

            String payloadJson =
                    new String(
                            decodedPayload,
                            StandardCharsets.UTF_8
                    );

            JsonNode payload =
                    objectMapper.readTree(payloadJson);

            JsonNode expirationClaim =
                    payload.get("exp");

            if (expirationClaim == null
                    || !expirationClaim.canConvertToLong()) {
                throw new SupportTicketException(
                        "Internal system access token does not contain a valid exp claim."
                );
            }

            return Instant.ofEpochSecond(
                    expirationClaim.asLong()
            );

        } catch (SupportTicketException ex) {
            throw ex;

        } catch (Exception ex) {
            log.error(
                    "Unable to read expiration from internal system JWT",
                    ex
            );

            throw new SupportTicketException(
                    "Unable to determine internal system access token expiration."
            );
        }
    }
}