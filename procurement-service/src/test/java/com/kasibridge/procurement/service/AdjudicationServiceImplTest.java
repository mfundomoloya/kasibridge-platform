package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AwardTenderRequest;
import com.kasibridge.procurement.dto.AwardTenderResponse;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import com.kasibridge.procurement.repository.BidEvaluationScoreRepository;
import com.kasibridge.procurement.repository.BidRepository;
import com.kasibridge.procurement.repository.TenderCommitteeAssignmentRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdjudicationServiceImplTest {

    private static final Long TENDER_ID = 11L;
    private static final Long WINNING_BID_ID = 101L;
    private static final Long LOSING_BID_ID = 102L;
    private static final Long ADJUDICATOR_USER_ID = 42L;

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private BidEvaluationScoreRepository scoreRepository;

    @Mock
    private ProcurementAuditService auditService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TenderCommitteeAssignmentRepository
            assignmentRepository;

    @Mock
    private ProcurementNotificationService
            procurementNotificationService;

    private AdjudicationServiceImpl adjudicationService;

    @BeforeEach
    void setUp() {
        adjudicationService =
                new AdjudicationServiceImpl(
                        tenderRepository,
                        bidRepository,
                        scoreRepository,
                        auditService,
                        currentUserService,
                        assignmentRepository,
                        procurementNotificationService
                );
    }

    @Test
    void shouldAwardEligibleEvaluatedBidAndRejectOtherEligibleBids() {
        Tender tender =
                Tender.builder()
                        .id(TENDER_ID)
                        .tenderReference(
                                "KB-TENDER-00000011"
                        )
                        .status(
                                Tender.TenderStatus
                                        .ADJUDICATION
                        )
                        .build();

        Bid winningBid =
                Bid.builder()
                        .id(WINNING_BID_ID)
                        .tenderId(TENDER_ID)
                        .bidReference(
                                "KB-BID-00000101"
                        )
                        .bidderAlias(
                                "Bidder Alpha"
                        )
                        .status(
                                Bid.BidStatus
                                        .RECOMMENDED
                        )
                        .build();

        Bid losingBid =
                Bid.builder()
                        .id(LOSING_BID_ID)
                        .tenderId(TENDER_ID)
                        .bidReference(
                                "KB-BID-00000102"
                        )
                        .bidderAlias(
                                "Bidder Beta"
                        )
                        .status(
                                Bid.BidStatus
                                        .UNDER_EVALUATION
                        )
                        .build();

        AwardTenderRequest request =
                new AwardTenderRequest();

        request.setWinningBidId(
                WINNING_BID_ID
        );
        request.setAwardReason(
                "Highest evaluated bid based "
                        + "on the published criteria."
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(ADJUDICATOR_USER_ID);

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(Optional.of(tender));

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

        when(bidRepository.findById(WINNING_BID_ID))
                .thenReturn(Optional.of(winningBid));

        when(
                scoreRepository
                        .existsByTenderIdAndBidId(
                                TENDER_ID,
                                WINNING_BID_ID
                        )
        ).thenReturn(true);

        when(
                bidRepository
                        .findByTenderIdAndStatusIn(
                                eq(TENDER_ID),
                                anyCollection()
                        )
        ).thenReturn(
                List.of(
                        winningBid,
                        losingBid
                )
        );

        when(bidRepository.save(winningBid))
                .thenReturn(winningBid);

        when(tenderRepository.save(tender))
                .thenReturn(tender);

        AwardTenderResponse response =
                adjudicationService.awardTender(
                        TENDER_ID,
                        request
                );

        assertEquals(
                Bid.BidStatus.AWARDED,
                winningBid.getStatus()
        );
        assertEquals(
                Bid.BidStatus.REJECTED,
                losingBid.getStatus()
        );
        assertEquals(
                Tender.TenderStatus.AWARDED,
                tender.getStatus()
        );
        assertEquals(
                WINNING_BID_ID,
                tender.getAwardedBidId()
        );
        assertEquals(
                ADJUDICATOR_USER_ID,
                tender.getAwardedByUserId()
        );
        assertEquals(
                request.getAwardReason(),
                tender.getAwardReason()
        );
        assertTrue(
                tender.getAwardedAt() != null
        );
        assertTrue(
                tender.getUpdatedAt() != null
        );

        assertEquals(
                TENDER_ID,
                response.getTenderId()
        );
        assertEquals(
                Tender.TenderStatus.AWARDED,
                response.getTenderStatus()
        );
        assertEquals(
                WINNING_BID_ID,
                response.getWinningBidId()
        );
        assertEquals(
                Bid.BidStatus.AWARDED,
                response.getWinningBidStatus()
        );
        assertEquals(
                ADJUDICATOR_USER_ID,
                response.getAdjudicatorUserId()
        );

        verify(bidRepository).save(winningBid);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Bid>> rejectedBidsCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(bidRepository).saveAll(
                rejectedBidsCaptor.capture()
        );

        List<Bid> persistedEligibleBids =
                rejectedBidsCaptor.getValue();

        assertTrue(
                persistedEligibleBids.contains(
                        losingBid
                )
        );

        verify(tenderRepository).save(tender);

        ArgumentCaptor<String> auditDetailsCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(auditService).recordSuccess(
                eq(
                        ProcurementAuditEvent
                                .AuditEventType
                                .TENDER_AWARDED
                ),
                eq(TENDER_ID),
                eq(WINNING_BID_ID),
                eq(ADJUDICATOR_USER_ID),
                eq("Tender awarded"),
                auditDetailsCaptor.capture()
        );

        String auditDetails =
                auditDetailsCaptor.getValue();

        assertTrue(
                auditDetails.contains(
                        "KB-TENDER-00000011"
                )
        );
        assertTrue(
                auditDetails.contains(
                        "Bidder Alpha"
                )
        );
        assertTrue(
                auditDetails.contains(
                        request.getAwardReason()
                )
        );

        verify(procurementNotificationService)
                .queueTenderAwarded(
                        winningBid,
                        tender
                );

        assertEquals(
                List.of(losingBid),
                persistedEligibleBids
        );
    }
}