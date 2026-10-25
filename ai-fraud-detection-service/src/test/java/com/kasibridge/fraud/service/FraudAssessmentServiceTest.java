package com.kasibridge.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.fraud.dto.FraudAssessmentResponse;
import com.kasibridge.fraud.dto.FraudAssessmentResult;
import com.kasibridge.fraud.dto.InvoiceFraudPredictionRequest;
import com.kasibridge.fraud.dto.InvoiceFraudPredictionResponse;
import com.kasibridge.fraud.entity.FraudAssessment;
import com.kasibridge.fraud.exception.FraudAssessmentConflictException;
import com.kasibridge.fraud.exception.FraudAssessmentNotFoundException;
import com.kasibridge.fraud.repository.FraudAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudAssessmentServiceTest {

    private static final String MODEL_SHA256 =
            "1a9dc135791a80c183b707de7686e84a"
                    + "87d6e3c15e7c6db0d1ed17ee1a7fe4de";

    private static final String MODEL_VERSION =
            "procurement-fraud-v1";

    @Mock
    private InvoiceFraudPredictionService
            predictionService;

    @Mock
    private FraudAssessmentRepository
            assessmentRepository;

    private ObjectMapper objectMapper;

    private FraudAssessmentService
            assessmentService;

    @BeforeEach
    void setUp() {
        objectMapper =
                new ObjectMapper();

        assessmentService =
                new FraudAssessmentService(
                        predictionService,
                        assessmentRepository,
                        objectMapper
                );
    }

    @Test
    void assess_shouldCreatePersistedAssessment()
            throws Exception {
        InvoiceFraudPredictionRequest request =
                createRequest(
                        "INV-LIVE-0001"
                );

        InvoiceFraudPredictionResponse prediction =
                createHighRiskPrediction();

        when(
                predictionService.predict(
                        request
                )
        ).thenReturn(
                prediction
        );

        when(
                assessmentRepository
                        .findByInvoiceReferenceAndModelVersion(
                                "INV-LIVE-0001",
                                MODEL_VERSION
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                assessmentRepository.save(
                        any(FraudAssessment.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        FraudAssessmentResult result =
                assessmentService.assess(
                        request
                );

        assertTrue(
                result.created()
        );

        assertNotNull(
                result.assessment()
        );

        assertEquals(
                "INV-LIVE-0001",
                result.assessment()
                        .invoiceReference()
        );

        assertEquals(
                "SUPPLIER-DEMO-001",
                result.assessment()
                        .supplierId()
        );

        assertEquals(
                "DPT-025",
                result.assessment()
                        .departmentId()
        );

        assertEquals(
                FraudAssessment
                        .Classification
                        .HIGH_RISK,
                result.assessment()
                        .classification()
        );

        assertEquals(
                FraudAssessment
                        .RiskLevel
                        .HIGH,
                result.assessment()
                        .riskLevel()
        );

        assertEquals(
                FraudAssessment
                        .ReviewStatus
                        .PENDING_REVIEW,
                result.assessment()
                        .reviewStatus()
        );

        assertEquals(
                FraudAssessment
                        .RecommendedAction
                        .HOLD_PAYMENT_AND_REVIEW,
                result.assessment()
                        .recommendedAction()
        );

        assertEquals(
                MODEL_VERSION,
                result.assessment()
                        .modelVersion()
        );

        assertEquals(
                MODEL_SHA256,
                result.assessment()
                        .modelSha256()
        );

        verify(
                predictionService
        ).predict(
                request
        );

        verify(
                assessmentRepository
        ).save(
                any(FraudAssessment.class)
        );
    }

    @Test
    void assess_shouldStoreExactFeatureSnapshotAndSha256()
            throws Exception {
        InvoiceFraudPredictionRequest request =
                createRequest(
                        "INV-LIVE-0001"
                );

        when(
                predictionService.predict(
                        request
                )
        ).thenReturn(
                createHighRiskPrediction()
        );

        when(
                assessmentRepository
                        .findByInvoiceReferenceAndModelVersion(
                                "INV-LIVE-0001",
                                MODEL_VERSION
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                assessmentRepository.save(
                        any(FraudAssessment.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        assessmentService.assess(
                request
        );

        ArgumentCaptor<FraudAssessment> captor =
                ArgumentCaptor.forClass(
                        FraudAssessment.class
                );

        verify(
                assessmentRepository
        ).save(
                captor.capture()
        );

        FraudAssessment savedAssessment =
                captor.getValue();

        String expectedJson =
                objectMapper.writeValueAsString(
                        request
                );

        String expectedSha256 =
                calculateSha256(
                        expectedJson
                );

        assertEquals(
                expectedJson,
                savedAssessment
                        .getFeatureSnapshotJson()
        );

        assertEquals(
                expectedSha256,
                savedAssessment
                        .getFeatureSnapshotSha256()
        );

        assertTrue(
                savedAssessment
                        .getFeatureSnapshotJson()
                        .contains(
                                "\"invoiceReference\":"
                                        + "\"INV-LIVE-0001\""
                        )
        );

        assertTrue(
                savedAssessment
                        .getFeatureSnapshotJson()
                        .contains(
                                "\"splitInvoiceFlag\":1"
                        )
        );

        assertTrue(
                savedAssessment
                        .getFeatureSnapshotJson()
                        .contains(
                                "\"supplierRiskScore\":0.82"
                        )
        );
    }

    @Test
    void assess_shouldReturnExistingAssessmentForIdenticalRequest()
            throws Exception {
        InvoiceFraudPredictionRequest request =
                createRequest(
                        "INV-LIVE-0001"
                );

        InvoiceFraudPredictionResponse prediction =
                createHighRiskPrediction();

        String snapshotJson =
                objectMapper.writeValueAsString(
                        request
                );

        String snapshotSha256 =
                calculateSha256(
                        snapshotJson
                );

        FraudAssessment existingAssessment =
                createExistingAssessment(
                        "INV-LIVE-0001",
                        snapshotJson,
                        snapshotSha256
                );

        when(
                predictionService.predict(
                        request
                )
        ).thenReturn(
                prediction
        );

        when(
                assessmentRepository
                        .findByInvoiceReferenceAndModelVersion(
                                "INV-LIVE-0001",
                                MODEL_VERSION
                        )
        ).thenReturn(
                Optional.of(
                        existingAssessment
                )
        );

        FraudAssessmentResult result =
                assessmentService.assess(
                        request
                );

        assertFalse(
                result.created()
        );

        assertEquals(
                "INV-LIVE-0001",
                result.assessment()
                        .invoiceReference()
        );

        assertEquals(
                MODEL_VERSION,
                result.assessment()
                        .modelVersion()
        );

        assertEquals(
                snapshotSha256,
                result.assessment()
                        .featureSnapshotSha256()
        );

        verify(
                assessmentRepository,
                never()
        ).save(
                any(FraudAssessment.class)
        );
    }

    @Test
    void assess_shouldRejectChangedFeaturesForExistingInvoice()
            throws Exception {
        InvoiceFraudPredictionRequest request =
                createRequest(
                        "INV-LIVE-0001"
                );

        String existingSnapshot =
                """
                {
                  "invoiceReference": "INV-LIVE-0001",
                  "invoiceAmount": 1000.00
                }
                """;

        FraudAssessment existingAssessment =
                createExistingAssessment(
                        "INV-LIVE-0001",
                        existingSnapshot,
                        calculateSha256(
                                existingSnapshot
                        )
                );

        when(
                predictionService.predict(
                        request
                )
        ).thenReturn(
                createHighRiskPrediction()
        );

        when(
                assessmentRepository
                        .findByInvoiceReferenceAndModelVersion(
                                "INV-LIVE-0001",
                                MODEL_VERSION
                        )
        ).thenReturn(
                Optional.of(
                        existingAssessment
                )
        );

        FraudAssessmentConflictException exception =
                assertThrows(
                        FraudAssessmentConflictException.class,
                        () ->
                                assessmentService.assess(
                                        request
                                )
                );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "different feature snapshot"
                        )
        );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "INV-LIVE-0001"
                        )
        );

        verify(
                assessmentRepository,
                never()
        ).save(
                any(FraudAssessment.class)
        );
    }

    @Test
    void assess_shouldGenerateInvoiceReferenceWhenMissing() {
        InvoiceFraudPredictionRequest request =
                createRequest(
                        null
                );

        when(
                predictionService.predict(
                        request
                )
        ).thenReturn(
                createHighRiskPrediction()
        );

        when(
                assessmentRepository
                        .findByInvoiceReferenceAndModelVersion(
                                anyString(),
                                anyString()
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                assessmentRepository.save(
                        any(FraudAssessment.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        FraudAssessmentResult result =
                assessmentService.assess(
                        request
                );

        assertTrue(
                result.created()
        );

        assertNotNull(
                result.assessment()
                        .invoiceReference()
        );

        assertTrue(
                result.assessment()
                        .invoiceReference()
                        .matches(
                                "^KB-INV-AUTO-[A-F0-9]{8}$"
                        )
        );

        /*
         * The snapshot reflects the exact incoming
         * request. The generated reference is stored
         * separately on the assessment entity.
         */
        ArgumentCaptor<FraudAssessment> captor =
                ArgumentCaptor.forClass(
                        FraudAssessment.class
                );

        verify(
                assessmentRepository
        ).save(
                captor.capture()
        );

        assertTrue(
                captor.getValue()
                        .getFeatureSnapshotJson()
                        .contains(
                                "\"invoiceReference\":null"
                        )
        );
    }

    @Test
    void assess_shouldAllowMissingSupplierAndDepartment() {
        InvoiceFraudPredictionRequest request =
                createRequest(
                        "INV-LIVE-0003"
                );

        request.setSupplierId(
                null
        );

        request.setDepartmentId(
                null
        );

        when(
                predictionService.predict(
                        request
                )
        ).thenReturn(
                createLegitimatePrediction()
        );

        when(
                assessmentRepository
                        .findByInvoiceReferenceAndModelVersion(
                                "INV-LIVE-0003",
                                MODEL_VERSION
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                assessmentRepository.save(
                        any(FraudAssessment.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        FraudAssessmentResult result =
                assessmentService.assess(
                        request
                );

        assertTrue(
                result.created()
        );

        assertNull(
                result.assessment()
                        .supplierId()
        );

        assertNull(
                result.assessment()
                        .departmentId()
        );

        assertEquals(
                FraudAssessment
                        .ReviewStatus
                        .AUTO_CLEARED,
                result.assessment()
                        .reviewStatus()
        );
    }

    @Test
    void getByAssessmentReference_shouldReturnAssessment() {
        FraudAssessment existingAssessment =
                createExistingAssessment(
                        "INV-LIVE-0001",
                        "{}",
                        calculateSha256("{}")
                );

        when(
                assessmentRepository
                        .findByAssessmentReference(
                                "KB-FA-20261024-A1B2C3D4"
                        )
        ).thenReturn(
                Optional.of(
                        existingAssessment
                )
        );

        FraudAssessmentResponse response =
                assessmentService
                        .getByAssessmentReference(
                                "  KB-FA-20261024-A1B2C3D4  "
                        );

        assertEquals(
                "INV-LIVE-0001",
                response.invoiceReference()
        );

        assertEquals(
                MODEL_VERSION,
                response.modelVersion()
        );
    }

    @Test
    void getByAssessmentReference_shouldRejectMissingAssessment() {
        when(
                assessmentRepository
                        .findByAssessmentReference(
                                "KB-FA-MISSING"
                        )
        ).thenReturn(
                Optional.empty()
        );

        FraudAssessmentNotFoundException exception =
                assertThrows(
                        FraudAssessmentNotFoundException.class,
                        () ->
                                assessmentService
                                        .getByAssessmentReference(
                                                "KB-FA-MISSING"
                                        )
                );

        assertEquals(
                "Fraud assessment not found: "
                        + "KB-FA-MISSING",
                exception.getMessage()
        );
    }

    @Test
    void assess_shouldRejectNullRequest() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                assessmentService.assess(
                                        null
                                )
                );

        assertEquals(
                "Invoice fraud prediction request "
                        + "is required.",
                exception.getMessage()
        );

        verify(
                predictionService,
                never()
        ).predict(
                any()
        );

        verify(
                assessmentRepository,
                never()
        ).save(
                any()
        );
    }

    private InvoiceFraudPredictionRequest
    createRequest(
            String invoiceReference
    ) {
        InvoiceFraudPredictionRequest request =
                new InvoiceFraudPredictionRequest();

        request.setInvoiceReference(
                invoiceReference
        );

        request.setSupplierId(
                "SUPPLIER-DEMO-001"
        );

        request.setDepartmentId(
                "DPT-025"
        );

        request.setInvoiceAmount(
                new BigDecimal(
                        "20346.26"
                )
        );

        request.setSubmissionHour(
                23
        );

        request.setSupplierInvoiceCount30d(
                new BigDecimal(
                        "18"
                )
        );

        request.setSupplierAvgAmount90d(
                new BigDecimal(
                        "4252.16"
                )
        );

        request.setInvoiceAmountZscore(
                new BigDecimal(
                        "3.4"
                )
        );

        request.setSupplierAgeDays(
                61
        );

        request.setAvgInvoiceAmount(
                new BigDecimal(
                        "5590.25"
                )
        );

        request.setAnnualBudget(
                new BigDecimal(
                        "50000000"
                )
        );

        request.setOcrTotalExtracted(
                new BigDecimal(
                        "20346.26"
                )
        );

        request.setInvoiceMonth(
                10
        );

        request.setInvoiceDayOfWeek(
                5
        );

        request.setInvoiceDayOfMonth(
                24
        );

        request.setInvoiceIsWeekend(
                1
        );

        request.setHasInvoiceImage(
                1
        );

        request.setHasOcrResult(
                1
        );

        request.setOcrAmountDifference(
                BigDecimal.ZERO
        );

        request.setOcrAmountDifferenceAbs(
                BigDecimal.ZERO
        );

        request.setOcrAmountDifferenceRatio(
                BigDecimal.ZERO
        );

        request.setDuplicateInvoiceFlag(
                0
        );

        request.setSplitInvoiceFlag(
                1
        );

        request.setLateNightSubmissionFlag(
                1
        );

        request.setSupplierRiskScore(
                new BigDecimal(
                        "0.82"
                )
        );

        request.setBlacklistedFlag(
                0
        );

        request.setImageTamperFlag(
                0
        );

        request.setPaymentTerms(
                "NET30"
        );

        request.setInvoiceType(
                "SERVICES"
        );

        request.setSupplierCountry(
                "ZA"
        );

        request.setRegion(
                "Gauteng"
        );

        return request;
    }

    private InvoiceFraudPredictionResponse
    createHighRiskPrediction() {
        return new InvoiceFraudPredictionResponse(
                InvoiceFraudPredictionResponse
                        .Classification
                        .HIGH_RISK,
                InvoiceFraudPredictionResponse
                        .RiskLevel
                        .HIGH,
                new BigDecimal(
                        "0.857937"
                ),
                "Random Forest",
                MODEL_VERSION,
                "Hybrid full-signal",
                InvoiceFraudPredictionResponse
                        .DetectionSource
                        .SUPERVISED_MODEL,
                InvoiceFraudPredictionResponse
                        .RecommendedAction
                        .HOLD_PAYMENT_AND_REVIEW,
                new BigDecimal(
                        "0.500000"
                ),
                MODEL_SHA256,
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

    private InvoiceFraudPredictionResponse
    createLegitimatePrediction() {
        return new InvoiceFraudPredictionResponse(
                InvoiceFraudPredictionResponse
                        .Classification
                        .LEGITIMATE,
                InvoiceFraudPredictionResponse
                        .RiskLevel
                        .LOW,
                new BigDecimal(
                        "0.068023"
                ),
                "Random Forest",
                MODEL_VERSION,
                "Hybrid full-signal",
                InvoiceFraudPredictionResponse
                        .DetectionSource
                        .SUPERVISED_MODEL,
                InvoiceFraudPredictionResponse
                        .RecommendedAction
                        .ALLOW_WITH_MONITORING,
                new BigDecimal(
                        "0.500000"
                ),
                MODEL_SHA256,
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

    private FraudAssessment
    createExistingAssessment(
            String invoiceReference,
            String snapshotJson,
            String snapshotSha256
    ) {
        return FraudAssessment.create(
                invoiceReference,
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
                MODEL_VERSION,
                "Hybrid full-signal",
                FraudAssessment
                        .DetectionSource
                        .SUPERVISED_MODEL,
                MODEL_SHA256,
                snapshotJson,
                snapshotSha256,
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

            return HexFormat.of()
                    .formatHex(hash);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not calculate test SHA-256.",
                    exception
            );
        }
    }
}