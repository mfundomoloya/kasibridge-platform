package com.kasibridge.fraud.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FraudAssessmentTest {

    private static final String MODEL_SHA256 =
            "1a9dc135791a80c183b707de7686e84a"
                    + "87d6e3c15e7c6db0d1ed17ee1a7fe4de";

    private static final String
            FEATURE_SNAPSHOT_SHA256 =
            "9b1d3a0671c0170f4f7323e60f829b4d"
                    + "611e0d1d159128b4512b0d3df6317638";

    @Test
    void create_shouldDefaultHighRiskAssessmentToPendingReview() {
        FraudAssessment assessment =
                createHighRiskAssessment();

        assertEquals(
                FraudAssessment
                        .Classification
                        .HIGH_RISK,
                assessment.getClassification()
        );

        assertEquals(
                FraudAssessment
                        .RiskLevel
                        .HIGH,
                assessment.getRiskLevel()
        );

        assertEquals(
                FraudAssessment
                        .ReviewStatus
                        .PENDING_REVIEW,
                assessment.getReviewStatus()
        );

        assertEquals(
                FraudAssessment
                        .RecommendedAction
                        .HOLD_PAYMENT_AND_REVIEW,
                assessment.getRecommendedAction()
        );

        assertNull(
                assessment.getReviewedBy()
        );

        assertNull(
                assessment.getReviewedAt()
        );
    }

    @Test
    void create_shouldDefaultLegitimateAssessmentToAutoCleared() {
        FraudAssessment assessment =
                createLegitimateAssessment();

        assertEquals(
                FraudAssessment
                        .Classification
                        .LEGITIMATE,
                assessment.getClassification()
        );

        assertEquals(
                FraudAssessment
                        .RiskLevel
                        .LOW,
                assessment.getRiskLevel()
        );

        assertEquals(
                FraudAssessment
                        .ReviewStatus
                        .AUTO_CLEARED,
                assessment.getReviewStatus()
        );

        assertEquals(
                FraudAssessment
                        .RecommendedAction
                        .ALLOW_WITH_MONITORING,
                assessment.getRecommendedAction()
        );
    }

    @Test
    void onCreate_shouldGenerateReferenceAndTimestamps() {
        FraudAssessment assessment =
                createHighRiskAssessment();

        assertNull(
                assessment.getAssessmentReference()
        );

        assertNull(
                assessment.getCreatedAt()
        );

        assessment.onCreate();

        assertNotNull(
                assessment.getAssessmentReference()
        );

        assertTrue(
                assessment
                        .getAssessmentReference()
                        .matches(
                                "^KB-FA-\\d{8}-[A-F0-9]{8}$"
                        )
        );

        assertNotNull(
                assessment.getAssessedAt()
        );

        assertNotNull(
                assessment.getCreatedAt()
        );
    }

    @Test
    void create_shouldNormaliseTextCountryAndChecksums() {
        FraudAssessment assessment =
                FraudAssessment.create(
                        "  INV-LIVE-0001  ",
                        "  SUPPLIER-001  ",
                        "  DPT-025  ",
                        new BigDecimal(
                                "20346.26"
                        ),
                        " za ",
                        " Gauteng ",
                        FraudAssessment
                                .Classification
                                .HIGH_RISK,
                        FraudAssessment
                                .RiskLevel
                                .HIGH,
                        new BigDecimal(
                                "0.857937"
                        ),
                        new BigDecimal(
                                "0.500000"
                        ),
                        FraudAssessment
                                .RecommendedAction
                                .HOLD_PAYMENT_AND_REVIEW,
                        " Random Forest ",
                        " procurement-fraud-v1 ",
                        " Hybrid full-signal ",
                        FraudAssessment
                                .DetectionSource
                                .SUPERVISED_MODEL,
                        MODEL_SHA256
                                .toUpperCase(),
                        """
                        {
                          "invoiceReference": "INV-LIVE-0001"
                        }
                        """,
                        FEATURE_SNAPSHOT_SHA256
                                .toUpperCase(),
                        LocalDateTime.of(
                                2026,
                                10,
                                24,
                                22,
                                41,
                                30
                        )
                );

        assertEquals(
                "INV-LIVE-0001",
                assessment.getInvoiceReference()
        );

        assertEquals(
                "SUPPLIER-001",
                assessment.getSupplierId()
        );

        assertEquals(
                "DPT-025",
                assessment.getDepartmentId()
        );

        assertEquals(
                "ZA",
                assessment.getSupplierCountry()
        );

        assertEquals(
                "Gauteng",
                assessment.getRegion()
        );

        assertEquals(
                "Random Forest",
                assessment.getModelName()
        );

        assertEquals(
                "procurement-fraud-v1",
                assessment.getModelVersion()
        );

        assertEquals(
                MODEL_SHA256,
                assessment.getModelSha256()
        );

        assertEquals(
                FEATURE_SNAPSHOT_SHA256,
                assessment
                        .getFeatureSnapshotSha256()
        );
    }

    @Test
    void create_shouldRejectRiskProbabilityAboveOne() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                FraudAssessment.create(
                                        "INV-LIVE-0001",
                                        "SUPPLIER-001",
                                        "DPT-025",
                                        new BigDecimal(
                                                "20346.26"
                                        ),
                                        "ZA",
                                        "Gauteng",
                                        FraudAssessment
                                                .Classification
                                                .HIGH_RISK,
                                        FraudAssessment
                                                .RiskLevel
                                                .CRITICAL,
                                        new BigDecimal(
                                                "1.250000"
                                        ),
                                        new BigDecimal(
                                                "0.500000"
                                        ),
                                        FraudAssessment
                                                .RecommendedAction
                                                .BLOCK_AND_ESCALATE,
                                        "Random Forest",
                                        "procurement-fraud-v1",
                                        "Hybrid full-signal",
                                        FraudAssessment
                                                .DetectionSource
                                                .SUPERVISED_MODEL,
                                        MODEL_SHA256,
                                        "{}",
                                        FEATURE_SNAPSHOT_SHA256,
                                        LocalDateTime.now()
                                )
                );

        assertEquals(
                "Risk probability must be "
                        + "between 0 and 1.",
                exception.getMessage()
        );
    }

    @Test
    void create_shouldRejectInvalidModelChecksum() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                FraudAssessment.create(
                                        "INV-LIVE-0001",
                                        "SUPPLIER-001",
                                        "DPT-025",
                                        new BigDecimal(
                                                "20346.26"
                                        ),
                                        "ZA",
                                        "Gauteng",
                                        FraudAssessment
                                                .Classification
                                                .HIGH_RISK,
                                        FraudAssessment
                                                .RiskLevel
                                                .HIGH,
                                        new BigDecimal(
                                                "0.857937"
                                        ),
                                        new BigDecimal(
                                                "0.500000"
                                        ),
                                        FraudAssessment
                                                .RecommendedAction
                                                .HOLD_PAYMENT_AND_REVIEW,
                                        "Random Forest",
                                        "procurement-fraud-v1",
                                        "Hybrid full-signal",
                                        FraudAssessment
                                                .DetectionSource
                                                .SUPERVISED_MODEL,
                                        "not-a-valid-checksum",
                                        "{}",
                                        FEATURE_SNAPSHOT_SHA256,
                                        LocalDateTime.now()
                                )
                );

        assertEquals(
                "Model SHA-256 must contain "
                        + "exactly 64 hexadecimal "
                        + "characters.",
                exception.getMessage()
        );
    }

    @Test
    void confirmFraud_shouldRecordReviewDetails() {
        FraudAssessment assessment =
                createHighRiskAssessment();

        assessment.confirmFraud(
                "analyst@kasibridge.local",
                "Invoice evidence confirmed."
        );

        assertEquals(
                FraudAssessment
                        .ReviewStatus
                        .CONFIRMED_FRAUD,
                assessment.getReviewStatus()
        );

        assertEquals(
                "analyst@kasibridge.local",
                assessment.getReviewedBy()
        );

        assertEquals(
                "Invoice evidence confirmed.",
                assessment.getReviewNotes()
        );

        assertNotNull(
                assessment.getReviewedAt()
        );
    }

    @Test
    void markFalsePositive_shouldRecordReviewDecision() {
        FraudAssessment assessment =
                createHighRiskAssessment();

        assessment.markFalsePositive(
                "analyst@kasibridge.local",
                "Supporting documentation verified."
        );

        assertEquals(
                FraudAssessment
                        .ReviewStatus
                        .FALSE_POSITIVE,
                assessment.getReviewStatus()
        );

        assertEquals(
                "analyst@kasibridge.local",
                assessment.getReviewedBy()
        );

        assertNotNull(
                assessment.getReviewedAt()
        );
    }

    @Test
    void escalate_shouldAllowAutoClearedAssessmentToBeReviewed() {
        FraudAssessment assessment =
                createLegitimateAssessment();

        assessment.escalate(
                "senior.analyst@kasibridge.local",
                "Manual rule evidence requires review."
        );

        assertEquals(
                FraudAssessment
                        .ReviewStatus
                        .ESCALATED,
                assessment.getReviewStatus()
        );

        assertEquals(
                "senior.analyst@kasibridge.local",
                assessment.getReviewedBy()
        );

        assertNotNull(
                assessment.getReviewedAt()
        );
    }

    @Test
    void confirmedAssessment_shouldRejectSecondReviewDecision() {
        FraudAssessment assessment =
                createHighRiskAssessment();

        assessment.confirmFraud(
                "analyst@kasibridge.local",
                "Confirmed."
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                assessment
                                        .markFalsePositive(
                                                "other-analyst",
                                                "Attempted overwrite."
                                        )
                );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "CONFIRMED_FRAUD"
                        )
        );

        assertEquals(
                FraudAssessment
                        .ReviewStatus
                        .CONFIRMED_FRAUD,
                assessment.getReviewStatus()
        );
    }

    @Test
    void onUpdate_shouldSetUpdatedTimestamp() {
        FraudAssessment assessment =
                createHighRiskAssessment();

        assertNull(
                assessment.getUpdatedAt()
        );

        assessment.onUpdate();

        assertNotNull(
                assessment.getUpdatedAt()
        );
    }

    @Test
    void create_shouldAllowMissingOptionalSupplierAndDepartment() {
        FraudAssessment assessment =
                FraudAssessment.create(
                        "INV-AUTO-001",
                        null,
                        null,
                        new BigDecimal(
                                "2599.13"
                        ),
                        "TR",
                        "Gauteng",
                        FraudAssessment
                                .Classification
                                .LEGITIMATE,
                        FraudAssessment
                                .RiskLevel
                                .LOW,
                        new BigDecimal(
                                "0.068023"
                        ),
                        new BigDecimal(
                                "0.500000"
                        ),
                        FraudAssessment
                                .RecommendedAction
                                .ALLOW_WITH_MONITORING,
                        "Random Forest",
                        "procurement-fraud-v1",
                        "Hybrid full-signal",
                        FraudAssessment
                                .DetectionSource
                                .SUPERVISED_MODEL,
                        MODEL_SHA256,
                        "{}",
                        FEATURE_SNAPSHOT_SHA256,
                        LocalDateTime.now()
                );

        assertNull(
                assessment.getSupplierId()
        );

        assertNull(
                assessment.getDepartmentId()
        );
    }

    private FraudAssessment
    createHighRiskAssessment() {
        return FraudAssessment.create(
                "INV-LIVE-0001",
                "SUPPLIER-DEMO-001",
                "DPT-025",
                new BigDecimal(
                        "20346.26"
                ),
                "ZA",
                "Gauteng",
                FraudAssessment
                        .Classification
                        .HIGH_RISK,
                FraudAssessment
                        .RiskLevel
                        .HIGH,
                new BigDecimal(
                        "0.857937"
                ),
                new BigDecimal(
                        "0.500000"
                ),
                FraudAssessment
                        .RecommendedAction
                        .HOLD_PAYMENT_AND_REVIEW,
                "Random Forest",
                "procurement-fraud-v1",
                "Hybrid full-signal",
                FraudAssessment
                        .DetectionSource
                        .SUPERVISED_MODEL,
                MODEL_SHA256,
                """
                {
                  "invoiceReference": "INV-LIVE-0001"
                }
                """,
                FEATURE_SNAPSHOT_SHA256,
                LocalDateTime.of(
                        2026,
                        10,
                        24,
                        22,
                        41,
                        30
                )
        );
    }

    private FraudAssessment
    createLegitimateAssessment() {
        return FraudAssessment.create(
                "INV-LIVE-0002",
                "SUPPLIER-DEMO-002",
                "DPT-025",
                new BigDecimal(
                        "2599.13"
                ),
                "TR",
                "Gauteng",
                FraudAssessment
                        .Classification
                        .LEGITIMATE,
                FraudAssessment
                        .RiskLevel
                        .LOW,
                new BigDecimal(
                        "0.068023"
                ),
                new BigDecimal(
                        "0.500000"
                ),
                FraudAssessment
                        .RecommendedAction
                        .ALLOW_WITH_MONITORING,
                "Random Forest",
                "procurement-fraud-v1",
                "Hybrid full-signal",
                FraudAssessment
                        .DetectionSource
                        .SUPERVISED_MODEL,
                MODEL_SHA256,
                """
                {
                  "invoiceReference": "INV-LIVE-0002"
                }
                """,
                FEATURE_SNAPSHOT_SHA256,
                LocalDateTime.of(
                        2026,
                        10,
                        24,
                        22,
                        42,
                        30
                )
        );
    }
}