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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "fraud_assessments",
        schema = "fraud",
        uniqueConstraints = {@UniqueConstraint(name = "uk_fraud_assessment_invoice_model",
                columnNames = {"invoice_reference", "model_version"}),
                @UniqueConstraint(name = "uk_fraud_assessment_reference", columnNames = {"assessment_reference"}
                )
        },
        indexes = {
                @Index(name = "idx_fraud_assessment_invoice", columnList = "invoice_reference"),
                @Index(name = "idx_fraud_assessment_supplier", columnList = "supplier_id"),
                @Index(name = "idx_fraud_assessment_department", columnList = "department_id"),
                @Index(name = "idx_fraud_assessment_risk", columnList = "risk_level"),
                @Index(name = "idx_fraud_assessment_review", columnList = "review_status"),
                @Index(name = "idx_fraud_assessment_model", columnList = "model_version"),
                @Index(name = "idx_fraud_assessment_time", columnList = "assessed_at"),
                @Index(name = "idx_fraud_assessment_queue",
                        columnList =
                                "review_status, risk_level, "
                                        + "assessed_at"
                ),
                @Index(name = "idx_fraud_assessment_supplier_time", columnList = "supplier_id, assessed_at")
        }
)
@Getter
@NoArgsConstructor
public class FraudAssessment {

    private static final DateTimeFormatter
            REFERENCE_DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyyMMdd",
                    Locale.ROOT
            );

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(name = "assessment_reference", nullable = false, unique = true, updatable = false, length = 40)
    private String assessmentReference;

    @Column(name = "invoice_reference", nullable = false, updatable = false, length = 100)
    private String invoiceReference;

    @Column(name = "supplier_id", updatable = false, length = 100)
    private String supplierId;

    @Column(name = "department_id", updatable = false, length = 100)
    private String departmentId;

    @Column(
            name = "invoice_amount",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal invoiceAmount;

    @Column(name = "supplier_country", updatable = false, length = 3)
    private String supplierCountry;

    @Column(name = "region", updatable = false, length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, updatable = false, length = 20)
    private Classification classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, updatable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "risk_probability", nullable = false, updatable = false, precision = 10, scale = 8)
    private BigDecimal riskProbability;

    @Column(name = "decision_threshold", nullable = false, updatable = false, precision = 10, scale = 8)
    private BigDecimal threshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", nullable = false, updatable = false, length = 40)
    private RecommendedAction recommendedAction;

    @Column(name = "model_name", nullable = false, updatable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false, updatable = false, length = 100)
    private String modelVersion;

    @Column(name = "experiment", nullable = false, updatable = false, length = 100)
    private String experiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_source", nullable = false, updatable = false, length = 30)
    private DetectionSource detectionSource;

    @Column(
            name = "model_sha256",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String modelSha256;

    @Column(name = "feature_snapshot_json", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String featureSnapshotJson;

    @Column(name = "feature_snapshot_sha256", nullable = false, updatable = false, length = 64)
    private String featureSnapshotSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private ReviewStatus reviewStatus;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "assessed_at", nullable = false, updatable = false)
    private LocalDateTime assessedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static FraudAssessment create(
            String invoiceReference,
            String supplierId,
            String departmentId,
            BigDecimal invoiceAmount,
            String supplierCountry,
            String region,
            Classification classification,
            RiskLevel riskLevel,
            BigDecimal riskProbability,
            BigDecimal threshold,
            RecommendedAction recommendedAction,
            String modelName,
            String modelVersion,
            String experiment,
            DetectionSource detectionSource,
            String modelSha256,
            String featureSnapshotJson,
            String featureSnapshotSha256,
            LocalDateTime assessedAt
    ) {
        FraudAssessment assessment =
                new FraudAssessment();

        assessment.invoiceReference =
                normalizeRequiredText(
                        invoiceReference,
                        "Invoice reference"
                );

        assessment.supplierId =
                normalizeOptionalText(
                        supplierId
                );

        assessment.departmentId =
                normalizeOptionalText(
                        departmentId
                );

        assessment.invoiceAmount =
                requireNonNegativeAmount(
                        invoiceAmount
                );

        assessment.supplierCountry =
                normalizeCountry(
                        supplierCountry
                );

        assessment.region =
                normalizeOptionalText(
                        region
                );

        assessment.classification =
                requireValue(
                        classification,
                        "Classification"
                );

        assessment.riskLevel =
                requireValue(
                        riskLevel,
                        "Risk level"
                );

        assessment.riskProbability =
                requireProbability(
                        riskProbability,
                        "Risk probability"
                );

        assessment.threshold =
                requireProbability(
                        threshold,
                        "Decision threshold"
                );

        assessment.recommendedAction =
                requireValue(
                        recommendedAction,
                        "Recommended action"
                );

        assessment.modelName =
                normalizeRequiredText(
                        modelName,
                        "Model name"
                );

        assessment.modelVersion =
                normalizeRequiredText(
                        modelVersion,
                        "Model version"
                );

        assessment.experiment =
                normalizeRequiredText(
                        experiment,
                        "Experiment"
                );

        assessment.detectionSource =
                requireValue(
                        detectionSource,
                        "Detection source"
                );

        assessment.modelSha256 =
                requireSha256(
                        modelSha256,
                        "Model SHA-256"
                );

        assessment.featureSnapshotJson =
                normalizeRequiredText(
                        featureSnapshotJson,
                        "Feature snapshot JSON"
                );

        assessment.featureSnapshotSha256 =
                requireSha256(
                        featureSnapshotSha256,
                        "Feature snapshot SHA-256"
                );

        assessment.assessedAt =
                assessedAt != null
                        ? assessedAt
                        : LocalDateTime.now();

        assessment.reviewStatus =
                defaultReviewStatus(
                        classification
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
                    generateAssessmentReference(
                            now.toLocalDate()
                    );
        }

        if (
                invoiceReference == null
                        || invoiceReference.isBlank()
        ) {
            invoiceReference =
                    generateInvoiceReference();
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
                            classification
                    );
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt =
                LocalDateTime.now();
    }

    public void beginReview(String reviewer, String notes) {
        requireStatus(
                ReviewStatus.PENDING_REVIEW
        );

        setReviewDetails(
                reviewer,
                notes
        );

        reviewStatus =
                ReviewStatus.PENDING_REVIEW;
    }

    public void confirmFraud(String reviewer, String notes) {
        requireStatus(
                ReviewStatus.PENDING_REVIEW,
                ReviewStatus.ESCALATED
        );

        setReviewDetails(
                reviewer,
                notes
        );

        reviewStatus =
                ReviewStatus.CONFIRMED_FRAUD;
    }

    public void markFalsePositive(String reviewer, String notes) {
        requireStatus(
                ReviewStatus.PENDING_REVIEW,
                ReviewStatus.ESCALATED
        );

        setReviewDetails(
                reviewer,
                notes
        );

        reviewStatus =
                ReviewStatus.FALSE_POSITIVE;
    }

    public void escalate(String reviewer, String notes) {
        requireStatus(
                ReviewStatus.PENDING_REVIEW,
                ReviewStatus.AUTO_CLEARED
        );

        setReviewDetails(
                reviewer,
                notes
        );

        reviewStatus =
                ReviewStatus.ESCALATED;
    }

    private void setReviewDetails(String reviewer, String notes) {
        reviewedBy =
                normalizeRequiredText(
                        reviewer,
                        "Reviewer"
                );

        reviewNotes =
                normalizeOptionalText(
                        notes
                );

        reviewedAt =
                LocalDateTime.now();
    }

    private void requireStatus(
            ReviewStatus... allowedStatuses
    ) {
        for (
                ReviewStatus allowedStatus
                : allowedStatuses
        ) {
            if (
                    reviewStatus
                            == allowedStatus
            ) {
                return;
            }
        }

        throw new IllegalStateException(
                "Assessment status "
                        + reviewStatus
                        + " does not allow this "
                        + "review transition."
        );
    }

    private static ReviewStatus defaultReviewStatus(
            Classification classification
    ) {
        return classification
                == Classification.HIGH_RISK
                ? ReviewStatus.PENDING_REVIEW
                : ReviewStatus.AUTO_CLEARED;
    }

    private static String generateAssessmentReference(LocalDate date) {
        return "KB-FA-"
                + REFERENCE_DATE_FORMAT
                .format(date)
                + "-"
                + randomReferenceSuffix();
    }

    private static String generateInvoiceReference() {
        return "KB-INV-AUTO-"
                + randomReferenceSuffix();
    }

    private static String
    randomReferenceSuffix() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private static BigDecimal requireNonNegativeAmount(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Invoice amount is required."
            );
        }

        if (
                value.compareTo(
                        BigDecimal.ZERO
                ) < 0
        ) {
            throw new IllegalArgumentException(
                    "Invoice amount cannot "
                            + "be negative."
            );
        }

        return value;
    }

    private static BigDecimal requireProbability(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
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
                            + "0 and 1."
            );
        }

        return value;
    }

    private static String requireSha256(String value, String fieldName) {
        String normalized =
                normalizeRequiredText(
                        value,
                        fieldName
                ).toLowerCase(
                        Locale.ROOT
                );

        if (
                !normalized.matches(
                        "^[a-f0-9]{64}$"
                )
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must contain exactly "
                            + "64 hexadecimal characters."
            );
        }

        return normalized;
    }

    private static String normalizeCountry(String value) {
        String normalized =
                normalizeOptionalText(
                        value
                );

        if (normalized == null) {
            return null;
        }

        normalized =
                normalized.toUpperCase(
                        Locale.ROOT
                );

        if (
                normalized.length() < 2
                        || normalized.length() > 3
        ) {
            throw new IllegalArgumentException(
                    "Supplier country must contain "
                            + "2 or 3 characters."
            );
        }

        return normalized;
    }

    private static String normalizeRequiredText(String value, String fieldName) {
        String normalized =
                normalizeOptionalText(
                        value
                );

        if (normalized == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private static <T> T requireValue(
            T value,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return value;
    }

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

    public enum RecommendedAction {
        ALLOW_WITH_MONITORING,
        REQUIRE_REVIEW,
        HOLD_PAYMENT_AND_REVIEW,
        BLOCK_AND_ESCALATE
    }

    public enum DetectionSource {
        SUPERVISED_MODEL,
        DETERMINISTIC_RULE,
        HYBRID
    }

    public enum ReviewStatus {
        PENDING_REVIEW,
        AUTO_CLEARED,
        CONFIRMED_FRAUD,
        FALSE_POSITIVE,
        ESCALATED
    }
}