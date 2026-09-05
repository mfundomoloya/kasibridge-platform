package com.kasibridge.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WhatsAppTemplateContext {
    private String recipientName;

    private Long tenderId;
    private String tenderReference;
    private String tenderTitle;

    private Long bidId;
    private String bidReference;
    private String bidderAlias;
    private BigDecimal bidAmount;

    private Long ticketId;
    private String ticketReference;
    private String ticketType;
    private String ticketSubject;
    private String ticketResponse;
    private String closureNotes;

    private String complianceFailureReason;

    private String anomalyReference;
    private String anomalyType;
    private String anomalySeverity;
}
