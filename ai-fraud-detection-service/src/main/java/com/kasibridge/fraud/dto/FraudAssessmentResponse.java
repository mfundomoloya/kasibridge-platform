package com.kasibridge.fraud.dto;

import com.kasibridge.fraud.entity.FraudAssessment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FraudAssessmentResponse(
        Long id,
        String assessmentReference,
        String invoiceReference,
        String supplierId,
        String departmentId,
        BigDecimal invoiceAmount,
        String supplierCountry,
        String region,
        FraudAssessment.Classification classification,
        FraudAssessment.RiskLevel riskLevel,
        BigDecimal riskProbability,
        BigDecimal threshold,
        FraudAssessment.RecommendedAction recommendedAction,
        String modelName,
        String modelVersion,
        String experiment,
        FraudAssessment.DetectionSource detectionSource,
        String modelSha256,
        String featureSnapshotSha256,
        FraudAssessment.ReviewStatus reviewStatus,
        String reviewedBy,
        String reviewNotes,
        LocalDateTime reviewedAt,
        LocalDateTime assessedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version
) {

    public static FraudAssessmentResponse from(
            FraudAssessment assessment
    ) {
        if (assessment == null) {
            throw new IllegalArgumentException(
                    "Fraud assessment is required."
            );
        }

        return new FraudAssessmentResponse(
                assessment.getId(),
                assessment.getAssessmentReference(),
                assessment.getInvoiceReference(),
                assessment.getSupplierId(),
                assessment.getDepartmentId(),
                assessment.getInvoiceAmount(),
                assessment.getSupplierCountry(),
                assessment.getRegion(),
                assessment.getClassification(),
                assessment.getRiskLevel(),
                assessment.getRiskProbability(),
                assessment.getThreshold(),
                assessment.getRecommendedAction(),
                assessment.getModelName(),
                assessment.getModelVersion(),
                assessment.getExperiment(),
                assessment.getDetectionSource(),
                assessment.getModelSha256(),
                assessment.getFeatureSnapshotSha256(),
                assessment.getReviewStatus(),
                assessment.getReviewedBy(),
                assessment.getReviewNotes(),
                assessment.getReviewedAt(),
                assessment.getAssessedAt(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt(),
                assessment.getVersion()
        );
    }
}