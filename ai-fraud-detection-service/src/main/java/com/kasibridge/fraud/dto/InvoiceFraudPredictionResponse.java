package com.kasibridge.fraud.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceFraudPredictionResponse(
        Classification classification,
        RiskLevel riskLevel,
        BigDecimal riskProbability,
        String modelName,
        String modelVersion,
        String experiment,
        DetectionSource detectionSource,
        RecommendedAction recommendedAction,
        BigDecimal threshold,
        String modelSha256,
        LocalDateTime assessedAt
) {

    public enum Classification {
        LEGITIMATE,
        HIGH_RISK
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum DetectionSource {
        SUPERVISED_MODEL
    }

    public enum RecommendedAction {
        ALLOW_WITH_MONITORING,
        REQUIRE_REVIEW,
        HOLD_PAYMENT_AND_REVIEW,
        BLOCK_AND_ESCALATE
    }

    public static InvoiceFraudPredictionResponse from(
            MlInferenceResponse response
    ) {
        if (response == null) {
            throw new IllegalArgumentException(
                    "ML inference response is required."
            );
        }

        return new InvoiceFraudPredictionResponse(
                Classification.valueOf(
                        response.classification()
                ),
                RiskLevel.valueOf(
                        response.riskLevel()
                ),
                response.riskProbability(),
                response.modelName(),
                response.modelVersion(),
                response.experiment(),
                DetectionSource.valueOf(
                        response.detectionSource()
                ),
                RecommendedAction.valueOf(
                        response.recommendedAction()
                ),
                response.threshold(),
                response.modelSha256(),
                LocalDateTime.now()
        );
    }
}