package com.kasibridge.procurement.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kasibridge.procurement.dto.WhatsAppDeliveryCommand;
import com.kasibridge.procurement.dto.WhatsAppDeliveryResult;
import com.kasibridge.procurement.entity.NotificationOutbox;
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

import java.util.ArrayList;
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

    private final RestTemplate restTemplate;

    @Value("${kasibridge.notifications.whatsapp.graph-api-base-url:https://graph.facebook.com}")
    private String graphApiBaseUrl;

    @Value("${kasibridge.notifications.whatsapp.api-version}")
    private String apiVersion;

    @Value("${kasibridge.notifications.whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${kasibridge.notifications.whatsapp.access-token}")
    private String accessToken;

    @Value(
            "${kasibridge.notifications.whatsapp.templates."
                    + "matching-tender-published.name:"
                    + "matching_tender_published}"
    )
    private String matchingTenderPublishedTemplateName;

    @Value(
            "${kasibridge.notifications.whatsapp.templates."
                    + "matching-tender-published.language:en_US}"
    )
    private String matchingTenderPublishedTemplateLanguage;


    @PostConstruct
    void logProviderActivation() {
        log.info("Real Meta WhatsApp provider is active.");
    }

    @Override
    public WhatsAppDeliveryResult sendMessage(
            WhatsAppDeliveryCommand command
    ) {
        if (command == null) {
            return WhatsAppDeliveryResult.failure(
                    "WhatsApp delivery command is required."
            );
        }

        String cleanedPhone =
                command.getRecipientPhone() == null
                        ? null
                        : command.getRecipientPhone()
                        .trim();

        String cleanedMessage =
                command.getRenderedMessage() == null
                        ? null
                        : command.getRenderedMessage()
                        .trim();

        String validationMessage =
                command.getTemplateType()
                        == NotificationOutbox
                        .NotificationTemplateType
                        .MATCHING_TENDER_PUBLISHED
                        ? "template-message"
                        : cleanedMessage;

        String validationFailure =
                validateConfiguration(
                        cleanedPhone,
                        validationMessage
                );

        if (validationFailure != null) {
            return WhatsAppDeliveryResult.failure(
                    validationFailure
            );
        }

        String normalizedPhone =
                normalizePhoneNumber(
                        cleanedPhone
                );

        String endpoint =
                String.format(
                        "%s/%s/%s/messages",
                        removeTrailingSlash(
                                graphApiBaseUrl
                        ),
                        apiVersion.trim(),
                        phoneNumberId.trim()
                );

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(
                accessToken.trim()
        );

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        Map<String, Object> requestBody;

        try {
            requestBody =
                    buildRequestBody(
                            command,
                            normalizedPhone,
                            cleanedMessage
                    );
        } catch (
                IllegalArgumentException exception
        ) {
            return WhatsAppDeliveryResult.failure(
                    exception.getMessage()
            );
        }

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        try {
            ResponseEntity<MetaSendMessageResponse>
                    response =
                    restTemplate.postForEntity(
                            endpoint,
                            request,
                            MetaSendMessageResponse.class
                    );

            MetaSendMessageResponse responseBody =
                    response.getBody();

            if (
                    !response.getStatusCode()
                            .is2xxSuccessful()
            ) {
                return WhatsAppDeliveryResult.failure(
                        "WhatsApp provider returned HTTP "
                                + response
                                .getStatusCode()
                                .value()
                );
            }

            String providerMessageId =
                    extractProviderMessageId(
                            responseBody
                    );

            if (
                    providerMessageId == null
                            || providerMessageId.isBlank()
            ) {
                return WhatsAppDeliveryResult.failure(
                        "WhatsApp provider accepted the "
                                + "request but returned no "
                                + "message ID."
                );
            }

            log.info(
                    "WhatsApp message accepted: "
                            + "providerMessageId={} "
                            + "recipientPhone={}",
                    providerMessageId,
                    maskPhoneNumber(
                            normalizedPhone
                    )
            );

            return WhatsAppDeliveryResult.success(
                    providerMessageId.trim()
            );

        } catch (
                HttpStatusCodeException exception
        ) {
            log.warn(
                    "WhatsApp provider rejected message: "
                            + "status={} recipientPhone={}",
                    exception
                            .getStatusCode()
                            .value(),
                    maskPhoneNumber(
                            normalizedPhone
                    )
            );

            return WhatsAppDeliveryResult.failure(
                    buildProviderFailure(
                            exception
                    )
            );

        } catch (
                ResourceAccessException exception
        ) {
            log.warn(
                    "WhatsApp provider could not be "
                            + "reached: recipientPhone={}",
                    maskPhoneNumber(
                            normalizedPhone
                    )
            );

            return WhatsAppDeliveryResult.failure(
                    "WhatsApp provider is unavailable."
            );

        } catch (Exception exception) {
            log.error(
                    "Unexpected WhatsApp provider failure: "
                            + "recipientPhone={}",
                    maskPhoneNumber(
                            normalizedPhone
                    ),
                    exception
            );

            return WhatsAppDeliveryResult.failure(
                    "Unexpected WhatsApp provider error: "
                            + safeExceptionMessage(
                            exception
                    )
            );
        }
    }
    private String validateConfiguration(String recipientPhone, String message) {
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

    private String extractProviderMessageId(MetaSendMessageResponse response) {
        if (response == null
                || response.messages() == null
                || response.messages().isEmpty()) {
            return null;
        }

        return response.messages().get(0).id();
    }

    private String buildProviderFailure(HttpStatusCodeException ex) {
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
        String normalizedValue = value.trim();

        while (normalizedValue.endsWith("/")) {
            normalizedValue =
                    normalizedValue.substring(
                            0,
                            normalizedValue.length() - 1
                    );
        }

        return normalizedValue;
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

    private Map<String, Object> buildRequestBody(
            WhatsAppDeliveryCommand command,
            String normalizedPhone,
            String cleanedMessage
    ) {
        if (
                command.getTemplateType()
                        == NotificationOutbox
                        .NotificationTemplateType
                        .MATCHING_TENDER_PUBLISHED
        ) {
            return buildMatchingTenderTemplateRequest(
                    command,
                    normalizedPhone
            );
        }

        return Map.of(
                "messaging_product",
                "whatsapp",

                "recipient_type",
                "individual",

                "to",
                normalizedPhone,

                "type",
                "text",

                "text",
                Map.of(
                        "preview_url",
                        false,

                        "body",
                        cleanedMessage
                )
        );
    }

    private Map<String, Object>
    buildMatchingTenderTemplateRequest(
            WhatsAppDeliveryCommand command,
            String normalizedPhone
    ) {
        String validationFailure =
                validateMatchingTenderTemplate(
                        command
                );

        if (validationFailure != null) {
            throw new IllegalArgumentException(
                    validationFailure
            );
        }

        List<Map<String, Object>> parameters =
                new ArrayList<>();

        parameters.add(
                textParameter(
                        command.getRecipientName()
                )
        );

        parameters.add(
                textParameter(
                        command.getTenderReference()
                )
        );

        parameters.add(
                textParameter(
                        command.getTenderTitle()
                )
        );

        Map<String, Object> bodyComponent =
                Map.of(
                        "type",
                        "body",

                        "parameters",
                        parameters
                );

        Map<String, Object> language =
                Map.of(
                        "code",
                        matchingTenderPublishedTemplateLanguage
                                .trim()
                );

        Map<String, Object> template =
                Map.of(
                        "name",
                        matchingTenderPublishedTemplateName
                                .trim(),

                        "language",
                        language,

                        "components",
                        List.of(
                                bodyComponent
                        )
                );

        return Map.of(
                "messaging_product",
                "whatsapp",

                "recipient_type",
                "individual",

                "to",
                normalizedPhone,

                "type",
                "template",

                "template",
                template
        );
    }

    private Map<String, Object> textParameter(
            String value
    ) {
        return Map.of(
                "type",
                "text",

                "text",
                value.trim()
        );
    }

    private String validateMatchingTenderTemplate(
            WhatsAppDeliveryCommand command
    ) {
        if (
                matchingTenderPublishedTemplateName
                        == null
                        || matchingTenderPublishedTemplateName
                        .isBlank()
        ) {
            return "Matching-tender WhatsApp template "
                    + "name is not configured.";
        }

        if (
                matchingTenderPublishedTemplateLanguage
                        == null
                        || matchingTenderPublishedTemplateLanguage
                        .isBlank()
        ) {
            return "Matching-tender WhatsApp template "
                    + "language is not configured.";
        }

        if (
                command.getRecipientName() == null
                        || command.getRecipientName()
                        .isBlank()
        ) {
            return "Recipient name is required for "
                    + "MATCHING_TENDER_PUBLISHED.";
        }

        if (
                command.getTenderReference() == null
                        || command.getTenderReference()
                        .isBlank()
        ) {
            return "Tender reference is required for "
                    + "MATCHING_TENDER_PUBLISHED.";
        }

        if (
                command.getTenderTitle() == null
                        || command.getTenderTitle()
                        .isBlank()
        ) {
            return "Tender title is required for "
                    + "MATCHING_TENDER_PUBLISHED.";
        }

        return null;
    }
}