package com.kasibridge.procurement.dto;

public record WhatsAppDeliveryResult(
        boolean successful,
        String providerMessageId,
        String failureReason
) {
    public static WhatsAppDeliveryResult success(String providerMessageId){
        return new WhatsAppDeliveryResult(
                true,
                providerMessageId,
                null
        );
    }

    public static WhatsAppDeliveryResult failure(String failureReason){
        return new WhatsAppDeliveryResult(
                false,
                null,
                failureReason
        );
    }
}
