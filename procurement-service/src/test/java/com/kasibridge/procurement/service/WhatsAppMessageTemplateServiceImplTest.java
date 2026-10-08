package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.WhatsAppTemplateContext;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.exception.NotificationOutboxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WhatsAppMessageTemplateServiceImplTest {

    private WhatsAppMessageTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService =
                new WhatsAppMessageTemplateServiceImpl();
    }

    @Test
    void shouldGenerateBidWithdrawnMessage() {
        WhatsAppTemplateContext context =
                WhatsAppTemplateContext.builder()
                        .recipientName("Sifiso Magagula")
                        .tenderId(7L)
                        .tenderReference(
                                "KB-TENDER-436FC79F"
                        )
                        .tenderTitle(
                                "Supply of ICT Equipment for Municipal Offices"
                        )
                        .bidId(16L)
                        .bidReference(
                                "KB-BID-68742A06"
                        )
                        .bidderAlias("Bidder B")
                        .build();

        String message =
                templateService.generateMessage(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_WITHDRAWN,
                        context
                );

        assertEquals(
                "Hi Sifiso Magagula, your bid "
                        + "KB-BID-68742A06 for tender "
                        + "KB-TENDER-436FC79F has been "
                        + "withdrawn successfully. "
                        + "The withdrawn bid will remain "
                        + "available in your KasiBridge bid history.",
                message
        );
    }

    @Test
    void shouldUseTenderIdWhenTenderReferenceIsMissing() {
        WhatsAppTemplateContext context =
                WhatsAppTemplateContext.builder()
                        .recipientName("Sifiso Magagula")
                        .tenderId(7L)
                        .bidId(16L)
                        .bidReference(
                                "KB-BID-68742A06"
                        )
                        .build();

        String message =
                templateService.generateMessage(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_WITHDRAWN,
                        context
                );

        assertEquals(
                "Hi Sifiso Magagula, your bid "
                        + "KB-BID-68742A06 for tender ID 7 "
                        + "has been withdrawn successfully. "
                        + "The withdrawn bid will remain "
                        + "available in your KasiBridge bid history.",
                message
        );
    }

    @Test
    void shouldUseFallbackGreetingWhenRecipientNameIsMissing() {
        WhatsAppTemplateContext context =
                WhatsAppTemplateContext.builder()
                        .tenderId(7L)
                        .tenderReference(
                                "KB-TENDER-436FC79F"
                        )
                        .bidId(16L)
                        .bidReference(
                                "KB-BID-68742A06"
                        )
                        .build();

        String message =
                templateService.generateMessage(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_WITHDRAWN,
                        context
                );

        assertEquals(
                "Hi there, your bid "
                        + "KB-BID-68742A06 for tender "
                        + "KB-TENDER-436FC79F has been "
                        + "withdrawn successfully. "
                        + "The withdrawn bid will remain "
                        + "available in your KasiBridge bid history.",
                message
        );
    }

    @Test
    void shouldUseGenericTenderDescriptionWhenTenderDetailsAreMissing() {
        WhatsAppTemplateContext context =
                WhatsAppTemplateContext.builder()
                        .recipientName("Sifiso Magagula")
                        .bidId(16L)
                        .bidReference(
                                "KB-BID-68742A06"
                        )
                        .build();

        String message =
                templateService.generateMessage(
                        NotificationOutbox
                                .NotificationTemplateType
                                .BID_WITHDRAWN,
                        context
                );

        assertEquals(
                "Hi Sifiso Magagula, your bid "
                        + "KB-BID-68742A06 for the tender "
                        + "has been withdrawn successfully. "
                        + "The withdrawn bid will remain "
                        + "available in your KasiBridge bid history.",
                message
        );
    }

    @Test
    void shouldRejectBidWithdrawnMessageWithoutBidReference() {
        WhatsAppTemplateContext context =
                WhatsAppTemplateContext.builder()
                        .recipientName("Sifiso Magagula")
                        .tenderId(7L)
                        .tenderReference(
                                "KB-TENDER-436FC79F"
                        )
                        .bidId(16L)
                        .build();

        NotificationOutboxException exception =
                assertThrows(
                        NotificationOutboxException.class,
                        () -> templateService.generateMessage(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN,
                                context
                        )
                );

        assertEquals(
                "Bid reference is required for BID_WITHDRAWN.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectBlankBidReference() {
        WhatsAppTemplateContext context =
                WhatsAppTemplateContext.builder()
                        .recipientName("Sifiso Magagula")
                        .tenderId(7L)
                        .tenderReference(
                                "KB-TENDER-436FC79F"
                        )
                        .bidId(16L)
                        .bidReference("   ")
                        .build();

        NotificationOutboxException exception =
                assertThrows(
                        NotificationOutboxException.class,
                        () -> templateService.generateMessage(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN,
                                context
                        )
                );

        assertEquals(
                "Bid reference is required for BID_WITHDRAWN.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullTemplateType() {
        WhatsAppTemplateContext context =
                WhatsAppTemplateContext.builder()
                        .bidReference(
                                "KB-BID-68742A06"
                        )
                        .build();

        NotificationOutboxException exception =
                assertThrows(
                        NotificationOutboxException.class,
                        () -> templateService.generateMessage(
                                null,
                                context
                        )
                );

        assertEquals(
                "Notification template type is required.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullTemplateContext() {
        NotificationOutboxException exception =
                assertThrows(
                        NotificationOutboxException.class,
                        () -> templateService.generateMessage(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_WITHDRAWN,
                                null
                        )
                );

        assertEquals(
                "WhatsApp template context is required.",
                exception.getMessage()
        );
    }
}