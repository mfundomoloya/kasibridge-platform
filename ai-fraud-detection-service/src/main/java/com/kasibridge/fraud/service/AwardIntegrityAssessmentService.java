package com.kasibridge.fraud.service;

import com.kasibridge.fraud.dto.AwardIntegrityAssessmentRequest;
import com.kasibridge.fraud.dto.AwardIntegrityAssessmentResponse;
import com.kasibridge.fraud.dto.AwardIntegritySignalResponse;
import com.kasibridge.fraud.exception.MlInferenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwardIntegrityAssessmentService {

    private static final Set<String>
            SUPPORTED_RISK_LEVELS =
            Set.of(
                    "LOW",
                    "MEDIUM",
                    "HIGH",
                    "CRITICAL"
            );

    private static final Set<String>
            SUPPORTED_RECOMMENDATIONS =
            Set.of(
                    "ACCEPT_WITH_MONITORING",
                    "REVIEW_COMPETITION",
                    "REQUIRE_GOVERNANCE_REVIEW",
                    "ESCALATE_FOR_INVESTIGATION"
            );

    private static final Set<String>
            SUPPORTED_SIGNAL_SEVERITIES =
            Set.of(
                    "INFO",
                    "LOW",
                    "MEDIUM",
                    "HIGH",
                    "CRITICAL"
            );

    private final MlInferenceClient
            mlInferenceClient;

    public AwardIntegrityAssessmentResponse
    assess(
            AwardIntegrityAssessmentRequest request
    ) {
        validateRequest(
                request
        );

        log.info(
                "Starting award integrity "
                        + "assessment: tenderId={} "
                        + "eligibleBidCount={} "
                        + "expectedScoreCount={} "
                        + "submittedScoreCount={} "
                        + "awardRank={}",
                request.tenderId(),
                request.eligibleBidCount(),
                request.expectedScoreCount(),
                request.submittedScoreCount(),
                request.awardRank()
        );

        AwardIntegrityAssessmentResponse response =
                mlInferenceClient
                        .assessAwardIntegrity(
                                request
                        );

        validateBusinessResponse(
                request,
                response
        );

        logAssessmentResult(
                response
        );

        return response;
    }

    private void validateRequest(
            AwardIntegrityAssessmentRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Award integrity assessment "
                            + "request is required."
            );
        }

        requirePositiveIdentifier(
                request.tenderId(),
                "Tender ID"
        );

        requirePositiveAmount(
                request.budgetAmount(),
                "Budget amount"
        );

        requireNonNegativeAmount(
                request.awardedAmount(),
                "Awarded amount"
        );

        requireNonNegativeCount(
                request.submittedBidCount(),
                "Submitted bid count"
        );

        requireNonNegativeCount(
                request.eligibleBidCount(),
                "Eligible bid count"
        );

        requireNonNegativeCount(
                request.withdrawnBidCount(),
                "Withdrawn bid count"
        );

        requireNonNegativeCount(
                request.activeEvaluatorCount(),
                "Active evaluator count"
        );

        requireNonNegativeCount(
                request.expectedScoreCount(),
                "Expected score count"
        );

        requireNonNegativeCount(
                request.submittedScoreCount(),
                "Submitted score count"
        );

        requireScore(
                request.averageTechnicalScore(),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                "Average technical score"
        );

        requireScore(
                request.averagePriceScore(),
                BigDecimal.ZERO,
                new BigDecimal("20.00"),
                "Average price score"
        );

        requireScore(
                request.averageTotalScore(),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                "Average total score"
        );

        requireScore(
                request.minimumTechnicalThreshold(),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                "Minimum technical threshold"
        );

        if (request.awardRank() <= 0) {
            throw new IllegalArgumentException(
                    "Award rank must be positive."
            );
        }

        requirePositiveIdentifier(
                request.specificationOfficerUserId(),
                "Specification Officer user ID"
        );

        requirePositiveIdentifier(
                request.adjudicatorUserId(),
                "Adjudicator user ID"
        );

        validateEvaluatorUserIds(
                request.evaluatorUserIds()
        );

        requireDateTime(
                request.publishedAt(),
                "Published timestamp"
        );

        requireDateTime(
                request.biddingClosedAt(),
                "Bidding-closed timestamp"
        );

        requireDateTime(
                request.awardedAt(),
                "Award timestamp"
        );

        validateCountConsistency(
                request
        );
    }

    private void validateCountConsistency(
            AwardIntegrityAssessmentRequest request
    ) {
        if (
                request.eligibleBidCount()
                        > request.submittedBidCount()
        ) {
            throw new IllegalArgumentException(
                    "Eligible bid count cannot "
                            + "exceed submitted bid "
                            + "count."
            );
        }

        if (
                request.withdrawnBidCount()
                        > request.submittedBidCount()
        ) {
            throw new IllegalArgumentException(
                    "Withdrawn bid count cannot "
                            + "exceed submitted bid "
                            + "count."
            );
        }

        if (
                request.submittedScoreCount()
                        > request.expectedScoreCount()
                        && request.expectedScoreCount() > 0
        ) {
            log.warn(
                    "Award integrity request has "
                            + "more submitted scores "
                            + "than expected: "
                            + "tenderId={} submitted={} "
                            + "expected={}",
                    request.tenderId(),
                    request.submittedScoreCount(),
                    request.expectedScoreCount()
            );
        }

        long evaluatorCount =
                request
                        .evaluatorUserIds()
                        .stream()
                        .distinct()
                        .count();

        if (
                request.activeEvaluatorCount()
                        != evaluatorCount
        ) {
            throw new IllegalArgumentException(
                    "Active evaluator count must "
                            + "match the number of "
                            + "distinct evaluator "
                            + "user IDs."
            );
        }

        long expectedScoreCount;

        try {
            expectedScoreCount =
                    Math.multiplyExact(
                            request
                                    .activeEvaluatorCount(),
                            request
                                    .eligibleBidCount()
                    );

        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Expected score count exceeds "
                            + "the supported range.",
                    exception
            );
        }

        if (
                request.expectedScoreCount()
                        != expectedScoreCount
        ) {
            throw new IllegalArgumentException(
                    "Expected score count must "
                            + "equal active evaluators "
                            + "multiplied by eligible "
                            + "bids."
            );
        }
    }

    private void validateEvaluatorUserIds(
            List<Long> evaluatorUserIds
    ) {
        if (
                evaluatorUserIds == null
                        || evaluatorUserIds.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "At least one Evaluator user ID "
                            + "is required."
            );
        }

        Set<Long> uniqueUserIds =
                new HashSet<>();

        for (Long evaluatorUserId
                : evaluatorUserIds) {
            requirePositiveIdentifier(
                    evaluatorUserId,
                    "Evaluator user ID"
            );

            if (
                    !uniqueUserIds.add(
                            evaluatorUserId
                    )
            ) {
                throw new IllegalArgumentException(
                        "Evaluator user IDs must "
                                + "be unique."
                );
            }
        }
    }

    private void validateBusinessResponse(
            AwardIntegrityAssessmentRequest request,
            AwardIntegrityAssessmentResponse response
    ) {
        if (response == null) {
            throw new MlInferenceException(
                    "Award integrity service "
                            + "returned no response."
            );
        }

        if (
                !request.tenderId()
                        .equals(
                                response.tenderId()
                        )
        ) {
            throw new MlInferenceException(
                    "Award integrity response "
                            + "tender ID does not "
                            + "match the request."
            );
        }

        if (
                response.assessmentType() == null
                        || !response
                        .assessmentType()
                        .equals(
                                "DETERMINISTIC_"
                                        + "AWARD_INTEGRITY"
                        )
        ) {
            throw new MlInferenceException(
                    "Award integrity response "
                            + "contains an unsupported "
                            + "assessment type."
            );
        }

        if (
                response.riskLevel() == null
                        || !SUPPORTED_RISK_LEVELS
                        .contains(
                                response.riskLevel()
                        )
        ) {
            throw new MlInferenceException(
                    "Award integrity response "
                            + "contains an unsupported "
                            + "risk level."
            );
        }

        if (
                response.recommendation() == null
                        || !SUPPORTED_RECOMMENDATIONS
                        .contains(
                                response
                                        .recommendation()
                        )
        ) {
            throw new MlInferenceException(
                    "Award integrity response "
                            + "contains an unsupported "
                            + "recommendation."
            );
        }

        if (
                !Boolean.FALSE.equals(
                        response.modelUsed()
                )
        ) {
            throw new MlInferenceException(
                    "The award integrity assessment "
                            + "must not claim that the "
                            + "invoice fraud model "
                            + "was used."
            );
        }

        validateSignals(
                response.signals()
        );
    }

    private void validateSignals(
            List<AwardIntegritySignalResponse> signals
    ) {
        if (
                signals == null
                        || signals.isEmpty()
        ) {
            throw new MlInferenceException(
                    "Award integrity assessment "
                            + "contains no signals."
            );
        }

        Set<String> signalCodes =
                new HashSet<>();

        int calculatedRiskPoints = 0;

        for (
                AwardIntegritySignalResponse signal
                : signals
        ) {
            if (signal == null) {
                throw new MlInferenceException(
                        "Award integrity assessment "
                                + "contains an empty "
                                + "signal."
                );
            }

            String code =
                    requireResponseText(
                            signal.code(),
                            "Signal code"
                    );

            if (!signalCodes.add(code)) {
                throw new MlInferenceException(
                        "Award integrity assessment "
                                + "contains duplicate "
                                + "signal code: "
                                + code
                );
            }

            if (
                    signal.severity() == null
                            || !SUPPORTED_SIGNAL_SEVERITIES
                            .contains(
                                    signal.severity()
                            )
            ) {
                throw new MlInferenceException(
                        "Award integrity signal "
                                + code
                                + " contains an "
                                + "unsupported severity."
                );
            }

            requireResponseText(
                    signal.message(),
                    "Signal message"
            );

            if (
                    signal.riskPoints() == null
                            || signal.riskPoints() < 0
            ) {
                throw new MlInferenceException(
                        "Award integrity signal "
                                + code
                                + " contains invalid "
                                + "risk points."
                );
            }

            calculatedRiskPoints =
                    Math.addExact(
                            calculatedRiskPoints,
                            signal.riskPoints()
                    );
        }

        if (calculatedRiskPoints > 100) {
            log.debug(
                    "Award integrity raw risk "
                            + "points exceed the "
                            + "normalised maximum: {}",
                    calculatedRiskPoints
            );
        }
    }

    private void logAssessmentResult(
            AwardIntegrityAssessmentResponse response
    ) {
        if (
                "HIGH".equals(
                        response.riskLevel()
                )
                        || "CRITICAL".equals(
                        response.riskLevel()
                )
                        || !Boolean.TRUE.equals(
                        response
                                .awardConsistentWithRules()
                )
        ) {
            log.warn(
                    "Award integrity review "
                            + "required: tenderId={} "
                            + "riskLevel={} "
                            + "riskScore={} "
                            + "recommendation={} "
                            + "consistentWithRules={} "
                            + "signalCount={}",
                    response.tenderId(),
                    response.riskLevel(),
                    response.riskScore(),
                    response.recommendation(),
                    response
                            .awardConsistentWithRules(),
                    response.signals().size()
            );

            return;
        }

        log.info(
                "Award integrity assessment "
                        + "completed: tenderId={} "
                        + "riskLevel={} "
                        + "riskScore={} "
                        + "recommendation={} "
                        + "consistentWithRules={} "
                        + "signalCount={}",
                response.tenderId(),
                response.riskLevel(),
                response.riskScore(),
                response.recommendation(),
                response
                        .awardConsistentWithRules(),
                response.signals().size()
        );
    }

    private static void requirePositiveIdentifier(
            Long value,
            String fieldName
    ) {
        if (
                value == null
                        || value <= 0
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be a positive "
                            + "integer."
            );
        }
    }

    private static void requirePositiveAmount(
            BigDecimal value,
            String fieldName
    ) {
        if (
                value == null
                        || value.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be greater than "
                            + "zero."
            );
        }
    }

    private static void requireNonNegativeAmount(
            BigDecimal value,
            String fieldName
    ) {
        if (
                value == null
                        || value.compareTo(
                        BigDecimal.ZERO
                ) < 0
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be negative."
            );
        }
    }

    private static void requireNonNegativeCount(
            long value,
            String fieldName
    ) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be negative."
            );
        }
    }

    private static void requireScore(
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum,
            String fieldName
    ) {
        if (
                value == null
                        || value.compareTo(
                        minimum
                ) < 0
                        || value.compareTo(
                        maximum
                ) > 0
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be between "
                            + minimum
                            + " and "
                            + maximum
                            + "."
            );
        }
    }

    private static void requireDateTime(
            LocalDateTime value,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName
                            + " is required."
            );
        }
    }

    private static String requireResponseText(
            String value,
            String fieldName
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            throw new MlInferenceException(
                    fieldName
                            + " is missing from the "
                            + "award integrity "
                            + "response."
            );
        }

        return value.trim();
    }
}