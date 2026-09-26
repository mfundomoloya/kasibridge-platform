package com.kasibridge.procurement.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kasibridge.procurement.dto.WhatsAppDeliveryResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "kasibridge.notifications.whatsapp.enabled",
        havingValue = "true"
)
@Slf4j
public class MetaWhatsAppProvider implements WhatsAppProvider {

    @PostConstruct
    void logProviderActivation() {
        log.info("Real Meta WhatsApp provider is active.");
    }
    private final RestTemplate restTemplate;

    @Value("${kasibridge.notifications.whatsapp.graph-api-base-url:https://graph.facebook.com}")
    private String graphApiBaseUrl;

    @Value("${kasibridge.notifications.whatsapp.api-version}")
    private String apiVersion;

    @Value("${kasibridge.notifications.whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${kasibridge.notifications.whatsapp.access-token}")
    private String accessToken;

    @Override
    public WhatsAppDeliveryResult sendMessage(
            String recipientPhone,
            String message
    ) {
        String validationFailure = validateConfiguration(
                recipientPhone,
                message
        );

        if (validationFailure != null) {
            return WhatsAppDeliveryResult.failure(validationFailure);
        }

        String normalizedPhone = normalizePhoneNumber(recipientPhone);

        String endpoint = String.format(
                "%s/%s/%s/messages",
                removeTrailingSlash(graphApiBaseUrl),
                apiVersion,
                phoneNumberId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", normalizedPhone,
                "type", "text",
                "text", Map.of(
                        "preview_url", false,
                        "body", message
                )
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<MetaSendMessageResponse> response =
                    restTemplate.postForEntity(
                            endpoint,
                            request,
                            MetaSendMessageResponse.class
                    );

            MetaSendMessageResponse responseBody = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                return WhatsAppDeliveryResult.failure(
                        "WhatsApp provider returned HTTP "
                                + response.getStatusCode().value()
                );
            }

            String providerMessageId = extractProviderMessageId(
                    responseBody
            );

            if (providerMessageId == null) {
                return WhatsAppDeliveryResult.failure(
                        "WhatsApp provider accepted the request but returned no message ID."
                );
            }

            log.info(
                    "WhatsApp message accepted: providerMessageId={} recipientPhone={}",
                    providerMessageId,
                    maskPhoneNumber(normalizedPhone)
            );

            return WhatsAppDeliveryResult.success(providerMessageId);

        } catch (HttpStatusCodeException ex) {
            log.warn(
                    "WhatsApp provider rejected message: status={} recipientPhone={}",
                    ex.getStatusCode().value(),
                    maskPhoneNumber(normalizedPhone)
            );

            return WhatsAppDeliveryResult.failure(
                    buildProviderFailure(ex)
            );

        } catch (ResourceAccessException ex) {
            log.warn(
                    "WhatsApp provider could not be reached: recipientPhone={}",
                    maskPhoneNumber(normalizedPhone)
            );

            return WhatsAppDeliveryResult.failure(
                    "WhatsApp provider is unavailable."
            );

        } catch (Exception ex) {
            log.error(
                    "Unexpected WhatsApp provider failure: recipientPhone={}",
                    maskPhoneNumber(normalizedPhone),
                    ex
            );

            return WhatsAppDeliveryResult.failure(
                    "Unexpected WhatsApp provider error: "
                            + safeExceptionMessage(ex)
            );
        }
    }

    private String validateConfiguration(
            String recipientPhone,
            String message
    ) {
        if (graphApiBaseUrl == null || graphApiBaseUrl.isBlank()) {
            return "WhatsApp Graph API base URL is not configured.";
        }

        if (apiVersion == null || apiVersion.isBlank()) {
            return "WhatsApp Graph API version is not configured.";
        }

        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            return "WhatsApp phone number ID is not configured.";
        }

        if (accessToken == null || accessToken.isBlank()) {
            return "WhatsApp access token is not configured.";
        }

        if (recipientPhone == null || recipientPhone.isBlank()) {
            return "Recipient phone number is missing.";
        }

        if (!recipientPhone.matches("^\\+?[1-9]\\d{7,14}$")) {
            return "Recipient phone number is invalid.";
        }

        if (message == null || message.isBlank()) {
            return "WhatsApp message is empty.";
        }

        return null;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceFirst("^\\+", "");
    }

    private String extractProviderMessageId(
            MetaSendMessageResponse response
    ) {
        if (response == null
                || response.messages() == null
                || response.messages().isEmpty()) {
            return null;
        }

        return response.messages().get(0).id();
    }

    private String buildProviderFailure(
            HttpStatusCodeException ex
    ) {
        String responseBody = ex.getResponseBodyAsString();

        if (responseBody == null || responseBody.isBlank()) {
            return "WhatsApp provider rejected the request with HTTP "
                    + ex.getStatusCode().value();
        }

        String sanitizedBody = responseBody
                .replaceAll("[\\r\\n\\t]+", " ")
                .trim();

        if (sanitizedBody.length() > 800) {
            sanitizedBody = sanitizedBody.substring(0, 800);
        }

        return "WhatsApp provider rejected the request with HTTP "
                + ex.getStatusCode().value()
                + ": "
                + sanitizedBody;
    }

    private String safeExceptionMessage(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }

        String message = ex.getMessage();

        return message.length() > 500
                ? message.substring(0, 500)
                : message;
    }

    private String removeTrailingSlash(String value) {
        return value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "****";
        }

        return phoneNumber.substring(0, 3)
                + "****"
                + phoneNumber.substring(phoneNumber.length() - 3);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MetaSendMessageResponse(
            @JsonProperty("messaging_product")
            String messagingProduct,

            List<MetaMessage> messages
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MetaMessage(
            String id
    ) {
    }
}