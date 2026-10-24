package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.NotificationOutbox;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WhatsAppDeliveryCommand {

    private Long notificationId;

    private NotificationOutbox.NotificationTemplateType
            templateType;

    private String recipientPhone;

    private String renderedMessage;

    private String recipientName;

    private String tenderReference;

    private String tenderTitle;

    private String bidReference;

    private String ticketReference;
}