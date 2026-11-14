package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.EvaluationProgressResponse;
import com.kasibridge.procurement.dto.TenderResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.exception.TenderStateException;
import com.kasibridge.procurement.repository.TenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenderServiceImplTest {

    @Mock
    private TenderRepository repository;

    @Mock
    private SpecificationHashService hashService;

    @Mock
    private ProcurementAuditService auditService;

    @Mock
    private ProcurementNotificationService procurementNotificationService;

    @Mock
    private EvaluationService evaluationService;

    @Mock
    private CurrentUserService currentUserService;

    private TenderServiceImpl tenderService;

    @BeforeEach
    void setUp() {
        tenderService = new TenderServiceImpl(
                repository,
                hashService,
                auditService,
                procurementNotificationService,
                evaluationService,
                currentUserService
        );
    }

    @Test
    void shouldStartAdjudicationWhenEvaluationIsComplete() {
        Long tenderId = 11L;
        Long actorUserId = 42L;

        Tender tender = evaluationTender(tenderId);

        EvaluationProgressResponse progress =
                EvaluationProgressResponse.builder()
                        .tenderId(tenderId)
                        .tenderReference("KB-TENDER-00000011")
                        .tenderTitle("Safety equipment")
                        .activeEvaluatorCount(2L)
                        .eligibleBidCount(3L)
                        .expectedScoreCount(6L)
                        .submittedScoreCount(6L)
                        .remainingScoreCount(0L)
                        .completionPercentage(new BigDecimal("100.00"))
                        .evaluationComplete(true)
                        .readyForAdjudication(true)
                        .build();

        when(repository.findById(tenderId))
                .thenReturn(Optional.of(tender));
        when(evaluationService.getEvaluationProgress(tenderId))
                .thenReturn(progress);
        when(currentUserService.getCurrentUserId())
                .thenReturn(actorUserId);
        when(repository.save(any(Tender.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TenderResponse response =
                tenderService.startAdjudication(tenderId);

        assertEquals(
                Tender.TenderStatus.ADJUDICATION,
                tender.getStatus()
        );
        assertEquals(
                Tender.TenderStatus.ADJUDICATION,
                response.getStatus()
        );

        ArgumentCaptor<String> auditDetailsCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(repository).save(tender);
        verify(auditService).recordSuccess(
                eq(ProcurementAuditEvent.AuditEventType
                        .TENDER_ADJUDICATION_STARTED),
                eq(tenderId),
                eq(null),
                eq(actorUserId),
                eq("Tender moved to adjudication"),
                auditDetailsCaptor.capture()
        );

        String auditDetails = auditDetailsCaptor.getValue();

        assertTrue(auditDetails.contains("activeEvaluators=2"));
        assertTrue(auditDetails.contains("eligibleBids=3"));
        assertTrue(auditDetails.contains("submittedScores=6"));
        assertTrue(auditDetails.contains("expectedScores=6"));
    }

    @Test
    void shouldBlockAdjudicationWhenEvaluationIsIncomplete() {
        Long tenderId = 11L;
        Tender tender = evaluationTender(tenderId);

        EvaluationProgressResponse progress =
                EvaluationProgressResponse.builder()
                        .tenderId(tenderId)
                        .activeEvaluatorCount(2L)
                        .eligibleBidCount(3L)
                        .expectedScoreCount(6L)
                        .submittedScoreCount(5L)
                        .remainingScoreCount(1L)
                        .completionPercentage(new BigDecimal("83.33"))
                        .evaluationComplete(false)
                        .readyForAdjudication(false)
                        .blockingReason(
                                "One evaluation score is still outstanding."
                        )
                        .build();

        when(repository.findById(tenderId))
                .thenReturn(Optional.of(tender));
        when(evaluationService.getEvaluationProgress(tenderId))
                .thenReturn(progress);

        TenderStateException exception =
                assertThrows(
                        TenderStateException.class,
                        () -> tenderService.startAdjudication(tenderId)
                );

        assertEquals(
                "One evaluation score is still outstanding.",
                exception.getMessage()
        );
        assertEquals(
                Tender.TenderStatus.EVALUATION,
                tender.getStatus()
        );

        verify(repository, never()).save(any(Tender.class));
        verify(currentUserService, never()).getCurrentUserId();
        verify(auditService, never()).recordSuccess(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldRejectAdjudicationWhenTenderIsNotInEvaluation() {
        Long tenderId = 11L;

        Tender tender = evaluationTender(tenderId);
        tender.setStatus(Tender.TenderStatus.BIDDING_CLOSED);

        when(repository.findById(tenderId))
                .thenReturn(Optional.of(tender));

        TenderStateException exception =
                assertThrows(
                        TenderStateException.class,
                        () -> tenderService.startAdjudication(tenderId)
                );

        assertEquals(
                "Only EVALUATION tenders can move to ADJUDICATION.",
                exception.getMessage()
        );

        verify(evaluationService, never())
                .getEvaluationProgress(any());
        verify(repository, never()).save(any(Tender.class));
        verify(auditService, never()).recordSuccess(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private Tender evaluationTender(Long tenderId) {
        return Tender.builder()
                .id(tenderId)
                .tenderReference("KB-TENDER-00000011")
                .title("Safety equipment")
                .status(Tender.TenderStatus.EVALUATION)
                .createdByUserId(7L)
                .build();
    }
}