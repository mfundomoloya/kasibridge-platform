package com.kasibridge.fraud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.fraud.dto.AwardIntegrityAssessmentRequest;
import com.kasibridge.fraud.dto.AwardIntegrityAssessmentResponse;
import com.kasibridge.fraud.dto.AwardIntegrityAssessmentResult;
import com.kasibridge.fraud.dto.AwardIntegrityBeginReviewRequest;
import com.kasibridge.fraud.dto.AwardIntegrityReviewRequest;
import com.kasibridge.fraud.dto.AwardIntegritySignalResponse;
import com.kasibridge.fraud.dto.PersistedAwardIntegrityAssessmentResponse;
import com.kasibridge.fraud.entity.AwardIntegrityAssessment;
import com.kasibridge.fraud.exception.AwardIntegrityAssessmentConflictException;
import com.kasibridge.fraud.exception.AwardIntegrityAssessmentNotFoundException;
import com.kasibridge.fraud.exception.AwardIntegrityAssessmentVersionConflictException;
import com.kasibridge.fraud.exception.MlInferenceException;
import com.kasibridge.fraud.repository.AwardIntegrityAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HexFormat;
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

    private final AwardIntegrityAssessmentRepository
            assessmentRepository;

    private final ObjectMapper
            objectMapper;

    @Transactional
    public AwardIntegrityAssessmentResult assess(
            AwardIntegrityAssessmentRequest request
    ) {
        validateRequest(
                request
        );

        String requestSnapshotJson =
                serializeSnapshot(
                        request,
                        "award integrity request"
                );

        String requestSnapshotSha256 =
                calculateSha256(
                        requestSnapshotJson
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

        return assessmentRepository
                .findByTenderIdAndRuleVersionAndRequestSnapshotSha256(
                        request.tenderId(),
                        response.ruleVersion(),
                        requestSnapshotSha256
                )
                .map(
                        existingAssessment -> {
                            log.info(
                                    "Returning existing "
                                            + "award integrity "
                                            + "assessment: "
                                            + "assessmentId={} "
                                            + "reference={} "
                                            + "tenderId={} "
                                            + "ruleVersion={}",
                                    existingAssessment.getId(),
                                    existingAssessment
                                            .getAssessmentReference(),
                                    existingAssessment.getTenderId(),
                                    existingAssessment.getRuleVersion()
                            );

                            return AwardIntegrityAssessmentResult
                                    .existing(
                                            toResponse(
                                                    existingAssessment
                                            )
                                    );
                        }
                )
                .orElseGet(
                        () -> persistAssessment(
                                request,
                                response,
                                requestSnapshotJson,
                                requestSnapshotSha256
                        )
                );
    }

    @Transactional(readOnly = true)
    public PersistedAwardIntegrityAssessmentResponse
    getAssessment(
            Long assessmentId
    ) {
        return toResponse(
                requireAssessment(
                        assessmentId
                )
        );
    }

    @Transactional(readOnly = true)
    public PersistedAwardIntegrityAssessmentResponse
    getAssessmentByReference(
            String assessmentReference
    ) {
        if (
                assessmentReference == null
                        || assessmentReference.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Assessment reference is required."
            );
        }

        AwardIntegrityAssessment assessment =
                assessmentRepository
                        .findByAssessmentReference(
                                assessmentReference.trim()
                        )
                        .orElseThrow(
                                () -> new
                                        AwardIntegrityAssessmentNotFoundException(
                                        "Award integrity "
                                                + "assessment was not "
                                                + "found for reference "
                                                + assessmentReference
                                                .trim()
                                                + "."
                                )
                        );

        return toResponse(
                assessment
        );
    }

    @Transactional(readOnly = true)
    public PersistedAwardIntegrityAssessmentResponse
    getLatestAssessmentForTender(
            Long tenderId
    ) {
        requirePositiveIdentifier(
                tenderId,
                "Tender ID"
        );

        AwardIntegrityAssessment assessment =
                assessmentRepository
                        .findFirstByTenderIdOrderByAssessedAtDescIdDesc(
                                tenderId
                        )
                        .orElseThrow(
                                () -> new
                                        AwardIntegrityAssessmentNotFoundException(
                                        "No award integrity "
                                                + "assessment was found "
                                                + "for tender "
                                                + tenderId
                                                + "."
                                )
                        );

        return toResponse(
                assessment
        );
    }

    @Transactional(readOnly = true)
    public Page<PersistedAwardIntegrityAssessmentResponse>
    getTenderAssessmentHistory(
            Long tenderId,
            Pageable pageable
    ) {
        requirePositiveIdentifier(
                tenderId,
                "Tender ID"
        );

        requirePageable(
                pageable
        );

        return assessmentRepository
                .findByTenderId(
                        tenderId,
                        pageable
                )
                .map(
                        this::toResponse
                );
    }

    @Transactional(readOnly = true)
    public Page<PersistedAwardIntegrityAssessmentResponse>
    getReviewQueue(
            AwardIntegrityAssessment.ReviewStatus reviewStatus,
            Pageable pageable
    ) {
        if (reviewStatus == null) {
            throw new IllegalArgumentException(
                    "Review status is required."
            );
        }

        requirePageable(
                pageable
        );

        return assessmentRepository
                .findByReviewStatus(
                        reviewStatus,
                        pageable
                )
                .map(
                        this::toResponse
                );
    }

    @Transactional(readOnly = true)
    public Page<PersistedAwardIntegrityAssessmentResponse>
    getRiskQueue(
            AwardIntegrityAssessment.RiskLevel riskLevel,
            Pageable pageable
    ) {
        if (riskLevel == null) {
            throw new IllegalArgumentException(
                    "Risk level is required."
            );
        }

        requirePageable(
                pageable
        );

        return assessmentRepository
                .findByRiskLevel(
                        riskLevel,
                        pageable
                )
                .map(
                        this::toResponse
                );
    }

    @Transactional(readOnly = true)
    public Page<PersistedAwardIntegrityAssessmentResponse>
    getReviewQueue(
            AwardIntegrityAssessment.ReviewStatus reviewStatus,
            AwardIntegrityAssessment.RiskLevel riskLevel,
            Pageable pageable
    ) {
        if (reviewStatus == null) {
            throw new IllegalArgumentException(
                    "Review status is required."
            );
        }

        if (riskLevel == null) {
            throw new IllegalArgumentException(
                    "Risk level is required."
            );
        }

        requirePageable(
                pageable
        );

        return assessmentRepository
                .findByReviewStatusAndRiskLevel(
                        reviewStatus,
                        riskLevel,
                        pageable
                )
                .map(
                        this::toResponse
                );
    }

    @Transactional(readOnly = true)
    public long countByReviewStatus(
            AwardIntegrityAssessment.ReviewStatus reviewStatus
    ) {
        if (reviewStatus == null) {
            throw new IllegalArgumentException(
                    "Review status is required."
            );
        }

        return assessmentRepository
                .countByReviewStatus(
                        reviewStatus
                );
    }

    @Transactional(readOnly = true)
    public long countByRiskLevel(
            AwardIntegrityAssessment.RiskLevel riskLevel
    ) {
        if (riskLevel == null) {
            throw new IllegalArgumentException(
                    "Risk level is required."
            );
        }

        return assessmentRepository
                .countByRiskLevel(
                        riskLevel
                );
    }

    @Transactional
    public PersistedAwardIntegrityAssessmentResponse
    beginReview(
            Long assessmentId,
            AwardIntegrityBeginReviewRequest request,
            String reviewer
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Begin-review request is required."
            );
        }

        String normalizedReviewer =
                requireText(
                        reviewer,
                        "Reviewer"
                );

        AwardIntegrityAssessment assessment =
                requireAssessment(
                        assessmentId
                );

        verifyExpectedVersion(
                assessment,
                request.expectedVersion()
        );

        try {
            assessment.beginReview(
                    normalizedReviewer
            );

            AwardIntegrityAssessment savedAssessment =
                    assessmentRepository.saveAndFlush(
                            assessment
                    );

            log.info(
                    "Award integrity review started: "
                            + "assessmentId={} "
                            + "reference={} "
                            + "tenderId={} "
                            + "reviewer={} "
                            + "version={}",
                    savedAssessment.getId(),
                    savedAssessment
                            .getAssessmentReference(),
                    savedAssessment.getTenderId(),
                    normalizedReviewer,
                    savedAssessment.getVersion()
            );

            return toResponse(
                    savedAssessment
            );

        } catch (IllegalStateException exception) {
            throw workflowConflict(
                    assessment,
                    exception
            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw optimisticVersionConflict(
                    assessment,
                    exception
            );
        }
    }

    @Transactional
    public PersistedAwardIntegrityAssessmentResponse
    acceptWithMonitoring(
            Long assessmentId,
            AwardIntegrityReviewRequest request,
            String reviewer
    ) {
        return applyReviewDecision(
                assessmentId,
                request,
                reviewer,
                ReviewDecision.ACCEPT_WITH_MONITORING
        );
    }

    @Transactional
    public PersistedAwardIntegrityAssessmentResponse
    requireCompetitionReview(
            Long assessmentId,
            AwardIntegrityReviewRequest request,
            String reviewer
    ) {
        return applyReviewDecision(
                assessmentId,
                request,
                reviewer,
                ReviewDecision.REQUIRE_COMPETITION_REVIEW
        );
    }

    @Transactional
    public PersistedAwardIntegrityAssessmentResponse
    escalate(
            Long assessmentId,
            AwardIntegrityReviewRequest request,
            String reviewer
    ) {
        return applyReviewDecision(
                assessmentId,
                request,
                reviewer,
                ReviewDecision.ESCALATE
        );
    }

    @Transactional
    public PersistedAwardIntegrityAssessmentResponse
    dismiss(
            Long assessmentId,
            AwardIntegrityReviewRequest request,
            String reviewer
    ) {
        return applyReviewDecision(
                assessmentId,
                request,
                reviewer,
                ReviewDecision.DISMISS
        );
    }

    private PersistedAwardIntegrityAssessmentResponse
    applyReviewDecision(
            Long assessmentId,
            AwardIntegrityReviewRequest request,
            String reviewer,
            ReviewDecision decision
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Award integrity review "
                            + "request is required."
            );
        }

        if (decision == null) {
            throw new IllegalArgumentException(
                    "Review decision is required."
            );
        }

        String normalizedReviewer =
                requireText(
                        reviewer,
                        "Reviewer"
                );

        String normalizedNotes =
                requireText(
                        request.reviewNotes(),
                        "Review notes"
                );

        AwardIntegrityAssessment assessment =
                requireAssessment(
                        assessmentId
                );

        verifyExpectedVersion(
                assessment,
                request.expectedVersion()
        );

        try {
            applyDecision(
                    assessment,
                    decision,
                    normalizedReviewer,
                    normalizedNotes
            );

            AwardIntegrityAssessment savedAssessment =
                    assessmentRepository.saveAndFlush(
                            assessment
                    );

            log.info(
                    "Award integrity review decision "
                            + "recorded: assessmentId={} "
                            + "reference={} "
                            + "tenderId={} "
                            + "decision={} "
                            + "reviewStatus={} "
                            + "reviewer={} "
                            + "version={}",
                    savedAssessment.getId(),
                    savedAssessment
                            .getAssessmentReference(),
                    savedAssessment.getTenderId(),
                    decision,
                    savedAssessment.getReviewStatus(),
                    normalizedReviewer,
                    savedAssessment.getVersion()
            );

            return toResponse(
                    savedAssessment
            );

        } catch (IllegalStateException exception) {
            throw workflowConflict(
                    assessment,
                    exception
            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw optimisticVersionConflict(
                    assessment,
                    exception
            );
        }
    }

    private void applyDecision(
            AwardIntegrityAssessment assessment,
            ReviewDecision decision,
            String reviewer,
            String reviewNotes
    ) {
        switch (decision) {
            case ACCEPT_WITH_MONITORING ->
                    assessment.acceptWithMonitoring(
                            reviewer,
                            reviewNotes
                    );

            case REQUIRE_COMPETITION_REVIEW ->
                    assessment.requireCompetitionReview(
                            reviewer,
                            reviewNotes
                    );

            case ESCALATE ->
                    assessment.escalate(
                            reviewer,
                            reviewNotes
                    );

            case DISMISS ->
                    assessment.dismiss(
                            reviewer,
                            reviewNotes
                    );
        }
    }

    private AwardIntegrityAssessmentResult
    persistAssessment(
            AwardIntegrityAssessmentRequest request,
            AwardIntegrityAssessmentResponse response,
            String requestSnapshotJson,
            String requestSnapshotSha256
    ) {
        String signalsJson =
                serializeSnapshot(
                        response.signals(),
                        "award integrity signals"
                );

        LocalDateTime assessedAt =
                response.assessedAt() == null
                        ? LocalDateTime.now()
                        : response
                        .assessedAt()
                        .toLocalDateTime();

        AwardIntegrityAssessment assessment =
                AwardIntegrityAssessment.create(
                        request.tenderId(),
                        AwardIntegrityAssessment
                                .AssessmentType
                                .valueOf(
                                        response.assessmentType()
                                ),
                        response.riskScore(),
                        AwardIntegrityAssessment
                                .RiskLevel
                                .valueOf(
                                        response.riskLevel()
                                ),
                        AwardIntegrityAssessment
                                .Recommendation
                                .valueOf(
                                        response.recommendation()
                                ),
                        response.ruleVersion(),
                        Boolean.TRUE.equals(
                                response.modelUsed()
                        ),
                        Boolean.TRUE.equals(
                                response
                                        .awardConsistentWithRules()
                        ),
                        requestSnapshotJson,
                        requestSnapshotSha256,
                        signalsJson,
                        assessedAt
                );

        try {
            AwardIntegrityAssessment savedAssessment =
                    assessmentRepository.saveAndFlush(
                            assessment
                    );

            logAssessmentResult(
                    response
            );

            log.info(
                    "Award integrity assessment "
                            + "persisted: assessmentId={} "
                            + "reference={} "
                            + "tenderId={} "
                            + "reviewStatus={} "
                            + "version={}",
                    savedAssessment.getId(),
                    savedAssessment
                            .getAssessmentReference(),
                    savedAssessment.getTenderId(),
                    savedAssessment.getReviewStatus(),
                    savedAssessment.getVersion()
            );

            return AwardIntegrityAssessmentResult
                    .created(
                            toResponse(
                                    savedAssessment
                            )
                    );

        } catch (
                DataIntegrityViolationException exception
        ) {
            return assessmentRepository
                    .findByTenderIdAndRuleVersionAndRequestSnapshotSha256(
                            request.tenderId(),
                            response.ruleVersion(),
                            requestSnapshotSha256
                    )
                    .map(
                            existingAssessment ->
                                    AwardIntegrityAssessmentResult
                                            .existing(
                                                    toResponse(
                                                            existingAssessment
                                                    )
                                            )
                    )
                    .orElseThrow(
                            () -> new
                                    AwardIntegrityAssessmentConflictException(
                                    "The award integrity "
                                            + "assessment could not "
                                            + "be persisted because "
                                            + "its evidence conflicts "
                                            + "with an existing "
                                            + "assessment.",
                                    exception
                            )
                    );
        }
    }

    private AwardIntegrityAssessment
    requireAssessment(
            Long assessmentId
    ) {
        requirePositiveIdentifier(
                assessmentId,
                "Assessment ID"
        );

        return assessmentRepository
                .findById(
                        assessmentId
                )
                .orElseThrow(
                        () -> new
                                AwardIntegrityAssessmentNotFoundException(
                                "Award integrity assessment "
                                        + assessmentId
                                        + " was not found."
                        )
                );
    }

    private void verifyExpectedVersion(
            AwardIntegrityAssessment assessment,
            Long expectedVersion
    ) {
        if (expectedVersion == null) {
            throw new IllegalArgumentException(
                    "Expected version is required."
            );
        }

        if (expectedVersion < 0) {
            throw new IllegalArgumentException(
                    "Expected version cannot be negative."
            );
        }

        Long currentVersion =
                assessment.getVersion();

        if (
                currentVersion == null
                        || !currentVersion.equals(
                        expectedVersion
                )
        ) {
            throw new
                    AwardIntegrityAssessmentVersionConflictException(
                    "Award integrity assessment "
                            + assessment.getId()
                            + " has version "
                            + currentVersion
                            + ", but version "
                            + expectedVersion
                            + " was submitted. Refresh "
                            + "the assessment before "
                            + "submitting another review "
                            + "decision."
            );
        }
    }

    private AwardIntegrityAssessmentConflictException
    workflowConflict(
            AwardIntegrityAssessment assessment,
            IllegalStateException exception
    ) {
        return new
                AwardIntegrityAssessmentConflictException(
                "Award integrity assessment "
                        + assessment.getId()
                        + " cannot complete the "
                        + "requested workflow "
                        + "transition. "
                        + exception.getMessage(),
                exception
        );
    }

    private AwardIntegrityAssessmentVersionConflictException
    optimisticVersionConflict(
            AwardIntegrityAssessment assessment,
            OptimisticLockingFailureException exception
    ) {
        return new
                AwardIntegrityAssessmentVersionConflictException(
                "Award integrity assessment "
                        + assessment.getId()
                        + " was modified by another "
                        + "request. Refresh the "
                        + "assessment before "
                        + "submitting another review "
                        + "decision.",
                exception
        );
    }

    private PersistedAwardIntegrityAssessmentResponse
    toResponse(
            AwardIntegrityAssessment assessment
    ) {
        return PersistedAwardIntegrityAssessmentResponse
                .from(
                        assessment,
                        objectMapper
                );
    }

    private String serializeSnapshot(
            Object value,
            String snapshotName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    snapshotName
                            + " is required."
            );
        }

        try {
            return objectMapper
                    .writeValueAsString(
                            value
                    );

        } catch (
                JsonProcessingException exception
        ) {
            throw new IllegalStateException(
                    "Could not serialize the "
                            + snapshotName
                            + ".",
                    exception
            );
        }
    }

    private String calculateSha256(
            String value
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(
                            hash
                    );

        } catch (
                NoSuchAlgorithmException exception
        ) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception
            );
        }
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
                            request.activeEvaluatorCount(),
                            request.eligibleBidCount()
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

        for (
                Long evaluatorUserId
                : evaluatorUserIds
        ) {
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

        validateResponseRiskScore(
                response.riskScore()
        );

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
                                response.recommendation()
                        )
        ) {
            throw new MlInferenceException(
                    "Award integrity response "
                            + "contains an unsupported "
                            + "recommendation."
            );
        }

        requireResponseText(
                response.ruleVersion(),
                "Rule version"
        );

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

        if (
                response.awardConsistentWithRules()
                        == null
        ) {
            throw new MlInferenceException(
                    "Award integrity response is "
                            + "missing the award rules "
                            + "consistency result."
            );
        }

        validateSignals(
                response.signals()
        );
    }

    private void validateResponseRiskScore(
            BigDecimal riskScore
    ) {
        if (
                riskScore == null
                        || riskScore.compareTo(
                        BigDecimal.ZERO
                ) < 0
                        || riskScore.compareTo(
                        BigDecimal.ONE
                ) > 0
        ) {
            throw new MlInferenceException(
                    "Award integrity response "
                            + "contains a risk score "
                            + "outside the supported "
                            + "zero-to-one range."
            );
        }
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

            try {
                calculatedRiskPoints =
                        Math.addExact(
                                calculatedRiskPoints,
                                signal.riskPoints()
                        );

            } catch (ArithmeticException exception) {
                throw new MlInferenceException(
                        "Award integrity signal risk "
                                + "points exceed the "
                                + "supported range.",
                        exception
                );
            }
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

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " is required."
            );
        }

        return value.trim();
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

    private static void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Pageable request is required."
            );
        }
    }

    private enum ReviewDecision {
        ACCEPT_WITH_MONITORING,
        REQUIRE_COMPETITION_REVIEW,
        ESCALATE,
        DISMISS
    }
}