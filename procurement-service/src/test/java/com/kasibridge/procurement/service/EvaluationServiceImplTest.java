package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BlindBidResponse;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import com.kasibridge.procurement.exception.BidEvaluationException;
import com.kasibridge.procurement.exception.ProcurementAuthorizationException;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.repository.BidEvaluationScoreRepository;
import com.kasibridge.procurement.repository.BidRepository;
import com.kasibridge.procurement.repository.TenderCommitteeAssignmentRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    private static final Long TENDER_ID = 7L;
    private static final Long EVALUATOR_USER_ID = 20L;

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private BidEvaluationScoreRepository scoreRepository;

    @Mock
    private TenderCommitteeAssignmentRepository assignmentRepository;

    @Mock
    private ProcurementAuditService auditService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private Tender evaluationTender;

    @BeforeEach
    void setUp() {
        evaluationTender = Tender.builder()
                .id(TENDER_ID)
                .tenderReference("KB-TENDER-436FC79F")
                .title(
                        "Supply of ICT Equipment for Municipal Offices"
                )
                .status(Tender.TenderStatus.EVALUATION)
                .build();
    }

    @Test
    void shouldReturnOnlyEligibleBlindBidsDuringEvaluation() {
        Bid compliantBid = createBid(
                16L,
                "Bidder B",
                "First blind technical proposal.",
                Bid.BidStatus.COMPLIANT
        );

        Bid underEvaluationBid = createBid(
                18L,
                "Bidder C",
                "Second blind technical proposal.",
                Bid.BidStatus.UNDER_EVALUATION
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(EVALUATOR_USER_ID);

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(evaluationTender)
                );

        mockAssignedEvaluator();

        when(
                bidRepository.findByTenderIdAndStatusIn(
                        TENDER_ID,
                        eligibleEvaluationStatuses()
                )
        ).thenReturn(
                List.of(
                        compliantBid,
                        underEvaluationBid
                )
        );

        List<BlindBidResponse> responses =
                evaluationService
                        .getBlindBidsForEvaluation(
                                TENDER_ID
                        );

        assertEquals(
                2,
                responses.size()
        );

        BlindBidResponse firstResponse =
                responses.get(0);

        assertEquals(
                16L,
                firstResponse.getBidId()
        );

        assertEquals(
                TENDER_ID,
                firstResponse.getTenderId()
        );

        assertEquals(
                "Bidder B",
                firstResponse.getBidderAlias()
        );

        assertEquals(
                "First blind technical proposal.",
                firstResponse.getTechnicalProposal()
        );

        BlindBidResponse secondResponse =
                responses.get(1);

        assertEquals(
                18L,
                secondResponse.getBidId()
        );

        assertEquals(
                TENDER_ID,
                secondResponse.getTenderId()
        );

        assertEquals(
                "Bidder C",
                secondResponse.getBidderAlias()
        );

        assertEquals(
                "Second blind technical proposal.",
                secondResponse.getTechnicalProposal()
        );

        verify(bidRepository)
                .findByTenderIdAndStatusIn(
                        TENDER_ID,
                        eligibleEvaluationStatuses()
                );

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_SCORE_VIEWED
                        ),
                        eq(TENDER_ID),
                        isNull(),
                        eq(EVALUATOR_USER_ID),
                        eq("Evaluator viewed blind bids"),
                        eq(
                                "Blind bid list accessed for tenderId="
                                        + TENDER_ID
                        )
                );
    }

    @Test
    void shouldReturnEmptyListWhenNoBidsAreEligibleForEvaluation() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(EVALUATOR_USER_ID);

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(evaluationTender)
                );

        mockAssignedEvaluator();

        when(
                bidRepository.findByTenderIdAndStatusIn(
                        TENDER_ID,
                        eligibleEvaluationStatuses()
                )
        ).thenReturn(List.of());

        List<BlindBidResponse> responses =
                evaluationService
                        .getBlindBidsForEvaluation(
                                TENDER_ID
                        );

        assertTrue(
                responses.isEmpty()
        );

        verify(bidRepository)
                .findByTenderIdAndStatusIn(
                        TENDER_ID,
                        eligibleEvaluationStatuses()
                );

        verify(
                bidRepository,
                never()
        ).findByTenderId(
                any()
        );

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_SCORE_VIEWED
                        ),
                        eq(TENDER_ID),
                        isNull(),
                        eq(EVALUATOR_USER_ID),
                        eq("Evaluator viewed blind bids"),
                        eq(
                                "Blind bid list accessed for tenderId="
                                        + TENDER_ID
                        )
                );
    }

    @Test
    void shouldRejectBlindBidRetrievalOutsideEvaluationStage() {
        evaluationTender.setStatus(
                Tender.TenderStatus.PUBLISHED
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(EVALUATOR_USER_ID);

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(evaluationTender)
                );

        mockAssignedEvaluator();

        BidEvaluationException exception =
                assertThrows(
                        BidEvaluationException.class,
                        () -> evaluationService
                                .getBlindBidsForEvaluation(
                                        TENDER_ID
                                )
                );

        assertEquals(
                "Blind bids can only be viewed when tender is in EVALUATION status",
                exception.getMessage()
        );

        verify(
                bidRepository,
                never()
        ).findByTenderIdAndStatusIn(
                any(),
                any()
        );

        verify(
                auditService,
                never()
        ).recordSuccess(
                any(),
                any(),
                any(),
                any(),
                anyString(),
                anyString()
        );
    }

    @Test
    void shouldRejectUnassignedEvaluator() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(EVALUATOR_USER_ID);

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(evaluationTender)
                );

        when(
                currentUserService.hasRole(
                        "ROLE_PLATFORM_ADMIN"
                )
        ).thenReturn(false);

        when(
                assignmentRepository
                        .existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
                                TENDER_ID,
                                EVALUATOR_USER_ID,
                                TenderCommitteeAssignment
                                        .CommitteeRole
                                        .EVALUATOR
                        )
        ).thenReturn(false);

        ProcurementAuthorizationException exception =
                assertThrows(
                        ProcurementAuthorizationException.class,
                        () -> evaluationService
                                .getBlindBidsForEvaluation(
                                        TENDER_ID
                                )
                );

        assertEquals(
                "User is not assigned as evaluator to this tender.",
                exception.getMessage()
        );

        verify(auditService)
                .recordFailure(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_SCORE_REJECTED_UNASSIGNED_EVALUATOR
                        ),
                        eq(TENDER_ID),
                        isNull(),
                        eq(EVALUATOR_USER_ID),
                        eq("Evaluator access rejected"),
                        eq(
                                "User is not assigned as evaluator to tenderId="
                                        + TENDER_ID
                        )
                );

        verify(
                bidRepository,
                never()
        ).findByTenderIdAndStatusIn(
                any(),
                any()
        );

        verify(
                auditService,
                never()
        ).recordSuccess(
                any(),
                any(),
                any(),
                any(),
                anyString(),
                anyString()
        );
    }

    @Test
    void shouldRejectBlindBidRetrievalWhenTenderDoesNotExist() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(EVALUATOR_USER_ID);

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(Optional.empty());

        TenderNotFoundException exception =
                assertThrows(
                        TenderNotFoundException.class,
                        () -> evaluationService
                                .getBlindBidsForEvaluation(
                                        TENDER_ID
                                )
                );

        assertEquals(
                "Tender not found with ID: " + TENDER_ID,
                exception.getMessage()
        );

        verify(
                currentUserService,
                never()
        ).hasRole(
                anyString()
        );

        verify(
                assignmentRepository,
                never()
        ).existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
                any(),
                any(),
                any()
        );

        verify(
                bidRepository,
                never()
        ).findByTenderIdAndStatusIn(
                any(),
                any()
        );

        verify(
                auditService,
                never()
        ).recordSuccess(
                any(),
                any(),
                any(),
                any(),
                anyString(),
                anyString()
        );

        verify(
                auditService,
                never()
        ).recordFailure(
                any(),
                any(),
                any(),
                any(),
                anyString(),
                anyString()
        );
    }

    @Test
    void shouldAllowPlatformAdminWithoutEvaluatorAssignment() {
        Bid compliantBid = createBid(
                16L,
                "Bidder B",
                "Platform administrator blind proposal view.",
                Bid.BidStatus.COMPLIANT
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(EVALUATOR_USER_ID);

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(evaluationTender)
                );

        when(
                currentUserService.hasRole(
                        "ROLE_PLATFORM_ADMIN"
                )
        ).thenReturn(true);

        when(
                bidRepository.findByTenderIdAndStatusIn(
                        TENDER_ID,
                        eligibleEvaluationStatuses()
                )
        ).thenReturn(
                List.of(compliantBid)
        );

        List<BlindBidResponse> responses =
                evaluationService
                        .getBlindBidsForEvaluation(
                                TENDER_ID
                        );

        assertEquals(
                1,
                responses.size()
        );

        BlindBidResponse response =
                responses.get(0);

        assertEquals(
                16L,
                response.getBidId()
        );

        assertEquals(
                TENDER_ID,
                response.getTenderId()
        );

        assertEquals(
                "Bidder B",
                response.getBidderAlias()
        );

        assertEquals(
                "Platform administrator blind proposal view.",
                response.getTechnicalProposal()
        );

        verify(
                assignmentRepository,
                never()
        ).existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
                any(),
                any(),
                any()
        );

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_SCORE_VIEWED
                        ),
                        eq(TENDER_ID),
                        isNull(),
                        eq(EVALUATOR_USER_ID),
                        eq("Evaluator viewed blind bids"),
                        eq(
                                "Blind bid list accessed for tenderId="
                                        + TENDER_ID
                        )
                );
    }

    private void mockAssignedEvaluator() {
        when(
                currentUserService.hasRole(
                        "ROLE_PLATFORM_ADMIN"
                )
        ).thenReturn(false);

        when(
                assignmentRepository
                        .existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
                                TENDER_ID,
                                EVALUATOR_USER_ID,
                                TenderCommitteeAssignment
                                        .CommitteeRole
                                        .EVALUATOR
                        )
        ).thenReturn(true);
    }

    private List<Bid.BidStatus> eligibleEvaluationStatuses() {
        return List.of(
                Bid.BidStatus.COMPLIANT,
                Bid.BidStatus.UNDER_EVALUATION
        );
    }

    private Bid createBid(
            Long bidId,
            String bidderAlias,
            String technicalProposal,
            Bid.BidStatus status
    ) {
        return Bid.builder()
                .id(bidId)
                .bidReference(
                        "KB-BID-" + bidId
                )
                .tenderId(TENDER_ID)
                .traderProfileId(2L)
                .submittedByUserId(8L)
                .bidderAlias(bidderAlias)
                .technicalProposal(technicalProposal)
                .priceAmount(
                        new BigDecimal("690000.00")
                )
                .status(status)
                .submittedAt(
                        LocalDateTime.now().minusDays(1)
                )
                .updatedAt(
                        LocalDateTime.now().minusHours(1)
                )
                .build();
    }
}