package com.kasibridge.fraud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "award_integrity_assessments",
        schema = "fraud",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_award_integrity_reference",
                        columnNames = {
                                "assessment_reference"
                        }
                ),
                @UniqueConstraint(name = "uk_award_integrity_evidence",
                        columnNames = {
                                "tender_id",
                                "rule_version",
                                "request_snapshot_sha256"
                        }
                )
        },
        indexes = {@Index(name = "idx_award_integrity_tender", columnList = "tender_id"),
                @Index(name = "idx_award_integrity_risk", columnList = "risk_level"),
                @Index(name = "idx_award_integrity_review", columnList = "review_status"),
                @Index(name = "idx_award_integrity_queue", columnList =
                        "review_status, "
                                        + "risk_level, "
                                        + "assessed_at"
                ),
                @Index(name = "idx_award_integrity_assessed_at", columnList = "assessed_at")
        }
)
@Getter
@NoArgsConstructor
public class AwardIntegrityAssessment {

    private static final String
            REFERENCE_PREFIX =
            "KB-AWARD-RISK-";

    private static final int
            MAX_REVIEW_NOTES_LENGTH =
            1000;

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "assessment_reference",
            nullable = false,
            unique = true,
            updatable = false,
            length = 40
    )
    private String assessmentReference;

    @Column(
            name = "tender_id",
            nullable = false,
            updatable = false
    )
    private Long tenderId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "assessment_type",
            nullable = false,
            updatable = false,
            length = 60
    )
    private AssessmentType assessmentType;

    @Column(
            name = "risk_score",
            nullable = false,
            updatable = false,
            precision = 10,
            scale = 8
    )
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "risk_level",
            nullable = false,
            updatable = false,
            length = 20
    )
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "recommendation",
            nullable = false,
            updatable = false,
            length = 60
    )
    private Recommendation recommendation;

    @Column(
            name = "rule_version",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String ruleVersion;

    @Column(
            name = "model_used",
            nullable = false,
            updatable = false
    )
    private boolean modelUsed;

    @Column(
            name = "award_consistent_with_rules",
            nullable = false,
            updatable = false
    )
    private boolean awardConsistentWithRules;

    @Column(
            name = "request_snapshot_json",
            nullable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private String requestSnapshotJson;

    @Column(
            name = "request_snapshot_sha256",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String requestSnapshotSha256;

    @Column(
            name = "signals_json",
            nullable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private String signalsJson;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "review_status",
            nullable = false,
            length = 40
    )
    private ReviewStatus reviewStatus;

    @Column(
            name = "reviewed_by",
            length = 100
    )
    private String reviewedBy;

    @Column(
            name = "review_notes",
            length = MAX_REVIEW_NOTES_LENGTH
    )
    private String reviewNotes;

    @Column(
            name = "reviewed_at"
    )
    private LocalDateTime reviewedAt;

    @Column(
            name = "assessed_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime assessedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at"
    )
    private LocalDateTime updatedAt;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    public static AwardIntegrityAssessment create(
            Long tenderId,
            AssessmentType assessmentType,
            BigDecimal riskScore,
            RiskLevel riskLevel,
            Recommendation recommendation,
            String ruleVersion,
            boolean modelUsed,
            boolean awardConsistentWithRules,
            String requestSnapshotJson,
            String requestSnapshotSha256,
            String signalsJson,
            LocalDateTime assessedAt
    ) {
        AwardIntegrityAssessment assessment =
                new AwardIntegrityAssessment();

        assessment.tenderId =
                requirePositiveIdentifier(
                        tenderId,
                        "Tender ID"
                );

        assessment.assessmentType =
                requireValue(
                        assessmentType,
                        "Assessment type"
                );

        assessment.riskScore =
                requireProbability(
                        riskScore,
                        "Risk score"
                );

        assessment.riskLevel =
                requireValue(
                        riskLevel,
                        "Risk level"
                );

        assessment.recommendation =
                requireValue(
                        recommendation,
                        "Recommendation"
                );

        assessment.ruleVersion =
                normalizeRequiredText(
                        ruleVersion,
                        "Rule version",
                        100
                );

        assessment.modelUsed =
                modelUsed;

        assessment.awardConsistentWithRules =
                awardConsistentWithRules;

        assessment.requestSnapshotJson =
                normalizeJsonSnapshot(
                        requestSnapshotJson,
                        "Request snapshot JSON"
                );

        assessment.requestSnapshotSha256 =
                requireSha256(
                        requestSnapshotSha256,
                        "Request snapshot SHA-256"
                );

        assessment.signalsJson =
                normalizeJsonSnapshot(
                        signalsJson,
                        "Signals JSON"
                );

        assessment.assessedAt =
                assessedAt != null
                        ? assessedAt
                        : LocalDateTime.now();

        assessment.reviewStatus =
                defaultReviewStatus(
                        riskLevel,
                        recommendation,
                        awardConsistentWithRules
                );

        return assessment;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now =
                LocalDateTime.now();

        if (
                assessmentReference == null
                        || assessmentReference.isBlank()
        ) {
            assessmentReference =
                    generateAssessmentReference();
        }

        if (assessedAt == null) {
            assessedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (reviewStatus == null) {
            reviewStatus =
                    defaultReviewStatus(
                            riskLevel,
                            recommendation,
                            awardConsistentWithRules
                    );
        }

        validateState();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt =
                LocalDateTime.now();

        validateState();
    }

    public void beginReview(
            String reviewer
    ) {
        requireReviewStatus(
                ReviewStatus.PENDING_REVIEW
        );

        reviewedBy =
                normalizeRequiredText(
                        reviewer,
                        "Reviewer",
                        100
                );

        reviewNotes = null;

        reviewedAt =
                LocalDateTime.now();

        reviewStatus =
                ReviewStatus.UNDER_REVIEW;
    }

    public void acceptWithMonitoring(
            String reviewer,
            String notes
    ) {
        requireOpenReviewStatus();

        setFinalReviewDetails(
                reviewer,
                notes
        );

        reviewStatus =
                ReviewStatus
                        .ACCEPTED_WITH_MONITORING;
    }

    public void requireCompetitionReview(
            String reviewer,
            String notes
    ) {
        requireOpenReviewStatus();

        setFinalReviewDetails(
                reviewer,
                notes
        );

        reviewStatus =
                ReviewStatus
                        .COMPETITION_REVIEW_REQUIRED;
    }

    public void escalate(
            String reviewer,
            String notes
    ) {
        requireOpenReviewStatus();

        setFinalReviewDetails(
                reviewer,
                notes
        );

        reviewStatus =
                ReviewStatus.ESCALATED;
    }

    public void dismiss(
            String reviewer,
            String notes
    ) {
        requireOpenReviewStatus();

        setFinalReviewDetails(
                reviewer,
                notes
        );

        reviewStatus =
                ReviewStatus.DISMISSED;
    }

    public boolean isOpenForReview() {
        return (
                reviewStatus
                        == ReviewStatus.PENDING_REVIEW
                        || reviewStatus
                        == ReviewStatus.UNDER_REVIEW
                        || reviewStatus
                        == ReviewStatus.ESCALATED
        );
    }

    public boolean isFinalReviewDecision() {
        return (
                reviewStatus
                        == ReviewStatus
                        .ACCEPTED_WITH_MONITORING
                        || reviewStatus
                        == ReviewStatus
                        .COMPETITION_REVIEW_REQUIRED
                        || reviewStatus
                        == ReviewStatus.DISMISSED
        );
    }

    private void setFinalReviewDetails(
            String reviewer,
            String notes
    ) {
        reviewedBy =
                normalizeRequiredText(
                        reviewer,
                        "Reviewer",
                        100
                );

        reviewNotes =
                normalizeRequiredText(
                        notes,
                        "Review notes",
                        MAX_REVIEW_NOTES_LENGTH
                );

        reviewedAt =
                LocalDateTime.now();
    }

    private void requireOpenReviewStatus() {
        if (
                reviewStatus
                        != ReviewStatus.PENDING_REVIEW
                        && reviewStatus
                        != ReviewStatus.UNDER_REVIEW
                        && reviewStatus
                        != ReviewStatus.ESCALATED
        ) {
            throw new IllegalStateException(
                    "Award integrity assessment "
                            + "is not open for review. "
                            + "Current status: "
                            + reviewStatus
                            + "."
            );
        }
    }

    private void requireReviewStatus(
            ReviewStatus requiredStatus
    ) {
        if (reviewStatus != requiredStatus) {
            throw new IllegalStateException(
                    "Award integrity assessment "
                            + "must be in "
                            + requiredStatus
                            + " status. Current "
                            + "status: "
                            + reviewStatus
                            + "."
            );
        }
    }

    private void validateState() {
        requirePositiveIdentifier(
                tenderId,
                "Tender ID"
        );

        requireValue(
                assessmentType,
                "Assessment type"
        );

        requireProbability(
                riskScore,
                "Risk score"
        );

        requireValue(
                riskLevel,
                "Risk level"
        );

        requireValue(
                recommendation,
                "Recommendation"
        );

        normalizeRequiredText(
                ruleVersion,
                "Rule version",
                100
        );

        normalizeJsonSnapshot(
                requestSnapshotJson,
                "Request snapshot JSON"
        );

        requireSha256(
                requestSnapshotSha256,
                "Request snapshot SHA-256"
        );

        normalizeJsonSnapshot(
                signalsJson,
                "Signals JSON"
        );

        requireValue(
                reviewStatus,
                "Review status"
        );

        validateReviewMetadata();
    }

    private void validateReviewMetadata() {
        if (
                reviewStatus
                        == ReviewStatus.PENDING_REVIEW
        ) {
            if (
                    reviewedBy != null
                            || reviewNotes != null
                            || reviewedAt != null
            ) {
                throw new IllegalStateException(
                        "Pending assessments cannot "
                                + "contain reviewer "
                                + "metadata."
                );
            }

            return;
        }

        if (
                reviewStatus
                        == ReviewStatus.UNDER_REVIEW
        ) {
            normalizeRequiredText(
                    reviewedBy,
                    "Reviewer",
                    100
            );

            if (reviewedAt == null) {
                throw new IllegalStateException(
                        "An assessment under review "
                                + "must have a review "
                                + "timestamp."
                );
            }

            return;
        }

        /*
         * A low-risk assessment may be accepted
         * automatically when the award is consistent
         * with the rules and the recommendation is
         * ACCEPT_WITH_MONITORING.
         *
         * This auto-cleared state has no human
         * reviewer metadata.
         */
        if (
                reviewStatus
                        == ReviewStatus
                        .ACCEPTED_WITH_MONITORING
                        && reviewedBy == null
                        && reviewNotes == null
                        && reviewedAt == null
        ) {
            return;
        }

        /*
         * All human final decisions require the
         * reviewer, review notes and review timestamp.
         */
        normalizeRequiredText(
                reviewedBy,
                "Reviewer",
                100
        );

        normalizeRequiredText(
                reviewNotes,
                "Review notes",
                MAX_REVIEW_NOTES_LENGTH
        );

        if (reviewedAt == null) {
            throw new IllegalStateException(
                    "A completed review must have "
                            + "a review timestamp."
            );
        }
    }

    private static ReviewStatus
    defaultReviewStatus(
            RiskLevel riskLevel,
            Recommendation recommendation,
            boolean awardConsistentWithRules
    ) {
        Objects.requireNonNull(
                riskLevel,
                "Risk level is required."
        );

        Objects.requireNonNull(
                recommendation,
                "Recommendation is required."
        );

        if (!awardConsistentWithRules) {
            return ReviewStatus.PENDING_REVIEW;
        }

        if (
                riskLevel == RiskLevel.LOW
                        && recommendation
                        == Recommendation
                        .ACCEPT_WITH_MONITORING
        ) {
            return ReviewStatus
                    .ACCEPTED_WITH_MONITORING;
        }

        return ReviewStatus.PENDING_REVIEW;
    }

    private static String
    generateAssessmentReference() {
        String randomPart =
                UUID.randomUUID()
                        .toString()
                        .replace(
                                "-",
                                ""
                        )
                        .substring(
                                0,
                                12
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        return REFERENCE_PREFIX
                + randomPart;
    }

    private static Long
    requirePositiveIdentifier(
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

        return value;
    }

    private static BigDecimal
    requireProbability(
            BigDecimal value,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName
                            + " is required."
            );
        }

        if (
                value.compareTo(
                        BigDecimal.ZERO
                ) < 0
                        || value.compareTo(
                        BigDecimal.ONE
                ) > 0
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be between "
                            + "zero and one."
            );
        }

        return value;
    }

    private static String
    requireSha256(
            String value,
            String fieldName
    ) {
        String normalized =
                normalizeRequiredText(
                        value,
                        fieldName,
                        64
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                !normalized.matches(
                        "^[0-9a-f]{64}$"
                )
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must contain exactly "
                            + "64 hexadecimal "
                            + "characters."
            );
        }

        return normalized;
    }

    private static String
    normalizeJsonSnapshot(
            String value,
            String fieldName
    ) {
        String normalized =
                normalizeRequiredText(
                        value,
                        fieldName,
                        Integer.MAX_VALUE
                );

        if (
                !(
                        normalized.startsWith(
                                "{"
                        )
                                && normalized.endsWith(
                                "}"
                        )
                )
                        && !(
                        normalized.startsWith(
                                "["
                        )
                                && normalized.endsWith(
                                "]"
                        )
                )
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must contain a JSON "
                            + "object or array."
            );
        }

        return normalized;
    }

    private static String
    normalizeRequiredText(
            String value,
            String fieldName,
            int maximumLength
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

        String normalized =
                value.trim();

        if (
                normalized.length()
                        > maximumLength
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " cannot exceed "
                            + maximumLength
                            + " characters."
            );
        }

        return normalized;
    }

    private static <T> T requireValue(
            T value,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName
                            + " is required."
            );
        }

        return value;
    }

    public enum AssessmentType {
        DETERMINISTIC_AWARD_INTEGRITY
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum Recommendation {
        ACCEPT_WITH_MONITORING,
        REVIEW_COMPETITION,
        REQUIRE_GOVERNANCE_REVIEW,
        ESCALATE_FOR_INVESTIGATION
    }

    public enum ReviewStatus {
        PENDING_REVIEW,
        UNDER_REVIEW,
        ACCEPTED_WITH_MONITORING,
        COMPETITION_REVIEW_REQUIRED,
        ESCALATED,
        DISMISSED
    }
}