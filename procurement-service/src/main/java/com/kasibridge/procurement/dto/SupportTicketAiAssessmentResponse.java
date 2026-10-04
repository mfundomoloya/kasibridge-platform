package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SupportTicketAiAssessmentResponse {

    private Long id;
    private String assessmentReference;
    private Long ticketId;

    private SupportTicketAiAssessment.DetectedCategory detectedCategory;
    private SupportTicketAiAssessment.TicketPriority priority;
    private SupportTicketAiAssessment.AiResolutionMode resolutionMode;
    private SupportTicketAiAssessment.AiAssessmentStatus assessmentStatus;

    private String summary;
    private String suggestedResponse;
    private String escalationReason;

    private BigDecimal confidenceScore;
    private String modelVersion;

    private String approvedResponse;
    private Long approvedByUserId;
    private LocalDateTime approvedAt;

    private boolean responseRelevanceConfirmed;
    private Long responseRelevanceConfirmedByUserId;
    private LocalDateTime responseRelevanceConfirmedAt;
    private String responseRelevanceConfirmationNotes;

    private Long publishedByUserId;
    private LocalDateTime publishedAt;

    private Long rejectedByUserId;
    private LocalDateTime rejectedAt;
    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public static SupportTicketAiAssessmentResponse from(
            SupportTicketAiAssessment assessment) {

        return SupportTicketAiAssessmentResponse.builder()

                .id(assessment.getId())
                .assessmentReference(assessment.getAssessmentReference())
                .ticketId(assessment.getTicketId())
                .detectedCategory(assessment.getDetectedCategory())
                .priority(assessment.getPriority())
                .resolutionMode(assessment.getResolutionMode())
                .assessmentStatus(assessment.getAssessmentStatus())
                .summary(assessment.getSummary())
                .suggestedResponse(assessment.getSuggestedResponse())
                .escalationReason(assessment.getEscalationReason())
                .confidenceScore(assessment.getConfidenceScore())
                .modelVersion(assessment.getModelVersion())
                .approvedResponse(assessment.getApprovedResponse())
                .approvedByUserId(assessment.getApprovedByUserId())
                .approvedAt(assessment.getApprovedAt())
                .publishedByUserId(assessment.getPublishedByUserId())
                .publishedAt(assessment.getPublishedAt())
                .rejectedByUserId(assessment.getRejectedByUserId())
                .rejectedAt(assessment.getRejectedAt())
                .rejectionReason(assessment.getRejectionReason())
                .createdAt(assessment.getCreatedAt())
                .updatedAt(assessment.getUpdatedAt())
                .version(assessment.getVersion())
                .build();
    }
}
