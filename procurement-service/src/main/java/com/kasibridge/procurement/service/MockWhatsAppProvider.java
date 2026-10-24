package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.WhatsAppDeliveryCommand;
import com.kasibridge.procurement.dto.WhatsAppDeliveryResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "kasibridge.notifications.mock-whatsapp.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@Slf4j
public class MockWhatsAppProvider implements WhatsAppProvider{

    @PostConstruct
    void logProviderActivation() {
        log.warn("Mock WhatsApp provider is active.");
    }
    @Value("${kasibridge.notifications.mock-whatsapp.enabled:true}")
    private boolean enabled;

    @Value("${kasibridge.notifications.mock-whatsapp.force-failure:false}")
    private boolean forceFailure;

    @Override
    public WhatsAppDeliveryResult sendMessage(WhatsAppDeliveryCommand command) {

        if(!enabled) {
            return WhatsAppDeliveryResult.failure(
                    "Mock WhatsApp provider is disabled.");
        }

        if(forceFailure) {
            return WhatsAppDeliveryResult.failure(
                    "Mock WhatsApp provider failure was force by configuration");
        }

        if(command.getRecipientPhone() == null || command.getRecipientPhone().isBlank()) {
            return WhatsAppDeliveryResult.failure(
                    "Recipient phone number is missing."
            );
        }

        if(!isValidPhoneNumber(command.getRecipientPhone())){
            return WhatsAppDeliveryResult.failure(
                    "Recipient phone number is invalid."
            );
        }

        if(command.getRenderedMessage()==null || command.getRenderedMessage().isBlank()) {
            return WhatsAppDeliveryResult.failure(
                    "WhatsApp message is empty."
            );
        }

        String providerMessageId = generateProviderMessageId();

        log.info("MOCK WHATSAPP SENT: providerMessageId={} recipientPhone={} message={}",
                providerMessageId,
                maskPhoneNumber(command.getRecipientPhone()),
                command.getRenderedMessage());

        return WhatsAppDeliveryResult.success(providerMessageId);
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("^\\+[1-9]\\d{7,14}$");
    }

    private String generateProviderMessageId() {
        return "MOCK-WA-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "****";
        }

        return phoneNumber.substring(0, 4)
                + "****"
                + phoneNumber.substring(phoneNumber.length() - 3);
    }
}
