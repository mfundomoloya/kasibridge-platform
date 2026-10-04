package com.kasibridge.procurement.dto;

import java.time.LocalDateTime;

public record WhatsAppWebhookStatus(
        String providerMessageId,
        String status,
        LocalDateTime providerTimestamp,
        String failureCode,
        String failureReason
) {
}
