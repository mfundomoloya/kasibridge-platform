package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.TraderProfileClientResponse;
import com.kasibridge.procurement.dto.WhatsAppTemplateContext;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import com.kasibridge.procurement.repository.TenderCommitteeAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcurementNotificationServiceImplTest {

    private static final Long USER_ID = 8L;

    private static final Long TRADER_PROFILE_ID = 2L;

    private static final Long TENDER_ID = 7L;

    private static final Long BID_ID = 16L;

    @Mock
    private TraderProfileClient traderProfileClient;

    @Mock
    private NotificationOutboxService
            notificationOutboxService;

    @Mock
    private WhatsAppMessageTemplateService
            templateService;

    @Mock
    private TenderCommitteeAssignmentRepository
            assignmentRepository;

    @Mock
    private NotificationOutboxRepository
            notificationOutboxRepository;

    @InjectMocks
    private ProcurementNotificationServiceImpl
            notificationService;

    private Bid withdrawnBid;

    private Tender tender;

    private TraderProfileClientResponse trader;

    @BeforeEach
    void setUp() {
        withdrawnBid =
                Bid.builder()
                        .id(BID_ID)
                        .bidReference(
                                "KB-BID-68742A06"
                        )
                        .tenderId(TENDER_ID)
                        .traderProfileId(
                                TRADER_PROFILE_ID
                        )
                        .submittedByUserId(USER_ID)
                        .bidderAlias("Bidder B")
                        .technicalProposal(
                                "Technical proposal for "
                                        + "delivery and "
                                        + "commissioning."
                        )
                        .priceAmount(
                                new BigDecimal(
                                        "690000.00"
                                )
                        )
                        .status(
                                Bid.BidStatus.WITHDRAWN
                        )
                        .build();

        tender =
                Tender.builder()
                        .id(TENDER_ID)
                        .tenderReference(
                                "KB-TENDER-436FC79F"
                        )
                        .title(
                                "Supply of ICT Equipment "
                                        + "for Municipal Offices"
                        )
                        .status(
                                Tender.TenderStatus.PUBLISHED
                        )
                        .requiredBusinessType(
                                "ICT Equipment Supply"
                        )
                        .serviceArea(
                                "Gauteng"
                        )
                        .build();

        trader =
                new TraderProfileClientResponse();

        trader.setId(TRADER_PROFILE_ID);
        trader.setUserId(USER_ID);
        trader.setFullName(
                "Sifiso Magagula"
        );
        trader.setPhoneNumber(
                "+27610000001"
        );
        trader.setEmail(
                "biddera@kasibridge.test"
        );
    }

    @Test
    void shouldQueueBidWithdrawnNotification() {
        String expectedMessage =
                "Hi Sifiso Magagula, your bid "
                        + "KB-BID-68742A06 for tender "
                        + "KB-TENDER-436FC79F has been "
                        + "withdrawn successfully. "
                        + "The withdrawn bid will remain "
                        + "available in your KasiBridge "
                        + "bid history.";

        when(
                traderProfileClient
                        .getTraderProfileByIdAsSystem(
                                TRADER_PROFILE_ID
                        )
        ).thenReturn(trader);

        when(
                notificationOutboxRepository
                        .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN,
                                BID_ID,
                                USER_ID
                        )
        ).thenReturn(false);

        when(
                templateService.generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN
                        ),
                        any(
                                WhatsAppTemplateContext.class
                        )
                )
        ).thenReturn(expectedMessage);

        notificationService.queueBidWithdrawn(
                withdrawnBid,
                tender
        );

        verify(traderProfileClient)
                .getTraderProfileByIdAsSystem(
                        TRADER_PROFILE_ID
                );

        verify(notificationOutboxRepository)
                .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_WITHDRAWN,
                        BID_ID,
                        USER_ID
                );

        ArgumentCaptor<WhatsAppTemplateContext>
                contextCaptor =
                ArgumentCaptor.forClass(
                        WhatsAppTemplateContext.class
                );

        verify(templateService)
                .generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN
                        ),
                        contextCaptor.capture()
                );

        WhatsAppTemplateContext capturedContext =
                contextCaptor.getValue();

        assertNotNull(capturedContext);

        assertEquals(
                "Sifiso Magagula",
                capturedContext.getRecipientName()
        );

        assertEquals(
                TENDER_ID,
                capturedContext.getTenderId()
        );

        assertEquals(
                "KB-TENDER-436FC79F",
                capturedContext.getTenderReference()
        );

        assertEquals(
                "Supply of ICT Equipment "
                        + "for Municipal Offices",
                capturedContext.getTenderTitle()
        );

        assertEquals(
                BID_ID,
                capturedContext.getBidId()
        );

        assertEquals(
                "KB-BID-68742A06",
                capturedContext.getBidReference()
        );

        assertEquals(
                "Bidder B",
                capturedContext.getBidderAlias()
        );

        assertEquals(
                new BigDecimal("690000.00"),
                capturedContext.getBidAmount()
        );

        verify(notificationOutboxService)
                .queueNotification(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP,
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_WITHDRAWN,
                        USER_ID,
                        "+27610000001",
                        "biddera@kasibridge.test",
                        expectedMessage,
                        TENDER_ID,
                        BID_ID,
                        null
                );
    }

    @Test
    void shouldQueueBidReceivedNotification() {
        withdrawnBid.setStatus(
                Bid.BidStatus.COMPLIANT
        );

        String expectedMessage =
                "Hi Sifiso Magagula, your bid "
                        + "KB-BID-68742A06 for tender "
                        + "KB-TENDER-436FC79F was received.";

        when(
                traderProfileClient
                        .getTraderProfileByIdAsSystem(
                                TRADER_PROFILE_ID
                        )
        ).thenReturn(trader);

        when(
                notificationOutboxRepository
                        .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_RECEIVED,
                                BID_ID,
                                USER_ID
                        )
        ).thenReturn(false);

        when(
                templateService.generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_RECEIVED
                        ),
                        any(
                                WhatsAppTemplateContext.class
                        )
                )
        ).thenReturn(expectedMessage);

        notificationService.queueBidReceived(
                withdrawnBid,
                tender
        );

        verify(notificationOutboxRepository)
                .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED,
                        BID_ID,
                        USER_ID
                );

        ArgumentCaptor<WhatsAppTemplateContext>
                contextCaptor =
                ArgumentCaptor.forClass(
                        WhatsAppTemplateContext.class
                );

        verify(templateService)
                .generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_RECEIVED
                        ),
                        contextCaptor.capture()
                );

        WhatsAppTemplateContext capturedContext =
                contextCaptor.getValue();

        assertNotNull(capturedContext);

        assertEquals(
                "Sifiso Magagula",
                capturedContext.getRecipientName()
        );

        assertEquals(
                TENDER_ID,
                capturedContext.getTenderId()
        );

        assertEquals(
                "KB-TENDER-436FC79F",
                capturedContext.getTenderReference()
        );

        assertEquals(
                BID_ID,
                capturedContext.getBidId()
        );

        assertEquals(
                "KB-BID-68742A06",
                capturedContext.getBidReference()
        );

        assertEquals(
                "Bidder B",
                capturedContext.getBidderAlias()
        );

        assertEquals(
                new BigDecimal("690000.00"),
                capturedContext.getBidAmount()
        );

        verify(notificationOutboxService)
                .queueNotification(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP,
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED,
                        USER_ID,
                        "+27610000001",
                        "biddera@kasibridge.test",
                        expectedMessage,
                        TENDER_ID,
                        BID_ID,
                        null
                );
    }

    @Test
    void shouldSkipDuplicateBidReceivedNotification() {
        withdrawnBid.setStatus(
                Bid.BidStatus.COMPLIANT
        );

        when(
                traderProfileClient
                        .getTraderProfileByIdAsSystem(
                                TRADER_PROFILE_ID
                        )
        ).thenReturn(trader);

        when(
                notificationOutboxRepository
                        .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_RECEIVED,
                                BID_ID,
                                USER_ID
                        )
        ).thenReturn(true);

        notificationService.queueBidReceived(
                withdrawnBid,
                tender
        );

        verify(notificationOutboxRepository)
                .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED,
                        BID_ID,
                        USER_ID
                );

        verify(
                templateService,
                never()
        ).generateMessage(
                any(
                        NotificationOutbox
                                .NotificationTemplateType.class
                ),
                any(
                        WhatsAppTemplateContext.class
                )
        );

        verifyNoOutboxNotificationQueued();
    }

    @Test
    void shouldSkipDuplicateBidWithdrawnNotification() {
        when(
                traderProfileClient
                        .getTraderProfileByIdAsSystem(
                                TRADER_PROFILE_ID
                        )
        ).thenReturn(trader);

        when(
                notificationOutboxRepository
                        .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN,
                                BID_ID,
                                USER_ID
                        )
        ).thenReturn(true);

        notificationService.queueBidWithdrawn(
                withdrawnBid,
                tender
        );

        verify(notificationOutboxRepository)
                .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_WITHDRAWN,
                        BID_ID,
                        USER_ID
                );

        verify(
                templateService,
                never()
        ).generateMessage(
                any(
                        NotificationOutbox
                                .NotificationTemplateType.class
                ),
                any(
                        WhatsAppTemplateContext.class
                )
        );

        verifyNoOutboxNotificationQueued();
    }

    @Test
    void shouldUseSystemClientToLoadTraderProfile() {
        when(
                traderProfileClient
                        .getTraderProfileByIdAsSystem(
                                TRADER_PROFILE_ID
                        )
        ).thenReturn(trader);

        when(
                notificationOutboxRepository
                        .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN,
                                BID_ID,
                                USER_ID
                        )
        ).thenReturn(false);

        when(
                templateService.generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN
                        ),
                        any(
                                WhatsAppTemplateContext.class
                        )
                )
        ).thenReturn(
                "Withdrawal notification message"
        );

        notificationService.queueBidWithdrawn(
                withdrawnBid,
                tender
        );

        verify(traderProfileClient)
                .getTraderProfileByIdAsSystem(
                        TRADER_PROFILE_ID
                );

        verify(
                traderProfileClient,
                never()
        ).getTraderProfileById(
                TRADER_PROFILE_ID
        );
    }

    @Test
    void shouldPropagateTraderLookupFailure() {
        when(
                traderProfileClient
                        .getTraderProfileByIdAsSystem(
                                TRADER_PROFILE_ID
                        )
        ).thenThrow(
                new IllegalStateException(
                        "Trader profile service unavailable"
                )
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                notificationService
                                        .queueBidWithdrawn(
                                                withdrawnBid,
                                                tender
                                        )
                );

        assertEquals(
                "Trader profile service unavailable",
                exception.getMessage()
        );

        verify(
                templateService,
                never()
        ).generateMessage(
                any(
                        NotificationOutbox
                                .NotificationTemplateType.class
                ),
                any(
                        WhatsAppTemplateContext.class
                )
        );

        verifyNoOutboxNotificationQueued();
    }

    @Test
    void shouldRejectBidWithoutTraderProfileId() {
        withdrawnBid.setTraderProfileId(null);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                notificationService
                                        .queueBidWithdrawn(
                                                withdrawnBid,
                                                tender
                                        )
                );

        assertEquals(
                "Bid does not contain a trader profile ID.",
                exception.getMessage()
        );

        verify(
                traderProfileClient,
                never()
        ).getTraderProfileByIdAsSystem(
                any()
        );

        verify(
                templateService,
                never()
        ).generateMessage(
                any(
                        NotificationOutbox
                                .NotificationTemplateType.class
                ),
                any(
                        WhatsAppTemplateContext.class
                )
        );

        verifyNoOutboxNotificationQueued();
    }

    @Test
    void shouldRejectNullBid() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                notificationService
                                        .queueBidWithdrawn(
                                                null,
                                                tender
                                        )
                );

        assertEquals(
                "Bid is required to load the trader profile.",
                exception.getMessage()
        );

        verify(
                traderProfileClient,
                never()
        ).getTraderProfileByIdAsSystem(
                any()
        );

        verify(
                templateService,
                never()
        ).generateMessage(
                any(
                        NotificationOutbox
                                .NotificationTemplateType.class
                ),
                any(
                        WhatsAppTemplateContext.class
                )
        );

        verifyNoOutboxNotificationQueued();
    }

    @Test
    void shouldPropagateTemplateGenerationFailure() {
        when(
                traderProfileClient
                        .getTraderProfileByIdAsSystem(
                                TRADER_PROFILE_ID
                        )
        ).thenReturn(trader);

        when(
                notificationOutboxRepository
                        .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN,
                                BID_ID,
                                USER_ID
                        )
        ).thenReturn(false);

        when(
                templateService.generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN
                        ),
                        any(
                                WhatsAppTemplateContext.class
                        )
                )
        ).thenThrow(
                new IllegalStateException(
                        "Unable to generate message"
                )
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                notificationService
                                        .queueBidWithdrawn(
                                                withdrawnBid,
                                                tender
                                        )
                );

        assertEquals(
                "Unable to generate message",
                exception.getMessage()
        );

        verify(traderProfileClient)
                .getTraderProfileByIdAsSystem(
                        TRADER_PROFILE_ID
                );

        verifyNoOutboxNotificationQueued();
    }

    @Test
    void shouldPropagateBidReceivedOutboxFailure() {
        withdrawnBid.setStatus(
                Bid.BidStatus.COMPLIANT
        );

        when(
                traderProfileClient
                        .getTraderProfileByIdAsSystem(
                                TRADER_PROFILE_ID
                        )
        ).thenReturn(trader);

        when(
                notificationOutboxRepository
                        .existsByTemplateTypeAndRelatedBidIdAndRecipientUserId(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_RECEIVED,
                                BID_ID,
                                USER_ID
                        )
        ).thenReturn(false);

        when(
                templateService.generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_RECEIVED
                        ),
                        any(
                                WhatsAppTemplateContext.class
                        )
                )
        ).thenReturn(
                "Your bid was received."
        );

        when(
                notificationOutboxService
                        .queueNotification(
                                any(
                                        NotificationOutbox
                                                .NotificationChannel
                                                .class
                                ),
                                any(
                                        NotificationOutbox
                                                .NotificationTemplateType
                                                .class
                                ),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any()
                        )
        ).thenThrow(
                new IllegalStateException(
                        "Notification outbox insert failed"
                )
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                notificationService
                                        .queueBidReceived(
                                                withdrawnBid,
                                                tender
                                        )
                );

        assertEquals(
                "Notification outbox insert failed",
                exception.getMessage()
        );

        verify(notificationOutboxService)
                .queueNotification(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP,
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_RECEIVED,
                        USER_ID,
                        "+27610000001",
                        "biddera@kasibridge.test",
                        "Your bid was received.",
                        TENDER_ID,
                        BID_ID,
                        null
                );
    }

    @Test
    void shouldQueueMatchingTenderPublishedNotification() {
        tender.setRequiredBusinessType(
                "ICT Equipment Supply"
        );

        tender.setServiceArea(
                "Gauteng"
        );

        String expectedMessage =
                "A matching tender was published.";

        when(
                traderProfileClient
                        .findActiveMatchingProfilesAsSystem(
                                "ICT Equipment Supply",
                                "Gauteng"
                        )
        ).thenReturn(
                List.of(trader)
        );

        when(
                notificationOutboxRepository
                        .existsByTemplateTypeAndRelatedTenderIdAndRecipientUserId(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED,
                                TENDER_ID,
                                USER_ID
                        )
        ).thenReturn(false);

        when(
                templateService.generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED
                        ),
                        any(
                                WhatsAppTemplateContext.class
                        )
                )
        ).thenReturn(expectedMessage);

        notificationService
                .queueMatchingTenderPublished(
                        tender
                );

        ArgumentCaptor<WhatsAppTemplateContext>
                contextCaptor =
                ArgumentCaptor.forClass(
                        WhatsAppTemplateContext.class
                );

        verify(templateService)
                .generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED
                        ),
                        contextCaptor.capture()
                );

        WhatsAppTemplateContext context =
                contextCaptor.getValue();

        assertEquals(
                "Sifiso Magagula",
                context.getRecipientName()
        );

        assertEquals(
                TENDER_ID,
                context.getTenderId()
        );

        assertEquals(
                "KB-TENDER-436FC79F",
                context.getTenderReference()
        );

        assertEquals(
                "Supply of ICT Equipment "
                        + "for Municipal Offices",
                context.getTenderTitle()
        );

        verify(notificationOutboxService)
                .queueNotification(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP,
                        NotificationOutbox
                                .NotificationTemplateType
                                .MATCHING_TENDER_PUBLISHED,
                        USER_ID,
                        "+27610000001",
                        "biddera@kasibridge.test",
                        expectedMessage,
                        TENDER_ID,
                        null,
                        null
                );
    }

    @Test
    void shouldDeduplicateMatchingTradersByUserId() {
        tender.setRequiredBusinessType(
                "ICT Equipment Supply"
        );

        tender.setServiceArea(
                "Gauteng"
        );

        TraderProfileClientResponse duplicateTrader =
                new TraderProfileClientResponse();

        duplicateTrader.setId(99L);
        duplicateTrader.setUserId(USER_ID);
        duplicateTrader.setFullName(
                "Duplicate Profile"
        );
        duplicateTrader.setPhoneNumber(
                "+27610000099"
        );
        duplicateTrader.setEmail(
                "duplicate@kasibridge.test"
        );

        when(
                traderProfileClient
                        .findActiveMatchingProfilesAsSystem(
                                "ICT Equipment Supply",
                                "Gauteng"
                        )
        ).thenReturn(
                List.of(
                        trader,
                        duplicateTrader
                )
        );

        when(
                notificationOutboxRepository
                        .existsByTemplateTypeAndRelatedTenderIdAndRecipientUserId(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED,
                                TENDER_ID,
                                USER_ID
                        )
        ).thenReturn(false);

        when(
                templateService.generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED
                        ),
                        any(
                                WhatsAppTemplateContext.class
                        )
                )
        ).thenReturn(
                "A matching tender was published."
        );

        notificationService
                .queueMatchingTenderPublished(
                        tender
                );

        verify(
                notificationOutboxService
        ).queueNotification(
                NotificationOutbox
                        .NotificationChannel
                        .WHATSAPP,
                NotificationOutbox
                        .NotificationTemplateType
                        .MATCHING_TENDER_PUBLISHED,
                USER_ID,
                "+27610000001",
                "biddera@kasibridge.test",
                "A matching tender was published.",
                TENDER_ID,
                null,
                null
        );
    }

    @Test
    void shouldSkipAlreadyQueuedMatchingTenderNotification() {
        tender.setRequiredBusinessType(
                "ICT Equipment Supply"
        );

        tender.setServiceArea(
                "Gauteng"
        );

        when(
                traderProfileClient
                        .findActiveMatchingProfilesAsSystem(
                                "ICT Equipment Supply",
                                "Gauteng"
                        )
        ).thenReturn(
                List.of(trader)
        );

        when(
                notificationOutboxRepository
                        .existsByTemplateTypeAndRelatedTenderIdAndRecipientUserId(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED,
                                TENDER_ID,
                                USER_ID
                        )
        ).thenReturn(true);

        notificationService
                .queueMatchingTenderPublished(
                        tender
                );

        verify(
                templateService,
                never()
        ).generateMessage(
                any(),
                any()
        );

        verifyNoOutboxNotificationQueued();
    }

    @Test
    void shouldSkipMatchingTraderWithoutUsablePhoneNumber() {
        tender.setRequiredBusinessType(
                "ICT Equipment Supply"
        );

        tender.setServiceArea(
                "Gauteng"
        );

        trader.setPhoneNumber("   ");

        when(
                traderProfileClient
                        .findActiveMatchingProfilesAsSystem(
                                "ICT Equipment Supply",
                                "Gauteng"
                        )
        ).thenReturn(
                List.of(trader)
        );

        notificationService
                .queueMatchingTenderPublished(
                        tender
                );

        verify(
                notificationOutboxRepository,
                never()
        ).existsByTemplateTypeAndRelatedTenderIdAndRecipientUserId(
                any(),
                any(),
                any()
        );

        verify(
                templateService,
                never()
        ).generateMessage(
                any(),
                any()
        );

        verifyNoOutboxNotificationQueued();
    }

    @Test
    void shouldPropagateMatchingTraderLookupFailure() {
        tender.setRequiredBusinessType(
                "ICT Equipment Supply"
        );

        tender.setServiceArea(
                "Gauteng"
        );

        when(
                traderProfileClient
                        .findActiveMatchingProfilesAsSystem(
                                "ICT Equipment Supply",
                                "Gauteng"
                        )
        ).thenThrow(
                new IllegalStateException(
                        "Matching trader lookup failed"
                )
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                notificationService
                                        .queueMatchingTenderPublished(
                                                tender
                                        )
                );

        assertEquals(
                "Matching trader lookup failed",
                exception.getMessage()
        );

        verifyNoOutboxNotificationQueued();
    }

    private void verifyNoOutboxNotificationQueued() {
        verify(
                notificationOutboxService,
                never()
        ).queueNotification(
                any(
                        NotificationOutbox
                                .NotificationChannel.class
                ),
                any(
                        NotificationOutbox
                                .NotificationTemplateType.class
                ),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }
}