package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.ProcurementAnomaly;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProcurementAnomalyRecordResponse {

    private Long id;
    private String anomalyReference;
    private Long tenderId;
    private Long bidId;
    private ProcurementAnomaly.AnomalyType type;
    private ProcurementAnomaly.Severity severity;
    private ProcurementAnomaly.AnomalyStatus status;
    private String message;
    private String evidence;
    private LocalDateTime detectedAt;
    private Long reviewedByUserId;
    private String reviewNotes;
    private LocalDateTime reviewedAt;

    public static ProcurementAnomalyRecordResponse from(ProcurementAnomaly anomaly) {
        return ProcurementAnomalyRecordResponse.builder()
                .id(anomaly.getId())
                .anomalyReference(anomaly.getAnomalyReference())
                .tenderId(anomaly.getTenderId())
                .bidId(anomaly.getBidId())
                .type(anomaly.getType())
                .severity(anomaly.getSeverity())
                .status(anomaly.getStatus())
                .message(anomaly.getMessage())
                .evidence(anomaly.getEvidence())
                .detectedAt(anomaly.getDetectedAt())
                .reviewedByUserId(anomaly.getReviewedByUserId())
                .reviewNotes(anomaly.getReviewNotes())
                .reviewedAt(anomaly.getReviewedAt())
                .build();
    }
}