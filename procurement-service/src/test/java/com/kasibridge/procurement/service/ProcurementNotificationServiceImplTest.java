package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.TraderProfileClientResponse;
import com.kasibridge.procurement.dto.WhatsAppTemplateContext;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.repository.TenderCommitteeAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private NotificationOutboxService notificationOutboxService;

    @Mock
    private WhatsAppMessageTemplateService templateService;

    @Mock
    private TenderCommitteeAssignmentRepository assignmentRepository;

    @InjectMocks
    private ProcurementNotificationServiceImpl notificationService;

    private Bid withdrawnBid;
    private Tender tender;
    private TraderProfileClientResponse trader;

    @BeforeEach
    void setUp() {
        withdrawnBid = Bid.builder()
                .id(BID_ID)
                .bidReference("KB-BID-68742A06")
                .tenderId(TENDER_ID)
                .traderProfileId(TRADER_PROFILE_ID)
                .submittedByUserId(USER_ID)
                .bidderAlias("Bidder B")
                .technicalProposal(
                        "Technical proposal for delivery and commissioning."
                )
                .priceAmount(
                        new BigDecimal("690000.00")
                )
                .status(Bid.BidStatus.WITHDRAWN)
                .build();

        tender = Tender.builder()
                .id(TENDER_ID)
                .tenderReference("KB-TENDER-436FC79F")
                .title(
                        "Supply of ICT Equipment for Municipal Offices"
                )
                .status(Tender.TenderStatus.PUBLISHED)
                .build();

        trader = new TraderProfileClientResponse();
        trader.setId(TRADER_PROFILE_ID);
        trader.setUserId(USER_ID);
        trader.setFullName("Sifiso Magagula");
        trader.setPhoneNumber("+27610000001");
        trader.setEmail("biddera@kasibridge.test");
    }

    @Test
    void shouldQueueBidWithdrawnNotification() {
        String expectedMessage =
                "Hi Sifiso Magagula, your bid "
                        + "KB-BID-68742A06 for tender "
                        + "KB-TENDER-436FC79F has been "
                        + "withdrawn successfully. "
                        + "The withdrawn bid will remain available "
                        + "in your KasiBridge bid history.";

        when(
                traderProfileClient.getTraderProfileByIdAsSystem(
                        TRADER_PROFILE_ID
                )
        ).thenReturn(trader);

        when(
                templateService.generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN
                        ),
                        any(WhatsAppTemplateContext.class)
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

        ArgumentCaptor<WhatsAppTemplateContext> contextCaptor =
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
                "Supply of ICT Equipment for Municipal Offices",
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
    void shouldUseSystemClientToLoadTraderProfile() {
        when(
                traderProfileClient.getTraderProfileByIdAsSystem(
                        TRADER_PROFILE_ID
                )
        ).thenReturn(trader);

        when(
                templateService.generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN
                        ),
                        any(WhatsAppTemplateContext.class)
                )
        ).thenReturn("Withdrawal notification message");

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
    void shouldNotQueueNotificationWhenTraderLookupFails() {
        when(
                traderProfileClient.getTraderProfileByIdAsSystem(
                        TRADER_PROFILE_ID
                )
        ).thenThrow(
                new IllegalStateException(
                        "Trader profile service unavailable"
                )
        );

        notificationService.queueBidWithdrawn(
                withdrawnBid,
                tender
        );

        verify(
                templateService,
                never()
        ).generateMessage(
                any(
                        NotificationOutbox
                                .NotificationTemplateType.class
                ),
                any(WhatsAppTemplateContext.class)
        );

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

    @Test
    void shouldNotQueueNotificationWhenBidHasNoTraderProfileId() {
        withdrawnBid.setTraderProfileId(null);

        notificationService.queueBidWithdrawn(
                withdrawnBid,
                tender
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
                any(WhatsAppTemplateContext.class)
        );

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

    @Test
    void shouldNotQueueNotificationWhenBidIsNull() {
        notificationService.queueBidWithdrawn(
                null,
                tender
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
                any(WhatsAppTemplateContext.class)
        );

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

    @Test
    void shouldNotQueueOutboxRecordWhenTemplateGenerationFails() {
        when(
                traderProfileClient.getTraderProfileByIdAsSystem(
                        TRADER_PROFILE_ID
                )
        ).thenReturn(trader);

        when(
                templateService.generateMessage(
                        eq(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN
                        ),
                        any(WhatsAppTemplateContext.class)
                )
        ).thenThrow(
                new IllegalStateException(
                        "Unable to generate message"
                )
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