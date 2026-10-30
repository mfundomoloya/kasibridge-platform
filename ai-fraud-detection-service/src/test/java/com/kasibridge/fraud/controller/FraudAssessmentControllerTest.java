package com.kasibridge.fraud.controller;

import com.kasibridge.fraud.dto.FraudAssessmentResponse;
import com.kasibridge.fraud.dto.FraudAssessmentReviewRequest;
import com.kasibridge.fraud.entity.FraudAssessment;
import com.kasibridge.fraud.service.FraudAssessmentService;
import com.kasibridge.fraud.service.FraudDetectionService;
import com.kasibridge.fraud.service.InvoiceFraudPredictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.kasibridge.security.KasiBridgeUserPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class)
class FraudAssessmentControllerTest {

    @Mock
    private KasiBridgeUserPrincipal
            kasiBridgeUserPrincipal;


    private static final String
            ASSESSMENT_REFERENCE =
            "KB-FA-20261025-92EDAC62";

    private static final String INVOICE_REFERENCE =
            "INV-LIVE-ASSESS-0002";

    private static final String REVIEWER =
            "analyst@kasibridge.local";

    private static final String MODEL_SHA256 =
            "1a9dc135791a80c183b707de7686e84a"
                    + "87d6e3c15e7c6db0d1ed17ee1a7fe4de";

    private static final String SNAPSHOT_SHA256 =
            "de5f16f86c54d9c78d5ebb08e9064513"
                    + "aa00237f47d460294f78c4deb907bd56";

    @Mock
    private FraudDetectionService
            fraudDetectionService;

    @Mock
    private InvoiceFraudPredictionService
            invoiceFraudPredictionService;

    @Mock
    private FraudAssessmentService
            fraudAssessmentService;

    @Mock
    private Authentication authentication;

    private FraudController controller;

    @BeforeEach
    void setUp() {
        controller =
                new FraudController(
                        fraudDetectionService,
                        invoiceFraudPredictionService,
                        fraudAssessmentService
                );
    }

    @Test
    void getFraudAssessment_shouldReturnPersistedAssessment() {
        FraudAssessmentResponse assessment =
                createAssessmentResponse(
                        FraudAssessment.ReviewStatus
                                .PENDING_REVIEW,
                        null,
                        null,
                        0L
                );

        when(
                fraudAssessmentService
                        .getByAssessmentReference(
                                ASSESSMENT_REFERENCE
                        )
        ).thenReturn(
                assessment
        );

        ResponseEntity<FraudAssessmentResponse>
                response =
                controller.getFraudAssessment(
                        ASSESSMENT_REFERENCE
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                ASSESSMENT_REFERENCE,
                response.getBody()
                        .assessmentReference()
        );

        assertEquals(
                INVOICE_REFERENCE,
                response.getBody()
                        .invoiceReference()
        );

        verify(
                fraudAssessmentService
        ).getByAssessmentReference(
                ASSESSMENT_REFERENCE
        );
    }

    @Test
    void confirmAssessmentFraud_shouldUseAuthenticatedPrincipal() {
        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Supplier documentation confirms "
                                + "fraudulent invoicing.",
                        0L
                );

        FraudAssessmentResponse assessment =
                createAssessmentResponse(
                        FraudAssessment.ReviewStatus
                                .CONFIRMED_FRAUD,
                        REVIEWER,
                        request.reviewNotes(),
                        1L
                );

        configureAuthenticatedReviewer(
                REVIEWER
        );

        when(
                fraudAssessmentService.confirmFraud(
                        ASSESSMENT_REFERENCE,
                        request,
                        REVIEWER
                )
        ).thenReturn(
                assessment
        );

        ResponseEntity<FraudAssessmentResponse>
                response =
                controller.confirmAssessmentFraud(
                        ASSESSMENT_REFERENCE,
                        request,
                        authentication
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                FraudAssessment.ReviewStatus
                        .CONFIRMED_FRAUD,
                response.getBody()
                        .reviewStatus()
        );

        assertEquals(
                REVIEWER,
                response.getBody()
                        .reviewedBy()
        );

        assertEquals(
                1L,
                response.getBody()
                        .version()
        );

        verify(
                fraudAssessmentService
        ).confirmFraud(
                ASSESSMENT_REFERENCE,
                request,
                REVIEWER
        );
    }

    @Test
    void markAssessmentFalsePositive_shouldUseAuthenticatedPrincipal() {
        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Supporting documents confirm "
                                + "that the invoice is valid.",
                        0L
                );

        FraudAssessmentResponse assessment =
                createAssessmentResponse(
                        FraudAssessment.ReviewStatus
                                .FALSE_POSITIVE,
                        REVIEWER,
                        request.reviewNotes(),
                        1L
                );

        configureAuthenticatedReviewer(
                REVIEWER
        );

        when(
                fraudAssessmentService
                        .markFalsePositive(
                                ASSESSMENT_REFERENCE,
                                request,
                                REVIEWER
                        )
        ).thenReturn(
                assessment
        );

        ResponseEntity<FraudAssessmentResponse>
                response =
                controller.markAssessmentFalsePositive(
                        ASSESSMENT_REFERENCE,
                        request,
                        authentication
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                FraudAssessment.ReviewStatus
                        .FALSE_POSITIVE,
                response.getBody()
                        .reviewStatus()
        );

        assertEquals(
                REVIEWER,
                response.getBody()
                        .reviewedBy()
        );

        verify(
                fraudAssessmentService
        ).markFalsePositive(
                ASSESSMENT_REFERENCE,
                request,
                REVIEWER
        );
    }

    @Test
    void escalateAssessment_shouldUseAuthenticatedPrincipal() {
        String platformAdministrator =
                "platform.admin@kasibridge.local";

        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "High-value invoice requires "
                                + "senior investigation.",
                        0L
                );

        FraudAssessmentResponse assessment =
                createAssessmentResponse(
                        FraudAssessment.ReviewStatus
                                .ESCALATED,
                        platformAdministrator,
                        request.reviewNotes(),
                        1L
                );

        configureAuthenticatedReviewer(
                platformAdministrator
        );

        when(
                fraudAssessmentService.escalate(
                        ASSESSMENT_REFERENCE,
                        request,
                        platformAdministrator
                )
        ).thenReturn(
                assessment
        );

        ResponseEntity<FraudAssessmentResponse>
                response =
                controller.escalateAssessment(
                        ASSESSMENT_REFERENCE,
                        request,
                        authentication
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                FraudAssessment.ReviewStatus
                        .ESCALATED,
                response.getBody()
                        .reviewStatus()
        );

        assertEquals(
                platformAdministrator,
                response.getBody()
                        .reviewedBy()
        );

        verify(
                fraudAssessmentService
        ).escalate(
                ASSESSMENT_REFERENCE,
                request,
                platformAdministrator
        );
    }

    @Test
    void confirmAssessmentFraud_shouldTrimReviewerName() {
        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Evidence confirms fraudulent "
                                + "invoice activity.",
                        0L
                );

        FraudAssessmentResponse assessment =
                createAssessmentResponse(
                        FraudAssessment.ReviewStatus
                                .CONFIRMED_FRAUD,
                        REVIEWER,
                        request.reviewNotes(),
                        1L
                );

        when(
                authentication.isAuthenticated()
        ).thenReturn(
                true
        );

        when(
                authentication.getName()
        ).thenReturn(
                "  " + REVIEWER + "  "
        );

        when(
                fraudAssessmentService.confirmFraud(
                        ASSESSMENT_REFERENCE,
                        request,
                        REVIEWER
                )
        ).thenReturn(
                assessment
        );

        controller.confirmAssessmentFraud(
                ASSESSMENT_REFERENCE,
                request,
                authentication
        );

        verify(
                fraudAssessmentService
        ).confirmFraud(
                ASSESSMENT_REFERENCE,
                request,
                REVIEWER
        );
    }

    @Test
    void confirmAssessmentFraud_shouldRejectNullAuthentication() {
        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Assessment evidence was reviewed.",
                        0L
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                controller
                                        .confirmAssessmentFraud(
                                                ASSESSMENT_REFERENCE,
                                                request,
                                                null
                                        )
                );

        assertEquals(
                "An authenticated reviewer "
                        + "is required.",
                exception.getMessage()
        );

        verify(
                fraudAssessmentService,
                never()
        ).confirmFraud(
                ASSESSMENT_REFERENCE,
                request,
                REVIEWER
        );
    }

    @Test
    void confirmAssessmentFraud_shouldRejectUnauthenticatedPrincipal() {
        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Assessment evidence was reviewed.",
                        0L
                );

        when(
                authentication.isAuthenticated()
        ).thenReturn(
                false
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                controller
                                        .confirmAssessmentFraud(
                                                ASSESSMENT_REFERENCE,
                                                request,
                                                authentication
                                        )
                );

        assertEquals(
                "An authenticated reviewer "
                        + "is required.",
                exception.getMessage()
        );

        verify(
                fraudAssessmentService,
                never()
        ).confirmFraud(
                ASSESSMENT_REFERENCE,
                request,
                REVIEWER
        );
    }

    @Test
    void confirmAssessmentFraud_shouldRejectAnonymousPrincipal() {
        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Assessment evidence was reviewed.",
                        0L
                );

        when(
                authentication.isAuthenticated()
        ).thenReturn(
                true
        );

        when(
                authentication.getName()
        ).thenReturn(
                "anonymousUser"
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                controller
                                        .confirmAssessmentFraud(
                                                ASSESSMENT_REFERENCE,
                                                request,
                                                authentication
                                        )
                );

        assertEquals(
                "An authenticated reviewer "
                        + "is required.",
                exception.getMessage()
        );

        verify(
                fraudAssessmentService,
                never()
        ).confirmFraud(
                ASSESSMENT_REFERENCE,
                request,
                REVIEWER
        );
    }

    @Test
    void confirmAssessmentFraud_shouldUseCustomPrincipalUsername() {
        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Evidence confirms fraudulent "
                                + "invoice activity.",
                        0L
                );

        FraudAssessmentResponse assessment =
                createAssessmentResponse(
                        FraudAssessment.ReviewStatus
                                .CONFIRMED_FRAUD,
                        "analyst",
                        request.reviewNotes(),
                        1L
                );

        when(
                authentication.isAuthenticated()
        ).thenReturn(
                true
        );

        when(
                authentication.getPrincipal()
        ).thenReturn(
                kasiBridgeUserPrincipal
        );

        when(
                kasiBridgeUserPrincipal.username()
        ).thenReturn(
                "analyst"
        );

        when(
                fraudAssessmentService.confirmFraud(
                        ASSESSMENT_REFERENCE,
                        request,
                        "analyst"
                )
        ).thenReturn(
                assessment
        );

        ResponseEntity<FraudAssessmentResponse>
                response =
                controller.confirmAssessmentFraud(
                        ASSESSMENT_REFERENCE,
                        request,
                        authentication
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                "analyst",
                response.getBody()
                        .reviewedBy()
        );

        verify(
                fraudAssessmentService
        ).confirmFraud(
                ASSESSMENT_REFERENCE,
                request,
                "analyst"
        );

        verify(
                authentication,
                never()
        ).getName();
    }

    @Test
    void confirmAssessmentFraud_shouldRejectPrincipalWithoutUsername() {
        FraudAssessmentReviewRequest request =
                new FraudAssessmentReviewRequest(
                        "Assessment evidence was reviewed.",
                        0L
                );

        when(
                authentication.isAuthenticated()
        ).thenReturn(
                true
        );

        when(
                authentication.getPrincipal()
        ).thenReturn(
                kasiBridgeUserPrincipal
        );

        when(
                kasiBridgeUserPrincipal.username()
        ).thenReturn(
                "   "
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                controller
                                        .confirmAssessmentFraud(
                                                ASSESSMENT_REFERENCE,
                                                request,
                                                authentication
                                        )
                );

        assertEquals(
                "The authenticated reviewer "
                        + "has no username.",
                exception.getMessage()
        );

        verify(
                fraudAssessmentService,
                never()
        ).confirmFraud(
                any(),
                any(),
                any()
        );

        verify(
                authentication,
                never()
        ).getName();
    }



    private void configureAuthenticatedReviewer(
            String reviewer
    ) {
        when(
                authentication.isAuthenticated()
        ).thenReturn(
                true
        );

        when(
                authentication.getName()
        ).thenReturn(
                reviewer
        );
    }

    private FraudAssessmentResponse
    createAssessmentResponse(
            FraudAssessment.ReviewStatus reviewStatus,
            String reviewedBy,
            String reviewNotes,
            Long version
    ) {
        LocalDateTime assessedAt =
                LocalDateTime.of(
                        2026,
                        10,
                        25,
                        22,
                        10,
                        47
                );

        LocalDateTime reviewedAt =
                reviewedBy == null
                        ? null
                        : LocalDateTime.of(
                        2026,
                        10,
                        30,
                        1,
                        15,
                        0
                );

        return new FraudAssessmentResponse(
                1L,
                ASSESSMENT_REFERENCE,
                INVOICE_REFERENCE,
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
                SNAPSHOT_SHA256,
                reviewStatus,
                reviewedBy,
                reviewNotes,
                reviewedAt,
                assessedAt,
                assessedAt.plusNanos(
                        124_825_445
                ),
                reviewedAt,
                version
        );
    }
}