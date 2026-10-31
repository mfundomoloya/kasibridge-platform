package com.kasibridge.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.fraud.dto.FraudAssessmentResponse;
import com.kasibridge.fraud.dto.FraudAssessmentSearchCriteria;
import com.kasibridge.fraud.entity.FraudAssessment;
import com.kasibridge.fraud.repository.FraudAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudAssessmentQueueServiceTest {

    private static final String MODEL_SHA256 =
            "1a9dc135791a80c183b707de7686e84a"
                    + "87d6e3c15e7c6db0d1ed17ee1a7fe4de";

    private static final String SNAPSHOT_SHA256 =
            "de5f16f86c54d9c78d5ebb08e9064513"
                    + "aa00237f47d460294f78c4deb907bd56";

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
    void searchAssessments_shouldMapRepositoryPage() {
        FraudAssessment assessment =
                createPersistedAssessment();

        Page<FraudAssessment> repositoryPage =
                new PageImpl<>(
                        List.of(assessment),
                        PageRequest.of(
                                0,
                                20
                        ),
                        1
                );

        when(
                assessmentRepository.findAll(
                        anyAssessmentSpecification(),
                        any(Pageable.class)
                )
        ).thenReturn(
                repositoryPage
        );

        FraudAssessmentSearchCriteria criteria =
                createCriteria();

        Page<FraudAssessmentResponse> result =
                assessmentService.searchAssessments(
                        criteria,
                        PageRequest.of(
                                0,
                                20
                        )
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                1,
                result.getContent().size()
        );

        FraudAssessmentResponse response =
                result.getContent().getFirst();

        assertEquals(
                "KB-FA-20261030-ABC12345",
                response.assessmentReference()
        );

        assertEquals(
                "INV-QUEUE-001",
                response.invoiceReference()
        );

        assertEquals(
                FraudAssessment.ReviewStatus
                        .PENDING_REVIEW,
                response.reviewStatus()
        );

        assertEquals(
                FraudAssessment.RiskLevel.HIGH,
                response.riskLevel()
        );

        assertEquals(
                "procurement-fraud-v1",
                response.modelVersion()
        );
    }

    @Test
    void searchAssessments_shouldPassCriteriaThroughSpecification() {
        Page<FraudAssessment> emptyPage =
                Page.empty(
                        PageRequest.of(
                                0,
                                20
                        )
                );

        when(
                assessmentRepository.findAll(
                        anyAssessmentSpecification(),
                        any(Pageable.class)
                )
        ).thenReturn(
                emptyPage
        );

        FraudAssessmentSearchCriteria criteria =
                new FraudAssessmentSearchCriteria(
                        FraudAssessment.ReviewStatus
                                .PENDING_REVIEW,
                        FraudAssessment.RiskLevel
                                .CRITICAL,
                        FraudAssessment.Classification
                                .HIGH_RISK,
                        "  SUPPLIER-DEMO-001  ",
                        "  DPT-025  ",
                        "  procurement-fraud-v1  ",
                        LocalDateTime.of(
                                2026,
                                10,
                                1,
                                0,
                                0
                        ),
                        LocalDateTime.of(
                                2026,
                                10,
                                31,
                                23,
                                59,
                                59
                        )
                );

        Page<FraudAssessmentResponse> result =
                assessmentService.searchAssessments(
                        criteria,
                        PageRequest.of(
                                0,
                                20
                        )
                );

        assertTrue(
                result.isEmpty()
        );

        assertEquals(
                "SUPPLIER-DEMO-001",
                criteria.supplierId()
        );

        assertEquals(
                "DPT-025",
                criteria.departmentId()
        );

        assertEquals(
                "procurement-fraud-v1",
                criteria.modelVersion()
        );

        verify(
                assessmentRepository
        ).findAll(
                anyAssessmentSpecification(),
                any(Pageable.class)
        );
    }

    @Test
    void searchAssessments_shouldUseDefaultPaginationWhenPageableIsNull() {
        Page<FraudAssessment> emptyPage =
                Page.empty(
                        PageRequest.of(
                                0,
                                20
                        )
                );

        when(
                assessmentRepository.findAll(
                        anyAssessmentSpecification(),
                        any(Pageable.class)
                )
        ).thenReturn(
                emptyPage
        );

        assessmentService.searchAssessments(
                null,
                null
        );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        verify(
                assessmentRepository
        ).findAll(
                anyAssessmentSpecification(),
                pageableCaptor.capture()
        );

        Pageable pageable =
                pageableCaptor.getValue();

        assertEquals(
                0,
                pageable.getPageNumber()
        );

        assertEquals(
                20,
                pageable.getPageSize()
        );

        assertTrue(
                pageable.getSort().isUnsorted()
        );
    }

    @Test
    void searchAssessments_shouldPreserveAllowedExplicitSort() {
        Sort requestedSort =
                Sort.by(
                        Sort.Order.desc(
                                "assessedAt"
                        ),
                        Sort.Order.asc(
                                "assessmentReference"
                        )
                );

        Pageable requestedPageable =
                PageRequest.of(
                        2,
                        25,
                        requestedSort
                );

        Page<FraudAssessment> emptyPage =
                Page.empty(
                        requestedPageable
                );

        when(
                assessmentRepository.findAll(
                        anyAssessmentSpecification(),
                        any(Pageable.class)
                )
        ).thenReturn(
                emptyPage
        );

        assessmentService.searchAssessments(
                createCriteria(),
                requestedPageable
        );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        verify(
                assessmentRepository
        ).findAll(
                anyAssessmentSpecification(),
                pageableCaptor.capture()
        );

        Pageable validatedPageable =
                pageableCaptor.getValue();

        assertEquals(
                2,
                validatedPageable.getPageNumber()
        );

        assertEquals(
                25,
                validatedPageable.getPageSize()
        );

        assertEquals(
                Sort.Direction.DESC,
                validatedPageable
                        .getSort()
                        .getOrderFor(
                                "assessedAt"
                        )
                        .getDirection()
        );

        assertEquals(
                Sort.Direction.ASC,
                validatedPageable
                        .getSort()
                        .getOrderFor(
                                "assessmentReference"
                        )
                        .getDirection()
        );
    }

    @Test
    void searchAssessments_shouldRejectUnsupportedSortField() {
        Pageable pageable =
                PageRequest.of(
                        0,
                        20,
                        Sort.by(
                                "featureSnapshotJson"
                        )
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                assessmentService
                                        .searchAssessments(
                                                createCriteria(),
                                                pageable
                                        )
                );

        assertEquals(
                "Unsupported fraud assessment "
                        + "sort field: "
                        + "featureSnapshotJson.",
                exception.getMessage()
        );
    }

    @Test
    void searchAssessments_shouldRejectPageSizeAboveMaximum() {
        Pageable pageable =
                PageRequest.of(
                        0,
                        101
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                assessmentService
                                        .searchAssessments(
                                                createCriteria(),
                                                pageable
                                        )
                );

        assertEquals(
                "Page size cannot exceed 100.",
                exception.getMessage()
        );
    }

    @Test
    void searchAssessments_shouldRejectInvalidAssessmentTimeRange() {
        FraudAssessmentSearchCriteria criteria =
                new FraudAssessmentSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        LocalDateTime.of(
                                2026,
                                10,
                                31,
                                0,
                                0
                        ),
                        LocalDateTime.of(
                                2026,
                                10,
                                1,
                                0,
                                0
                        )
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                assessmentService
                                        .searchAssessments(
                                                criteria,
                                                PageRequest.of(
                                                        0,
                                                        20
                                                )
                                        )
                );

        assertEquals(
                "Assessment start time cannot "
                        + "be after assessment "
                        + "end time.",
                exception.getMessage()
        );

        verify(
                assessmentRepository,
                never()
        ).findAll(
                anyAssessmentSpecification(),
                any(Pageable.class)
        );
    }
    @Test
    void searchCriteria_shouldReportWhetherFiltersArePresent() {
        FraudAssessmentSearchCriteria populated =
                createCriteria();

        FraudAssessmentSearchCriteria empty =
                new FraudAssessmentSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        assertTrue(
                populated.hasFilters()
        );

        assertFalse(
                empty.hasFilters()
        );
    }

    private FraudAssessmentSearchCriteria
    createCriteria() {
        return new FraudAssessmentSearchCriteria(
                FraudAssessment.ReviewStatus
                        .PENDING_REVIEW,
                FraudAssessment.RiskLevel
                        .HIGH,
                FraudAssessment.Classification
                        .HIGH_RISK,
                "SUPPLIER-DEMO-001",
                "DPT-025",
                "procurement-fraud-v1",
                null,
                null
        );
    }

    private FraudAssessment
    createPersistedAssessment() {
        LocalDateTime assessedAt =
                LocalDateTime.of(
                        2026,
                        10,
                        30,
                        8,
                        30,
                        0
                );

        FraudAssessment assessment =
                FraudAssessment.create(
                        "INV-QUEUE-001",
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
                            "INV-QUEUE-001"
                        }
                        """,
                        SNAPSHOT_SHA256,
                        assessedAt
                );

        ReflectionTestUtils.setField(
                assessment,
                "id",
                10L
        );

        ReflectionTestUtils.setField(
                assessment,
                "assessmentReference",
                "KB-FA-20261030-ABC12345"
        );

        ReflectionTestUtils.setField(
                assessment,
                "createdAt",
                assessedAt.plusSeconds(1)
        );

        ReflectionTestUtils.setField(
                assessment,
                "version",
                0L
        );

        return assessment;
    }

    @SuppressWarnings("unchecked")
    private Specification<FraudAssessment>
    anyAssessmentSpecification() {
        return any(
                Specification.class
        );
    }
}