package com.kasibridge.fraud.dto;

import com.kasibridge.fraud.entity.FraudAssessment;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record FraudAssessmentSearchCriteria(
        FraudAssessment.ReviewStatus reviewStatus,
        FraudAssessment.RiskLevel riskLevel,
        FraudAssessment.Classification classification,
        String supplierId,
        String departmentId,
        String modelVersion,

        @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime assessedFrom,

        @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime assessedTo
) {

    public FraudAssessmentSearchCriteria {
        supplierId =
                normalizeOptionalText(
                        supplierId
                );

        departmentId =
                normalizeOptionalText(
                        departmentId
                );

        modelVersion =
                normalizeOptionalText(
                        modelVersion
                );
    }

    public void validate() {
        if (
                assessedFrom != null
                        && assessedTo != null
                        && assessedFrom.isAfter(
                        assessedTo
                )
        ) {
            throw new IllegalArgumentException(
                    "Assessment start time cannot "
                            + "be after assessment "
                            + "end time."
            );
        }
    }

    public boolean hasFilters() {
        return reviewStatus != null
                || riskLevel != null
                || classification != null
                || supplierId != null
                || departmentId != null
                || modelVersion != null
                || assessedFrom != null
                || assessedTo != null;
    }

    private static String normalizeOptionalText(
            String value
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}