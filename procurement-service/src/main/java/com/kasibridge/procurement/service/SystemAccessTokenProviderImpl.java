package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.SystemLoginRequest;
import com.kasibridge.procurement.dto.SystemTokenResponse;
import com.kasibridge.procurement.exception.SupportTicketException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemAccessTokenProviderImpl
        implements SystemAccessTokenProvider {

    private final RestTemplate restTemplate;

    @Value("${kasibridge.services.auth.base-url}")
    private String authBaseUrl;

    @Value("${kasibridge.internal.system.username}")
    private String systemUsername;

    @Value("${kasibridge.internal.system.password}")
    private String systemPassword;

    private volatile String cachedAccessToken;

    @Override
    public synchronized String getAccessToken() {
        if (cachedAccessToken != null
                && !cachedAccessToken.isBlank()) {
            return cachedAccessToken;
        }

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

            if (body == null
                    || body.getToken() == null
                    || body.getToken().isBlank()) {
                throw new SupportTicketException(
                        "Auth service returned no system access token."
                );
            }

            if (body.getRoles() == null
                    || !body.getRoles().contains("ROLE_SYSTEM")) {
                throw new SupportTicketException(
                        "Internal service account does not have ROLE_SYSTEM."
                );
            }

            cachedAccessToken =
                    body.getToken().trim();

            log.info(
                    "System access token obtained for internal service account username={}",
                    body.getUsername()
            );

            return cachedAccessToken;

        } catch (SupportTicketException ex) {
            throw ex;

        } catch (Exception ex) {
            log.error(
                    "Unable to obtain system access token from auth-service",
                    ex
            );

            throw new SupportTicketException(
                    "Unable to authenticate internal procurement service."
            );
        }
    }

    @Override
    public synchronized void invalidateToken() {
        cachedAccessToken = null;

        log.info(
                "Cached internal system access token invalidated"
        );
    }
}