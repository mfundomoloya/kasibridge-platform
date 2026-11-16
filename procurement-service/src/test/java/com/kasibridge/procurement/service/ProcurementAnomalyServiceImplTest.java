package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.ProcurementAnomalyRecordResponse;
import com.kasibridge.procurement.dto.ReviewProcurementAnomalyRequest;
import com.kasibridge.procurement.entity.ProcurementAnomaly;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import com.kasibridge.procurement.exception.AnomalyStateException;
import com.kasibridge.procurement.exception.ProcurementAuthorizationException;
import com.kasibridge.procurement.repository.ProcurementAnomalyRepository;
import com.kasibridge.procurement.repository.TenderCommitteeAssignmentRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcurementAnomalyServiceImplTest {

    private static final Long ANOMALY_ID = 91L;
    private static final Long TENDER_ID = 11L;
    private static final Long BID_ID = 101L;
    private static final Long ADJUDICATOR_USER_ID = 42L;

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private AdjudicationService adjudicationService;

    @Mock
    private ProcurementAnomalyRepository anomalyRepository;

    @Mock
    private TenderCommitteeAssignmentRepository
            assignmentRepository;

    @Mock
    private ProcurementAuditService auditService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProcurementNotificationService
            procurementNotificationService;

    private ProcurementAnomalyServiceImpl anomalyService;

    @BeforeEach
    void setUp() {
        anomalyService =
                new ProcurementAnomalyServiceImpl(
                        tenderRepository,
                        adjudicationService,
                        anomalyRepository,
                        auditService,
                        currentUserService,
                        procurementNotificationService,
                        assignmentRepository
                );
    }

    @Test
    void shouldMarkOpenAnomalyReviewedForAssignedAdjudicator() {
        ProcurementAnomaly anomaly =
                openAnomaly();

        ReviewProcurementAnomalyRequest request =
                reviewRequest(
                        " Evidence was checked against "
                                + "the immutable evaluation "
                                + "record. "
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(ADJUDICATOR_USER_ID);

        when(anomalyRepository.findById(ANOMALY_ID))
                .thenReturn(Optional.of(anomaly));

        when(
                assignmentRepository
                        .existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
                                TENDER_ID,
                                ADJUDICATOR_USER_ID,
                                TenderCommitteeAssignment
                                        .CommitteeRole
                                        .ADJUDICATOR
                        )
        ).thenReturn(true);

        when(anomalyRepository.save(anomaly))
                .thenReturn(anomaly);

        ProcurementAnomalyRecordResponse response =
                anomalyService.markReviewed(
                        ANOMALY_ID,
                        request
                );

        assertEquals(
                ProcurementAnomaly.AnomalyStatus.REVIEWED,
                anomaly.getStatus()
        );

        assertEquals(
                ADJUDICATOR_USER_ID,
                anomaly.getReviewedByUserId()
        );

        assertEquals(
                "Evidence was checked against "
                        + "the immutable evaluation "
                        + "record.",
                anomaly.getReviewNotes()
        );

        assertNotNull(
                anomaly.getReviewedAt()
        );

        assertEquals(
                ProcurementAnomaly.AnomalyStatus.REVIEWED,
                response.getStatus()
        );

        verify(anomalyRepository).save(anomaly);

        verify(auditService).recordSuccess(
                ProcurementAuditEvent
                        .AuditEventType
                        .PROCUREMENT_ANOMALY_REVIEWED,
                TENDER_ID,
                BID_ID,
                ADJUDICATOR_USER_ID,
                "Procurement anomaly reviewed",
                "Anomaly reference="
                        + anomaly.getAnomalyReference()
                        + ", reviewNotes="
                        + anomaly.getReviewNotes()
        );
    }

    @Test
    void shouldDismissOpenAnomalyForAssignedAdjudicator() {
        ProcurementAnomaly anomaly =
                openAnomaly();

        ReviewProcurementAnomalyRequest request =
                reviewRequest(
                        "The score gap is supported by "
                                + "the published evaluation "
                                + "evidence."
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(ADJUDICATOR_USER_ID);

        when(anomalyRepository.findById(ANOMALY_ID))
                .thenReturn(Optional.of(anomaly));

        when(
                assignmentRepository
                        .existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
                                TENDER_ID,
                                ADJUDICATOR_USER_ID,
                                TenderCommitteeAssignment
                                        .CommitteeRole
                                        .ADJUDICATOR
                        )
        ).thenReturn(true);

        when(anomalyRepository.save(anomaly))
                .thenReturn(anomaly);

        ProcurementAnomalyRecordResponse response =
                anomalyService.dismiss(
                        ANOMALY_ID,
                        request
                );

        assertEquals(
                ProcurementAnomaly.AnomalyStatus.DISMISSED,
                anomaly.getStatus()
        );

        assertEquals(
                ADJUDICATOR_USER_ID,
                anomaly.getReviewedByUserId()
        );

        assertEquals(
                request.getReviewNotes(),
                anomaly.getReviewNotes()
        );

        assertNotNull(
                anomaly.getReviewedAt()
        );

        assertEquals(
                ProcurementAnomaly.AnomalyStatus.DISMISSED,
                response.getStatus()
        );

        verify(anomalyRepository).save(anomaly);

        verify(auditService).recordSuccess(
                ProcurementAuditEvent
                        .AuditEventType
                        .PROCUREMENT_ANOMALY_DISMISSED,
                TENDER_ID,
                BID_ID,
                ADJUDICATOR_USER_ID,
                "Procurement anomaly dismissed",
                "Anomaly reference="
                        + anomaly.getAnomalyReference()
                        + ", reviewNotes="
                        + anomaly.getReviewNotes()
        );
    }

    @Test
    void shouldRejectTransitionForUnassignedAdjudicator() {
        ProcurementAnomaly anomaly =
                openAnomaly();

        ReviewProcurementAnomalyRequest request =
                reviewRequest(
                        "Attempted review of an "
                                + "unassigned tender."
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(ADJUDICATOR_USER_ID);

        when(anomalyRepository.findById(ANOMALY_ID))
                .thenReturn(Optional.of(anomaly));

        when(
                assignmentRepository
                        .existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
                                TENDER_ID,
                                ADJUDICATOR_USER_ID,
                                TenderCommitteeAssignment
                                        .CommitteeRole
                                        .ADJUDICATOR
                        )
        ).thenReturn(false);

        ProcurementAuthorizationException exception =
                assertThrows(
                        ProcurementAuthorizationException.class,
                        () ->
                                anomalyService.markReviewed(
                                        ANOMALY_ID,
                                        request
                                )
                );

        assertEquals(
                "User is not assigned as adjudicator "
                        + "to this anomaly's tender.",
                exception.getMessage()
        );

        assertEquals(
                ProcurementAnomaly.AnomalyStatus.OPEN,
                anomaly.getStatus()
        );

        assertNull(
                anomaly.getReviewedByUserId()
        );

        assertNull(
                anomaly.getReviewNotes()
        );

        assertNull(
                anomaly.getReviewedAt()
        );

        verify(anomalyRepository, never())
                .save(any(ProcurementAnomaly.class));

        verify(auditService).recordFailure(
                ProcurementAuditEvent
                        .AuditEventType
                        .PROCUREMENT_ANOMALY_TRANSITION_REJECTED,
                TENDER_ID,
                BID_ID,
                ADJUDICATOR_USER_ID,
                "Procurement anomaly transition rejected",
                "User is not assigned as adjudicator "
                        + "to tenderId="
                        + TENDER_ID
                        + ", anomalyReference="
                        + anomaly.getAnomalyReference()
        );

        verify(
                auditService,
                never()
        ).recordSuccess(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );

        verifyNoInteractions(
                procurementNotificationService
        );
    }

    @Test
    void shouldRejectTransitionWhenAnomalyIsNotOpen() {
        ProcurementAnomaly anomaly =
                openAnomaly();

        anomaly.setStatus(
                ProcurementAnomaly.AnomalyStatus.REVIEWED
        );

        anomaly.setReviewedByUserId(7L);
        anomaly.setReviewNotes(
                "Previously reviewed."
        );

        LocalDateTime originalReviewedAt =
                LocalDateTime.now()
                        .minusDays(1);

        anomaly.setReviewedAt(
                originalReviewedAt
        );

        ReviewProcurementAnomalyRequest request =
                reviewRequest(
                        "Attempted second transition."
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(ADJUDICATOR_USER_ID);

        when(anomalyRepository.findById(ANOMALY_ID))
                .thenReturn(Optional.of(anomaly));

        when(
                assignmentRepository
                        .existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
                                TENDER_ID,
                                ADJUDICATOR_USER_ID,
                                TenderCommitteeAssignment
                                        .CommitteeRole
                                        .ADJUDICATOR
                        )
        ).thenReturn(true);

        AnomalyStateException exception =
                assertThrows(
                        AnomalyStateException.class,
                        () ->
                                anomalyService.dismiss(
                                        ANOMALY_ID,
                                        request
                                )
                );

        assertEquals(
                "Only OPEN anomalies can be "
                        + "reviewed or dismissed.",
                exception.getMessage()
        );

        assertEquals(
                ProcurementAnomaly.AnomalyStatus.REVIEWED,
                anomaly.getStatus()
        );

        assertEquals(
                7L,
                anomaly.getReviewedByUserId()
        );

        assertEquals(
                "Previously reviewed.",
                anomaly.getReviewNotes()
        );

        assertEquals(
                originalReviewedAt,
                anomaly.getReviewedAt()
        );

        verify(anomalyRepository, never())
                .save(any(ProcurementAnomaly.class));

        verify(
                auditService,
                never()
        ).recordSuccess(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );

        verifyNoInteractions(
                procurementNotificationService
        );
    }

    private ProcurementAnomaly openAnomaly() {
        return ProcurementAnomaly.builder()
                .id(ANOMALY_ID)
                .anomalyReference(
                        "KB-ANOMALY-00000091"
                )
                .tenderId(TENDER_ID)
                .bidId(BID_ID)
                .type(
                        ProcurementAnomaly.AnomalyType
                                .LARGE_SCORE_GAP
                )
                .severity(
                        ProcurementAnomaly.Severity.HIGH
                )
                .status(
                        ProcurementAnomaly.AnomalyStatus.OPEN
                )
                .message(
                        "A large score gap requires "
                                + "adjudicator review."
                )
                .evidence(
                        "Highest score=92.00, "
                                + "next score=58.00"
                )
                .detectedAt(
                        LocalDateTime.now()
                                .minusHours(2)
                )
                .build();
    }

    private ReviewProcurementAnomalyRequest reviewRequest(
            String notes
    ) {
        ReviewProcurementAnomalyRequest request =
                new ReviewProcurementAnomalyRequest();

        request.setReviewNotes(notes);

        return request;
    }
}