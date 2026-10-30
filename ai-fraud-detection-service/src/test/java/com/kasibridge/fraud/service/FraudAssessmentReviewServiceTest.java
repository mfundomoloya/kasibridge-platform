package com.kasibridge.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.fraud.dto.FraudAssessmentResponse;
import com.kasibridge.fraud.dto.FraudAssessmentReviewRequest;
import com.kasibridge.fraud.entity.FraudAssessment;
import com.kasibridge.fraud.exception.FraudAssessmentNotFoundException;
import com.kasibridge.fraud.exception.FraudAssessmentVersionConflictException;
import com.kasibridge.fraud.repository.FraudAssessmentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudAssessmentReviewServiceTest {

    private static final String
            ASSESSMENT_REFERENCE =
            "KB-FA-20261025-92EDAC62";

    private static final String MODEL_SHA256 =
            "1a9dc135791a80c183b707de7686e84a"
                    + "87d6e3c15e7c6db0d1ed17ee1a7fe4de";

    private static final String SNAPSHOT_SHA256 =
            "de5f16f86c54d9c78d5ebb08e9064513"
                    + "aa00237f47d460294f78c4deb907bd56";

    private static final String REVIEWER =
            "analyst@kasibridge.local";

    @Mock
    private InvoiceFraudPredictionService
            predictionService;

    @Mock
    private FraudAssessmentRepository
            assessmentRepository;

    private FraudAssessmentService
            assessmentService;

    @BeforeEach
    void setUp() {
        assessmentService =
                new FraudAssessmentService(
                        predictionService,
                        assessmentRepository,
                        new ObjectMapper()
                );
    }

    @Test
    void confirmFraud_shouldPersistConfirmedDecision() {
        FraudAssessment assessment =
                createPersistedHighRiskAssessment(
                        0L
                );

        when(
                assessmentRepository
                        .findByAssessmentReference(
                                ASSESSMENT_REFERENCE
                        )
        ).thenReturn(
                Optional.of(assessment)
        );

        when(
                assessmentRepository.saveAndFlush(
                        assessment
                )
        ).thenAnswer(
                invocation -> {
                    FraudAssessment saved =
                            invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            saved,
                            "version",
                            1L
                    );

                    return saved;
                }
        );

        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Supplier documentation confirms "
                                + "fraudulent invoicing.",
                        0L
                );

        FraudAssessmentResponse response =
                assessmentService.confirmFraud(
                        ASSESSMENT_REFERENCE,
                        request,
                        REVIEWER
                );

        assertEquals(
                FraudAssessment.ReviewStatus
                        .CONFIRMED_FRAUD,
                response.reviewStatus()
        );

        assertEquals(
                REVIEWER,
                response.reviewedBy()
        );

        assertEquals(
                "Supplier documentation confirms "
                        + "fraudulent invoicing.",
                response.reviewNotes()
        );

        assertNotNull(
                response.reviewedAt()
        );

        assertEquals(
                1L,
                response.version()
        );

        verify(
                assessmentRepository
        ).saveAndFlush(
                assessment
        );
    }

    @Test
    void markFalsePositive_shouldPersistAnalystDecision() {
        FraudAssessment assessment =
                createPersistedHighRiskAssessment(
                        0L
                );

        when(
                assessmentRepository
                        .findByAssessmentReference(
                                ASSESSMENT_REFERENCE
                        )
        ).thenReturn(
                Optional.of(assessment)
        );

        when(
                assessmentRepository.saveAndFlush(
                        assessment
                )
        ).thenReturn(
                assessment
        );

        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Supporting documents confirm "
                                + "the invoice is valid.",
                        0L
                );

        FraudAssessmentResponse response =
                assessmentService
                        .markFalsePositive(
                                ASSESSMENT_REFERENCE,
                                request,
                                REVIEWER
                        );

        assertEquals(
                FraudAssessment.ReviewStatus
                        .FALSE_POSITIVE,
                response.reviewStatus()
        );

        assertEquals(
                REVIEWER,
                response.reviewedBy()
        );

        assertEquals(
                "Supporting documents confirm "
                        + "the invoice is valid.",
                response.reviewNotes()
        );

        assertNotNull(
                response.reviewedAt()
        );

        verify(
                assessmentRepository
        ).saveAndFlush(
                assessment
        );
    }

    @Test
    void escalate_shouldEscalatePendingAssessment() {
        FraudAssessment assessment =
                createPersistedHighRiskAssessment(
                        0L
                );

        when(
                assessmentRepository
                        .findByAssessmentReference(
                                ASSESSMENT_REFERENCE
                        )
        ).thenReturn(
                Optional.of(assessment)
        );

        when(
                assessmentRepository.saveAndFlush(
                        assessment
                )
        ).thenReturn(
                assessment
        );

        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "High-value invoice requires "
                                + "senior investigation.",
                        0L
                );

        FraudAssessmentResponse response =
                assessmentService.escalate(
                        ASSESSMENT_REFERENCE,
                        request,
                        REVIEWER
                );

        assertEquals(
                FraudAssessment.ReviewStatus
                        .ESCALATED,
                response.reviewStatus()
        );

        assertEquals(
                REVIEWER,
                response.reviewedBy()
        );

        assertNotNull(
                response.reviewedAt()
        );

        verify(
                assessmentRepository
        ).saveAndFlush(
                assessment
        );
    }

    @Test
    void escalate_shouldAllowAutoClearedAssessment() {
        FraudAssessment assessment =
                createPersistedLegitimateAssessment(
                        2L
                );

        when(
                assessmentRepository
                        .findByAssessmentReference(
                                ASSESSMENT_REFERENCE
                        )
        ).thenReturn(
                Optional.of(assessment)
        );

        when(
                assessmentRepository.saveAndFlush(
                        assessment
                )
        ).thenReturn(
                assessment
        );

        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "A deterministic fraud rule "
                                + "requires manual review.",
                        2L
                );

        FraudAssessmentResponse response =
                assessmentService.escalate(
                        ASSESSMENT_REFERENCE,
                        request,
                        "senior.analyst@kasibridge.local"
                );

        assertEquals(
                FraudAssessment.ReviewStatus
                        .ESCALATED,
                response.reviewStatus()
        );

        assertEquals(
                "senior.analyst@kasibridge.local",
                response.reviewedBy()
        );

        verify(
                assessmentRepository
        ).saveAndFlush(
                assessment
        );
    }

    @Test
    void confirmFraud_shouldRejectStaleVersion() {
        FraudAssessment assessment =
                createPersistedHighRiskAssessment(
                        3L
                );

        when(
                assessmentRepository
                        .findByAssessmentReference(
                                ASSESSMENT_REFERENCE
                        )
        ).thenReturn(
                Optional.of(assessment)
        );

        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Attempt to submit a stale "
                                + "review decision.",
                        2L
                );

        FraudAssessmentVersionConflictException
                exception =
                assertThrows(
                        FraudAssessmentVersionConflictException.class,
                        () ->
                                assessmentService
                                        .confirmFraud(
                                                ASSESSMENT_REFERENCE,
                                                request,
                                                REVIEWER
                                        )
                );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "Expected version 2"
                        )
        );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "current version is 3"
                        )
        );

        verify(
                assessmentRepository,
                never()
        ).saveAndFlush(
                any(FraudAssessment.class)
        );
    }

    @Test
    void confirmFraud_shouldRejectMissingAssessment() {
        when(
                assessmentRepository
                        .findByAssessmentReference(
                                "KB-FA-MISSING"
                        )
        ).thenReturn(
                Optional.empty()
        );

        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Assessment evidence "
                                + "was reviewed.",
                        0L
                );

        FraudAssessmentNotFoundException exception =
                assertThrows(
                        FraudAssessmentNotFoundException.class,
                        () ->
                                assessmentService
                                        .confirmFraud(
                                                "KB-FA-MISSING",
                                                request,
                                                REVIEWER
                                        )
                );

        assertEquals(
                "Fraud assessment not found: "
                        + "KB-FA-MISSING",
                exception.getMessage()
        );

        verify(
                assessmentRepository,
                never()
        ).saveAndFlush(
                any(FraudAssessment.class)
        );
    }

    @Test
    void falsePositive_shouldRejectFinalisedAssessment() {
        FraudAssessment assessment =
                createPersistedHighRiskAssessment(
                        0L
                );

        assessment.confirmFraud(
                REVIEWER,
                "Fraud was previously confirmed."
        );

        ReflectionTestUtils.setField(
                assessment,
                "version",
                1L
        );

        when(
                assessmentRepository
                        .findByAssessmentReference(
                                ASSESSMENT_REFERENCE
                        )
        ).thenReturn(
                Optional.of(assessment)
        );

        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Attempt to overwrite the "
                                + "completed decision.",
                        1L
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                assessmentService
                                        .markFalsePositive(
                                                ASSESSMENT_REFERENCE,
                                                request,
                                                "another.analyst"
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
                FraudAssessment.ReviewStatus
                        .CONFIRMED_FRAUD,
                assessment.getReviewStatus()
        );

        verify(
                assessmentRepository,
                never()
        ).saveAndFlush(
                any(FraudAssessment.class)
        );
    }

    @Test
    void confirmFraud_shouldRejectBlankReviewer() {
        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Review notes are available.",
                        0L
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                assessmentService
                                        .confirmFraud(
                                                ASSESSMENT_REFERENCE,
                                                request,
                                                "   "
                                        )
                );

        assertEquals(
                "Reviewer is required.",
                exception.getMessage()
        );

        verify(
                assessmentRepository,
                never()
        ).findByAssessmentReference(
                any()
        );
    }

    @Test
    void confirmFraud_shouldRejectNullRequest() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                assessmentService
                                        .confirmFraud(
                                                ASSESSMENT_REFERENCE,
                                                null,
                                                REVIEWER
                                        )
                );

        assertEquals(
                "Fraud assessment review request "
                        + "is required.",
                exception.getMessage()
        );

        verify(
                assessmentRepository,
                never()
        ).findByAssessmentReference(
                any()
        );
    }

    private FraudAssessment
    createPersistedHighRiskAssessment(
            Long version
    ) {
        FraudAssessment assessment =
                FraudAssessment.create(
                        "INV-LIVE-ASSESS-0002",
                        "SUPPLIER-DEMO-001",
                        "DPT-025",
                        new BigDecimal(
                                "20346.26"
                        ),
                        "ZA",
                        "Gauteng",
                        FraudAssessment.Classification
                                .HIGH_RISK,
                        FraudAssessment.RiskLevel
                                .HIGH,
                        new BigDecimal(
                                "0.857937"
                        ),
                        new BigDecimal(
                                "0.500000"
                        ),
                        FraudAssessment.RecommendedAction
                                .HOLD_PAYMENT_AND_REVIEW,
                        "Random Forest",
                        "procurement-fraud-v1",
                        "Hybrid full-signal",
                        FraudAssessment.DetectionSource
                                .SUPERVISED_MODEL,
                        MODEL_SHA256,
                        """
                        {
                          "invoiceReference":
                            "INV-LIVE-ASSESS-0002"
                        }
                        """,
                        SNAPSHOT_SHA256,
                        LocalDateTime.of(
                                2026,
                                10,
                                25,
                                22,
                                10,
                                47
                        )
                );

        preparePersistedAssessment(
                assessment,
                version
        );

        return assessment;
    }

    private FraudAssessment
    createPersistedLegitimateAssessment(
            Long version
    ) {
        FraudAssessment assessment =
                FraudAssessment.create(
                        "INV-LIVE-ASSESS-0003",
                        "SUPPLIER-DEMO-002",
                        "DPT-025",
                        new BigDecimal(
                                "2599.13"
                        ),
                        "TR",
                        "Gauteng",
                        FraudAssessment.Classification
                                .LEGITIMATE,
                        FraudAssessment.RiskLevel
                                .LOW,
                        new BigDecimal(
                                "0.068023"
                        ),
                        new BigDecimal(
                                "0.500000"
                        ),
                        FraudAssessment.RecommendedAction
                                .ALLOW_WITH_MONITORING,
                        "Random Forest",
                        "procurement-fraud-v1",
                        "Hybrid full-signal",
                        FraudAssessment.DetectionSource
                                .SUPERVISED_MODEL,
                        MODEL_SHA256,
                        """
                        {
                          "invoiceReference":
                            "INV-LIVE-ASSESS-0003"
                        }
                        """,
                        SNAPSHOT_SHA256,
                        LocalDateTime.of(
                                2026,
                                10,
                                25,
                                22,
                                12,
                                47
                        )
                );

        preparePersistedAssessment(
                assessment,
                version
        );

        return assessment;
    }

    private void preparePersistedAssessment(
            FraudAssessment assessment,
            Long version
    ) {
        ReflectionTestUtils.setField(
                assessment,
                "id",
                1L
        );

        ReflectionTestUtils.setField(
                assessment,
                "assessmentReference",
                ASSESSMENT_REFERENCE
        );

        ReflectionTestUtils.setField(
                assessment,
                "createdAt",
                LocalDateTime.of(
                        2026,
                        10,
                        25,
                        22,
                        10,
                        47
                )
        );

        ReflectionTestUtils.setField(
                assessment,
                "version",
                version
        );
    }
}