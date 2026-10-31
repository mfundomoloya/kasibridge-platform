package com.kasibridge.fraud.dto;

import java.time.LocalDateTime;

public record FraudAssessmentDashboardResponse(
        long totalAssessments,
        long pendingReview,
        long escalated,
        long confirmedFraud,
        long falsePositive,
        long autoCleared,
        long criticalRisk,
        long highRisk,
        long openHighOrCriticalRisk,
        LocalDateTime oldestPendingAssessedAt,
        Long oldestPendingAgeHours,
        LocalDateTime latestAssessedAt,
        LocalDateTime generatedAt
) {

    public FraudAssessmentDashboardResponse {
        requireNonNegative(
                totalAssessments,
                "Total assessments"
        );

        requireNonNegative(
                pendingReview,
                "Pending review count"
        );

        requireNonNegative(
                escalated,
                "Escalated count"
        );

        requireNonNegative(
                confirmedFraud,
                "Confirmed fraud count"
        );

        requireNonNegative(
                falsePositive,
                "False-positive count"
        );

        requireNonNegative(
                autoCleared,
                "Auto-cleared count"
        );

        requireNonNegative(
                criticalRisk,
                "Critical-risk count"
        );

        requireNonNegative(
                highRisk,
                "High-risk count"
        );

        requireNonNegative(
                openHighOrCriticalRisk,
                "Open high-or-critical-risk count"
        );

        if (
                oldestPendingAgeHours != null
                        && oldestPendingAgeHours < 0
        ) {
            throw new IllegalArgumentException(
                    "Oldest pending age cannot "
                            + "be negative."
            );
        }

        if (generatedAt == null) {
            throw new IllegalArgumentException(
                    "Dashboard generation time "
                            + "is required."
            );
        }
    }

    private static void requireNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be negative."
            );
        }
    }
}