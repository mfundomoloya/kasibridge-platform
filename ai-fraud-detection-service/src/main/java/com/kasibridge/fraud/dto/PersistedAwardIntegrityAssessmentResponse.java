package com.kasibridge.fraud.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.fraud.entity
        .AwardIntegrityAssessment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PersistedAwardIntegrityAssessmentResponse(

        Long id,
        String assessmentReference,
        Long tenderId,
        String assessmentType,
        BigDecimal riskScore,
        String riskLevel,
        String recommendation,
        String ruleVersion,
        boolean modelUsed,
        boolean awardConsistentWithRules,

        List<AwardIntegritySignalResponse> signals,
        String reviewStatus,
        String reviewedBy,
        String reviewNotes,
        LocalDateTime reviewedAt,
        LocalDateTime assessedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version
) {

    private static final TypeReference<
            List<AwardIntegritySignalResponse>
            > SIGNAL_LIST_TYPE =
            new TypeReference<>() {
            };

    public PersistedAwardIntegrityAssessmentResponse {
        signals =
                signals == null
                        ? List.of()
                        : List.copyOf(
                        signals
                );
    }

    public static PersistedAwardIntegrityAssessmentResponse from(AwardIntegrityAssessment assessment,
                                                                 ObjectMapper objectMapper
    ) {
        if (assessment == null) {
            throw new IllegalArgumentException(
                    "Award integrity assessment "
                            + "is required."
            );
        }

        if (objectMapper == null) {
            throw new IllegalArgumentException(
                    "Object mapper is required."
            );
        }

        return new
                PersistedAwardIntegrityAssessmentResponse(
                assessment.getId(),
                assessment.getAssessmentReference(),
                assessment.getTenderId(),
                assessment.getAssessmentType().name(),
                assessment.getRiskScore(),
                assessment.getRiskLevel().name(),
                assessment.getRecommendation().name(),
                assessment.getRuleVersion(),
                assessment.isModelUsed(),
                assessment.isAwardConsistentWithRules(),
                deserializeSignals(
                        assessment.getSignalsJson(),
                        objectMapper
                ),
                assessment.getReviewStatus().name(),
                assessment.getReviewedBy(),
                assessment.getReviewNotes(),
                assessment.getReviewedAt(),
                assessment.getAssessedAt(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt(),
                assessment.getVersion()
        );
    }

    private static List<AwardIntegritySignalResponse> deserializeSignals(String signalsJson, ObjectMapper objectMapper
    ) {
        if (
                signalsJson == null
                        || signalsJson.isBlank()
        ) {
            throw new IllegalStateException(
                    "Persisted award integrity "
                            + "assessment contains no "
                            + "signal snapshot."
            );
        }

        try {
            List<AwardIntegritySignalResponse>
                    signals =
                    objectMapper.readValue(
                            signalsJson,
                            SIGNAL_LIST_TYPE
                    );

            if (
                    signals == null
                            || signals.isEmpty()
            ) {
                throw new IllegalStateException(
                        "Persisted award integrity "
                                + "assessment contains "
                                + "an empty signal "
                                + "snapshot."
                );
            }

            return List.copyOf(
                    signals
            );

        } catch (
                JsonProcessingException exception
        ) {
            throw new IllegalStateException(
                    "Could not deserialize the "
                            + "persisted award "
                            + "integrity signal "
                            + "snapshot.",
                    exception
            );
        }
    }
}