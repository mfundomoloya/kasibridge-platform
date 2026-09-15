package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TicketAiTriageResult {

    private SupportTicketAiAssessment.DetectedCategory detectedCategory;

    private SupportTicketAiAssessment.TicketPriority priority;

    private SupportTicketAiAssessment.AiResolutionMode resolutionMode;

    private String summary;

    private String suggestedResponse;

    private String escalationReason;

    private BigDecimal confidenceScore;

    private String modelVersion;
}
