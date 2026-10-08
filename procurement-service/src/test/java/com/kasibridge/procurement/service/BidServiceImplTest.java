package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BidResponse;
import com.kasibridge.procurement.dto.SubmitBidRequest;
import com.kasibridge.procurement.dto.TraderProfileClientResponse;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.BidComplianceResult;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.exception.BidNotFoundException;
import com.kasibridge.procurement.exception.BidStateException;
import com.kasibridge.procurement.exception.DuplicateBidException;
import com.kasibridge.procurement.exception.TenderStateException;
import com.kasibridge.procurement.repository.BidComplianceResultRepository;
import com.kasibridge.procurement.repository.BidRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidServiceImplTest {

    private static final Long CURRENT_USER_ID = 8L;
    private static final Long TRADER_PROFILE_ID = 2L;
    private static final Long TENDER_ID = 7L;
    private static final Long BID_ID = 15L;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private BidComplianceResultRepository complianceRepository;

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private ComplianceGatekeeperService gatekeeperService;

    @Mock
    private ProcurementAuditService auditService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TraderProfileClient traderProfileClient;

    @Mock
    private ProcurementNotificationService procurementNotificationService;

    @InjectMocks
    private BidServiceImpl bidService;

    private TraderProfileClientResponse trader;
    private Tender publishedTender;
    private Bid compliantBid;

    @BeforeEach
    void setUp() {
        trader = new TraderProfileClientResponse();
        trader.setId(TRADER_PROFILE_ID);
        trader.setUserId(CURRENT_USER_ID);

        publishedTender = Tender.builder()
                .id(TENDER_ID)
                .tenderReference("KB-TENDER-436FC79F")
                .title(
                        "Supply of ICT Equipment for Municipal Offices"
                )
                .status(Tender.TenderStatus.PUBLISHED)
                .build();

        compliantBid = Bid.builder()
                .id(BID_ID)
                .bidReference("KB-BID-623BEF93")
                .tenderId(TENDER_ID)
                .traderProfileId(TRADER_PROFILE_ID)
                .submittedByUserId(CURRENT_USER_ID)
                .bidderAlias("Bidder A")
                .technicalProposal(
                        "Technical proposal for delivery and commissioning."
                )
                .priceAmount(
                        new BigDecimal("690000.00")
                )
                .status(Bid.BidStatus.COMPLIANT)
                .submittedAt(
                        LocalDateTime.now().minusDays(1)
                )
                .updatedAt(
                        LocalDateTime.now().minusDays(1)
                )
                .build();
    }

    @Test
    void shouldWithdrawOwnedBidWhileTenderIsPublished() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(CURRENT_USER_ID);

        when(traderProfileClient.getCurrentTraderProfile())
                .thenReturn(trader);

        when(bidRepository.findById(BID_ID))
                .thenReturn(
                        Optional.of(compliantBid)
                );

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(publishedTender)
                );

        when(bidRepository.save(any(Bid.class)))
                .thenAnswer(
                        invocation -> invocation.getArgument(0)
                );

        when(complianceRepository.findByBidId(BID_ID))
                .thenReturn(Optional.empty());

        BidResponse response =
                bidService.withdrawCurrentTraderBid(BID_ID);

        assertEquals(
                Bid.BidStatus.WITHDRAWN,
                response.getStatus()
        );

        assertEquals(
                BID_ID,
                response.getId()
        );

        assertEquals(
                TENDER_ID,
                response.getTenderId()
        );

        ArgumentCaptor<Bid> bidCaptor =
                ArgumentCaptor.forClass(Bid.class);

        verify(bidRepository)
                .save(
                        bidCaptor.capture()
                );

        assertEquals(
                Bid.BidStatus.WITHDRAWN,
                bidCaptor.getValue().getStatus()
        );

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_WITHDRAWN
                        ),
                        eq(TENDER_ID),
                        eq(BID_ID),
                        eq(CURRENT_USER_ID),
                        eq("Bid withdrawn"),
                        anyString()
                );

        verify(procurementNotificationService)
                .queueBidWithdrawn(
                        compliantBid,
                        publishedTender
                );
    }

    @Test
    void shouldRejectWithdrawalWhenBidDoesNotExist() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(CURRENT_USER_ID);

        when(traderProfileClient.getCurrentTraderProfile())
                .thenReturn(trader);

        when(bidRepository.findById(BID_ID))
                .thenReturn(Optional.empty());

        BidNotFoundException exception =
                assertThrows(
                        BidNotFoundException.class,
                        () -> bidService
                                .withdrawCurrentTraderBid(BID_ID)
                );

        assertEquals(
                "Bid not found with ID: " + BID_ID,
                exception.getMessage()
        );

        verify(
                bidRepository,
                never()
        ).save(
                any(Bid.class)
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
                procurementNotificationService,
                never()
        ).queueBidWithdrawn(
                any(Bid.class),
                any(Tender.class)
        );
    }

    @Test
    void shouldConcealBidWhenAuthenticatedTraderIsNotOwner() {
        TraderProfileClientResponse differentTrader =
                new TraderProfileClientResponse();

        differentTrader.setId(99L);
        differentTrader.setUserId(100L);

        when(currentUserService.getCurrentUserId())
                .thenReturn(100L);

        when(traderProfileClient.getCurrentTraderProfile())
                .thenReturn(differentTrader);

        when(bidRepository.findById(BID_ID))
                .thenReturn(
                        Optional.of(compliantBid)
                );

        BidNotFoundException exception =
                assertThrows(
                        BidNotFoundException.class,
                        () -> bidService
                                .withdrawCurrentTraderBid(BID_ID)
                );

        assertEquals(
                "Bid not found or does not belong to the authenticated trader.",
                exception.getMessage()
        );

        verify(
                tenderRepository,
                never()
        ).findById(
                any()
        );

        verify(
                bidRepository,
                never()
        ).save(
                any(Bid.class)
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
                procurementNotificationService,
                never()
        ).queueBidWithdrawn(
                any(Bid.class),
                any(Tender.class)
        );
    }

    @Test
    void shouldRejectBidThatHasAlreadyBeenWithdrawn() {
        compliantBid.setStatus(
                Bid.BidStatus.WITHDRAWN
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(CURRENT_USER_ID);

        when(traderProfileClient.getCurrentTraderProfile())
                .thenReturn(trader);

        when(bidRepository.findById(BID_ID))
                .thenReturn(
                        Optional.of(compliantBid)
                );

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(publishedTender)
                );

        BidStateException exception =
                assertThrows(
                        BidStateException.class,
                        () -> bidService
                                .withdrawCurrentTraderBid(BID_ID)
                );

        assertEquals(
                "Bid has already been withdrawn.",
                exception.getMessage()
        );

        verify(
                bidRepository,
                never()
        ).save(
                any(Bid.class)
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
                procurementNotificationService,
                never()
        ).queueBidWithdrawn(
                any(Bid.class),
                any(Tender.class)
        );
    }

    @Test
    void shouldRejectWithdrawalWhenTenderIsNotPublished() {
        publishedTender.setStatus(
                Tender.TenderStatus.EVALUATION
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(CURRENT_USER_ID);

        when(traderProfileClient.getCurrentTraderProfile())
                .thenReturn(trader);

        when(bidRepository.findById(BID_ID))
                .thenReturn(
                        Optional.of(compliantBid)
                );

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(publishedTender)
                );

        TenderStateException exception =
                assertThrows(
                        TenderStateException.class,
                        () -> bidService
                                .withdrawCurrentTraderBid(BID_ID)
                );

        assertEquals(
                "A bid can only be withdrawn while the tender is open for bidding.",
                exception.getMessage()
        );

        verify(
                bidRepository,
                never()
        ).save(
                any(Bid.class)
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
                procurementNotificationService,
                never()
        ).queueBidWithdrawn(
                any(Bid.class),
                any(Tender.class)
        );
    }

    @Test
    void shouldRejectWithdrawalForBidUnderEvaluation() {
        compliantBid.setStatus(
                Bid.BidStatus.UNDER_EVALUATION
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(CURRENT_USER_ID);

        when(traderProfileClient.getCurrentTraderProfile())
                .thenReturn(trader);

        when(bidRepository.findById(BID_ID))
                .thenReturn(
                        Optional.of(compliantBid)
                );

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(publishedTender)
                );

        BidStateException exception =
                assertThrows(
                        BidStateException.class,
                        () -> bidService
                                .withdrawCurrentTraderBid(BID_ID)
                );

        assertEquals(
                "A bid in status UNDER_EVALUATION cannot be withdrawn.",
                exception.getMessage()
        );

        verify(
                bidRepository,
                never()
        ).save(
                any(Bid.class)
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
                procurementNotificationService,
                never()
        ).queueBidWithdrawn(
                any(Bid.class),
                any(Tender.class)
        );
    }

    @Test
    void shouldRejectSubmissionWhenTraderHasActiveBidForTender() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(CURRENT_USER_ID);

        when(traderProfileClient.getCurrentTraderProfile())
                .thenReturn(trader);

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(publishedTender)
                );

        when(
                bidRepository.existsActiveBidForTrader(
                        TENDER_ID,
                        TRADER_PROFILE_ID,
                        Bid.BidStatus.WITHDRAWN
                )
        ).thenReturn(true);

        SubmitBidRequest request =
                validSubmitBidRequest();

        DuplicateBidException exception =
                assertThrows(
                        DuplicateBidException.class,
                        () -> bidService.submitBid(
                                TENDER_ID,
                                request
                        )
                );

        assertEquals(
                "Trader already has an active bid for this tender.",
                exception.getMessage()
        );

        verify(
                bidRepository,
                never()
        ).save(
                any(Bid.class)
        );

        verify(
                gatekeeperService,
                never()
        ).evaluate(
                any(SubmitBidRequest.class)
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
                procurementNotificationService,
                never()
        ).queueBidReceived(
                any(Bid.class),
                any(Tender.class)
        );
    }

    @Test
    void shouldAllowReplacementBidAfterPreviousBidWasWithdrawn() {
        Long replacementBidId = 16L;
        Long complianceResultId = 15L;

        SubmitBidRequest request =
                validSubmitBidRequest();

        ComplianceGatekeeperService.ComplianceDecision decision =
                new ComplianceGatekeeperService.ComplianceDecision(
                        true,
                        null
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(CURRENT_USER_ID);

        when(traderProfileClient.getCurrentTraderProfile())
                .thenReturn(trader);

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(publishedTender)
                );

        when(
                bidRepository.existsActiveBidForTrader(
                        TENDER_ID,
                        TRADER_PROFILE_ID,
                        Bid.BidStatus.WITHDRAWN
                )
        ).thenReturn(false);

        when(bidRepository.countByTenderId(TENDER_ID))
                .thenReturn(1L);

        when(bidRepository.save(any(Bid.class)))
                .thenAnswer(invocation -> {
                    Bid bid = invocation.getArgument(0);

                    if (bid.getId() == null) {
                        bid.setId(replacementBidId);
                    }

                    if (bid.getSubmittedAt() == null) {
                        bid.setSubmittedAt(
                                LocalDateTime.now()
                        );
                    }

                    if (bid.getUpdatedAt() == null) {
                        bid.setUpdatedAt(
                                LocalDateTime.now()
                        );
                    }

                    return bid;
                });

        when(gatekeeperService.evaluate(request))
                .thenReturn(decision);

        when(
                complianceRepository.save(
                        any(BidComplianceResult.class)
                )
        ).thenAnswer(invocation -> {
            BidComplianceResult complianceResult =
                    invocation.getArgument(0);

            complianceResult.setId(
                    complianceResultId
            );

            return complianceResult;
        });

        BidResponse response =
                bidService.submitBid(
                        TENDER_ID,
                        request
                );

        assertEquals(
                replacementBidId,
                response.getId()
        );

        assertEquals(
                TENDER_ID,
                response.getTenderId()
        );

        assertEquals(
                TRADER_PROFILE_ID,
                response.getTraderId()
        );

        assertEquals(
                "Bidder B",
                response.getBidderAlias()
        );

        assertEquals(
                new BigDecimal("690000.00"),
                response.getPriceAmount()
        );

        assertEquals(
                Bid.BidStatus.COMPLIANT,
                response.getStatus()
        );

        assertNotNull(
                response.getBidReference()
        );

        assertTrue(
                response.getBidReference()
                        .startsWith("KB-BID-")
        );

        assertNotNull(
                response.getCompliance()
        );

        assertEquals(
                complianceResultId,
                response.getCompliance().getId()
        );

        assertEquals(
                replacementBidId,
                response.getCompliance().getBidId()
        );

        assertTrue(
                response.getCompliance().isPassed()
        );

        ArgumentCaptor<Bid> bidCaptor =
                ArgumentCaptor.forClass(Bid.class);

        verify(
                bidRepository,
                times(2)
        ).save(
                bidCaptor.capture()
        );

        Bid savedBid =
                bidCaptor.getAllValues().get(1);

        assertEquals(
                replacementBidId,
                savedBid.getId()
        );

        assertEquals(
                TENDER_ID,
                savedBid.getTenderId()
        );

        assertEquals(
                TRADER_PROFILE_ID,
                savedBid.getTraderProfileId()
        );

        assertEquals(
                CURRENT_USER_ID,
                savedBid.getSubmittedByUserId()
        );

        assertEquals(
                "Bidder B",
                savedBid.getBidderAlias()
        );

        assertEquals(
                "Technical proposal for delivery and commissioning.",
                savedBid.getTechnicalProposal()
        );

        assertEquals(
                new BigDecimal("690000.00"),
                savedBid.getPriceAmount()
        );

        assertEquals(
                Bid.BidStatus.COMPLIANT,
                savedBid.getStatus()
        );

        assertNotNull(
                savedBid.getBidReference()
        );

        assertTrue(
                savedBid.getBidReference()
                        .startsWith("KB-BID-")
        );

        ArgumentCaptor<BidComplianceResult> complianceCaptor =
                ArgumentCaptor.forClass(
                        BidComplianceResult.class
                );

        verify(complianceRepository)
                .save(
                        complianceCaptor.capture()
                );

        BidComplianceResult savedCompliance =
                complianceCaptor.getValue();

        assertEquals(
                replacementBidId,
                savedCompliance.getBidId()
        );

        assertTrue(
                savedCompliance.isCsdValid()
        );

        assertTrue(
                savedCompliance.isTaxClearanceValid()
        );

        assertTrue(
                savedCompliance.isBbbeeValid()
        );

        assertTrue(
                savedCompliance.isRequiredDocumentsUploaded()
        );

        assertTrue(
                savedCompliance.isPassed()
        );

        assertNull(
                savedCompliance.getFailureReason()
        );

        verify(gatekeeperService)
                .evaluate(request);

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_SUBMITTED
                        ),
                        eq(TENDER_ID),
                        eq(replacementBidId),
                        eq(CURRENT_USER_ID),
                        eq("Bid submitted"),
                        anyString()
                );

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_COMPLIANCE_PASSED
                        ),
                        eq(TENDER_ID),
                        eq(replacementBidId),
                        eq(CURRENT_USER_ID),
                        eq("Bid compliance passed"),
                        eq(
                                "All baseline compliance checks passed"
                        )
                );

        verify(procurementNotificationService)
                .queueBidReceived(
                        savedBid,
                        publishedTender
                );

        verify(procurementNotificationService)
                .queueCompliancePassed(
                        savedBid,
                        publishedTender
                );

        verify(
                procurementNotificationService,
                never()
        ).queueComplianceFailed(
                any(Bid.class),
                any(Tender.class),
                anyString()
        );
    }

    @Test
    void shouldMarkReplacementBidAsComplianceFailed() {
        Long replacementBidId = 17L;
        Long complianceResultId = 16L;

        SubmitBidRequest request =
                validSubmitBidRequest();

        request.setTaxClearanceValid(false);
        request.setRequiredDocumentsUploaded(false);

        String failureReason =
                "Tax clearance is invalid or missing; "
                        + "Required documents were not uploaded";

        ComplianceGatekeeperService.ComplianceDecision decision =
                new ComplianceGatekeeperService.ComplianceDecision(
                        false,
                        failureReason
                );

        when(currentUserService.getCurrentUserId())
                .thenReturn(CURRENT_USER_ID);

        when(traderProfileClient.getCurrentTraderProfile())
                .thenReturn(trader);

        when(tenderRepository.findById(TENDER_ID))
                .thenReturn(
                        Optional.of(publishedTender)
                );

        when(
                bidRepository.existsActiveBidForTrader(
                        TENDER_ID,
                        TRADER_PROFILE_ID,
                        Bid.BidStatus.WITHDRAWN
                )
        ).thenReturn(false);

        when(bidRepository.countByTenderId(TENDER_ID))
                .thenReturn(2L);

        when(bidRepository.save(any(Bid.class)))
                .thenAnswer(invocation -> {
                    Bid bid = invocation.getArgument(0);

                    if (bid.getId() == null) {
                        bid.setId(replacementBidId);
                    }

                    if (bid.getSubmittedAt() == null) {
                        bid.setSubmittedAt(
                                LocalDateTime.now()
                        );
                    }

                    if (bid.getUpdatedAt() == null) {
                        bid.setUpdatedAt(
                                LocalDateTime.now()
                        );
                    }

                    return bid;
                });

        when(gatekeeperService.evaluate(request))
                .thenReturn(decision);

        when(
                complianceRepository.save(
                        any(BidComplianceResult.class)
                )
        ).thenAnswer(invocation -> {
            BidComplianceResult complianceResult =
                    invocation.getArgument(0);

            complianceResult.setId(
                    complianceResultId
            );

            return complianceResult;
        });

        BidResponse response =
                bidService.submitBid(
                        TENDER_ID,
                        request
                );

        assertEquals(
                replacementBidId,
                response.getId()
        );

        assertEquals(
                TENDER_ID,
                response.getTenderId()
        );

        assertEquals(
                TRADER_PROFILE_ID,
                response.getTraderId()
        );

        assertEquals(
                "Bidder C",
                response.getBidderAlias()
        );

        assertEquals(
                Bid.BidStatus.COMPLIANCE_FAILED,
                response.getStatus()
        );

        assertNotNull(
                response.getCompliance()
        );

        assertEquals(
                complianceResultId,
                response.getCompliance().getId()
        );

        assertEquals(
                replacementBidId,
                response.getCompliance().getBidId()
        );

        assertFalse(
                response.getCompliance().isPassed()
        );

        assertFalse(
                response.getCompliance().isTaxClearanceValid()
        );

        assertFalse(
                response.getCompliance()
                        .isRequiredDocumentsUploaded()
        );

        assertEquals(
                failureReason,
                response.getCompliance().getFailureReason()
        );

        ArgumentCaptor<Bid> bidCaptor =
                ArgumentCaptor.forClass(Bid.class);

        verify(
                bidRepository,
                times(2)
        ).save(
                bidCaptor.capture()
        );

        Bid savedBid =
                bidCaptor.getAllValues().get(1);

        assertEquals(
                replacementBidId,
                savedBid.getId()
        );

        assertEquals(
                TRADER_PROFILE_ID,
                savedBid.getTraderProfileId()
        );

        assertEquals(
                CURRENT_USER_ID,
                savedBid.getSubmittedByUserId()
        );

        assertEquals(
                "Bidder C",
                savedBid.getBidderAlias()
        );

        assertEquals(
                Bid.BidStatus.COMPLIANCE_FAILED,
                savedBid.getStatus()
        );

        ArgumentCaptor<BidComplianceResult> complianceCaptor =
                ArgumentCaptor.forClass(
                        BidComplianceResult.class
                );

        verify(complianceRepository)
                .save(
                        complianceCaptor.capture()
                );

        BidComplianceResult savedCompliance =
                complianceCaptor.getValue();

        assertEquals(
                replacementBidId,
                savedCompliance.getBidId()
        );

        assertTrue(
                savedCompliance.isCsdValid()
        );

        assertFalse(
                savedCompliance.isTaxClearanceValid()
        );

        assertTrue(
                savedCompliance.isBbbeeValid()
        );

        assertFalse(
                savedCompliance.isRequiredDocumentsUploaded()
        );

        assertFalse(
                savedCompliance.isPassed()
        );

        assertEquals(
                failureReason,
                savedCompliance.getFailureReason()
        );

        verify(gatekeeperService)
                .evaluate(request);

        verify(auditService)
                .recordSuccess(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_SUBMITTED
                        ),
                        eq(TENDER_ID),
                        eq(replacementBidId),
                        eq(CURRENT_USER_ID),
                        eq("Bid submitted"),
                        anyString()
                );

        verify(auditService)
                .recordFailure(
                        eq(
                                ProcurementAuditEvent
                                        .AuditEventType
                                        .BID_COMPLIANCE_FAILED
                        ),
                        eq(TENDER_ID),
                        eq(replacementBidId),
                        eq(CURRENT_USER_ID),
                        eq("Bid compliance failed"),
                        eq(failureReason)
                );

        verify(procurementNotificationService)
                .queueBidReceived(
                        savedBid,
                        publishedTender
                );

        verify(procurementNotificationService)
                .queueComplianceFailed(
                        savedBid,
                        publishedTender,
                        failureReason
                );

        verify(
                procurementNotificationService,
                never()
        ).queueCompliancePassed(
                any(Bid.class),
                any(Tender.class)
        );
    }

    private SubmitBidRequest validSubmitBidRequest() {
        SubmitBidRequest request =
                new SubmitBidRequest();

        request.setTechnicalProposal(
                "Technical proposal for delivery and commissioning."
        );

        request.setPriceAmount(
                new BigDecimal("690000.00")
        );

        request.setCsdValid(true);
        request.setTaxClearanceValid(true);
        request.setBbbeeValid(true);
        request.setRequiredDocumentsUploaded(true);

        return request;
    }
}