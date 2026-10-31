package com.kasibridge.fraud.repository;

import java.time.LocalDateTime;

public interface FraudAssessmentDashboardProjection {

    Long getTotalAssessments();

    Long getPendingReview();

    Long getEscalated();

    Long getConfirmedFraud();

    Long getFalsePositive();

    Long getAutoCleared();

    Long getCriticalRisk();

    Long getHighRisk();

    Long getOpenHighOrCriticalRisk();

    LocalDateTime getOldestPendingAssessedAt();

    LocalDateTime getLatestAssessedAt();
}