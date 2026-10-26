package com.kasibridge.procurement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.procurement.dto.WhatsAppDeliveryCommand;
import com.kasibridge.procurement.dto.WhatsAppDeliveryResult;
import com.kasibridge.procurement.entity.NotificationOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MetaWhatsAppProviderTest {

    private static final String ENDPOINT =
            "https://graph.facebook.com"
                    + "/v25.0"
                    + "/1426619213860134"
                    + "/messages";

    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private MetaWhatsAppProvider provider;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        restTemplate =
                new RestTemplate();

        mockServer =
                MockRestServiceServer
                        .bindTo(restTemplate)
                        .build();

        provider =
                new MetaWhatsAppProvider(
                        restTemplate
                );

        objectMapper =
                new ObjectMapper();

        ReflectionTestUtils.setField(
                provider,
                "graphApiBaseUrl",
                "https://graph.facebook.com"
        );

        ReflectionTestUtils.setField(
                provider,
                "apiVersion",
                "v25.0"
        );

        ReflectionTestUtils.setField(
                provider,
                "phoneNumberId",
                "1426619213860134"
        );

        ReflectionTestUtils.setField(
                provider,
                "accessToken",
                "test-access-token"
        );

        ReflectionTestUtils.setField(
                provider,
                "matchingTenderPublishedTemplateName",
                "matching_tender_published"
        );

        ReflectionTestUtils.setField(
                provider,
                "matchingTenderPublishedTemplateLanguage",
                "en_US"
        );
    }

    @Test
    void sendMessage_shouldSendMatchingTenderAsApprovedTemplate() {
        WhatsAppDeliveryCommand command =
                WhatsAppDeliveryCommand.builder()
                        .notificationId(126L)
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED
                        )
                        .recipientPhone(
                                "+27610000001"
                        )
                        .renderedMessage(
                                "Fallback matching-tender message."
                        )
                        .recipientName(
                                "Test Trader"
                        )
                        .tenderReference(
                                "KB-TENDER-EXAMPLE"
                        )
                        .tenderTitle(
                                "Road Rehabilitation "
                                        + "and Maintenance"
                        )
                        .build();

        mockServer.expect(
                        once(),
                        requestTo(ENDPOINT)
                )
                .andExpect(
                        method(
                                HttpMethod.POST
                        )
                )
                .andExpect(
                        header(
                                "Authorization",
                                "Bearer test-access-token"
                        )
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonBodyEquals(
                                """
                                {
                                  "messaging_product": "whatsapp",
                                  "recipient_type": "individual",
                                  "to": "27610000001",
                                  "type": "template",
                                  "template": {
                                    "name": "matching_tender_published",
                                    "language": {
                                      "code": "en_US"
                                    },
                                    "components": [
                                      {
                                        "type": "body",
                                        "parameters": [
                                          {
                                            "type": "text",
                                            "text": "Test Trader"
                                          },
                                          {
                                            "type": "text",
                                            "text": "KB-TENDER-EXAMPLE"
                                          },
                                          {
                                            "type": "text",
                                            "text": "Road Rehabilitation and Maintenance"
                                          }
                                        ]
                                      }
                                    ]
                                  }
                                }
                                """
                        )
                )
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "messaging_product": "whatsapp",
                                  "contacts": [
                                    {
                                      "input": "27610000001",
                                      "wa_id": "27610000001"
                                    }
                                  ],
                                  "messages": [
                                    {
                                      "id": "wamid.MATCHING001",
                                      "message_status": "accepted"
                                    }
                                  ]
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        WhatsAppDeliveryResult result =
                provider.sendMessage(
                        command
                );

        assertNotNull(result);

        assertTrue(
                result.successful()
        );

        assertEquals(
                "wamid.MATCHING001",
                result.providerMessageId()
        );

        assertNull(
                result.failureReason()
        );

        mockServer.verify();
    }

    @Test
    void sendMessage_shouldKeepLegacyNotificationAsTextMessage() {
        WhatsAppDeliveryCommand command =
                WhatsAppDeliveryCommand.builder()
                        .notificationId(127L)
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .SUPPORT_TICKET_CREATED
                        )
                        .recipientPhone(
                                "+27610000002"
                        )
                        .renderedMessage(
                                "Your support ticket "
                                        + "was created."
                        )
                        .build();

        mockServer.expect(
                        once(),
                        requestTo(ENDPOINT)
                )
                .andExpect(
                        method(
                                HttpMethod.POST
                        )
                )
                .andExpect(
                        header(
                                "Authorization",
                                "Bearer test-access-token"
                        )
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonBodyEquals(
                                """
                                {
                                  "messaging_product": "whatsapp",
                                  "recipient_type": "individual",
                                  "to": "27610000002",
                                  "type": "text",
                                  "text": {
                                    "preview_url": false,
                                    "body": "Your support ticket was created."
                                  }
                                }
                                """
                        )
                )
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "messaging_product": "whatsapp",
                                  "messages": [
                                    {
                                      "id": "wamid.LEGACY001"
                                    }
                                  ]
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        WhatsAppDeliveryResult result =
                provider.sendMessage(
                        command
                );

        assertNotNull(result);

        assertTrue(
                result.successful()
        );

        assertEquals(
                "wamid.LEGACY001",
                result.providerMessageId()
        );

        assertNull(
                result.failureReason()
        );

        mockServer.verify();
    }

    @Test
    void sendMessage_shouldRejectMatchingTenderWithoutRecipientName() {
        WhatsAppDeliveryCommand command =
                WhatsAppDeliveryCommand.builder()
                        .notificationId(128L)
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED
                        )
                        .recipientPhone(
                                "+27610000003"
                        )
                        .renderedMessage(
                                "Fallback message."
                        )
                        .recipientName(null)
                        .tenderReference(
                                "KB-TENDER-EXAMPLE"
                        )
                        .tenderTitle(
                                "Road Rehabilitation "
                                        + "and Maintenance"
                        )
                        .build();

        WhatsAppDeliveryResult result =
                provider.sendMessage(
                        command
                );

        assertNotNull(result);

        assertFalse(
                result.successful()
        );

        assertNull(
                result.providerMessageId()
        );

        assertEquals(
                "Recipient name is required for "
                        + "MATCHING_TENDER_PUBLISHED.",
                result.failureReason()
        );

        /*
         * No request was expected, so verify() also
         * proves that Meta was not called.
         */
        mockServer.verify();
    }

    @Test
    void sendMessage_shouldRejectMatchingTenderWithoutReference() {
        WhatsAppDeliveryCommand command =
                WhatsAppDeliveryCommand.builder()
                        .notificationId(129L)
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED
                        )
                        .recipientPhone(
                                "+27610000004"
                        )
                        .renderedMessage(
                                "Fallback message."
                        )
                        .recipientName(
                                "Test Trader"
                        )
                        .tenderReference("   ")
                        .tenderTitle(
                                "Road Rehabilitation "
                                        + "and Maintenance"
                        )
                        .build();

        WhatsAppDeliveryResult result =
                provider.sendMessage(
                        command
                );

        assertNotNull(result);

        assertFalse(
                result.successful()
        );

        assertNull(
                result.providerMessageId()
        );

        assertEquals(
                "Tender reference is required for "
                        + "MATCHING_TENDER_PUBLISHED.",
                result.failureReason()
        );

        mockServer.verify();
    }

    @Test
    void sendMessage_shouldRejectMatchingTenderWithoutTitle() {
        WhatsAppDeliveryCommand command =
                WhatsAppDeliveryCommand.builder()
                        .notificationId(130L)
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .MATCHING_TENDER_PUBLISHED
                        )
                        .recipientPhone(
                                "+27610000005"
                        )
                        .renderedMessage(
                                "Fallback message."
                        )
                        .recipientName(
                                "Test Trader"
                        )
                        .tenderReference(
                                "KB-TENDER-EXAMPLE"
                        )
                        .tenderTitle(null)
                        .build();

        WhatsAppDeliveryResult result =
                provider.sendMessage(
                        command
                );

        assertNotNull(result);

        assertFalse(
                result.successful()
        );

        assertNull(
                result.providerMessageId()
        );

        assertEquals(
                "Tender title is required for "
                        + "MATCHING_TENDER_PUBLISHED.",
                result.failureReason()
        );

        mockServer.verify();
    }

    @Test
    void sendMessage_shouldRejectNullDeliveryCommand() {
        WhatsAppDeliveryResult result =
                provider.sendMessage(
                        null
                );

        assertNotNull(result);

        assertFalse(
                result.successful()
        );

        assertNull(
                result.providerMessageId()
        );

        assertEquals(
                "WhatsApp delivery command is required.",
                result.failureReason()
        );

        mockServer.verify();
    }

    private RequestMatcher jsonBodyEquals(
            String expectedJson
    ) {
        return request -> {
            if (
                    !(request
                            instanceof MockClientHttpRequest
                            mockRequest)
            ) {
                throw new AssertionError(
                        "Expected MockClientHttpRequest "
                                + "but received "
                                + request
                                .getClass()
                                .getName()
                );
            }

            String actualBody =
                    mockRequest
                            .getBodyAsString();

            assertNotNull(
                    actualBody
            );

            assertFalse(
                    actualBody.isBlank(),
                    "Expected a non-empty JSON "
                            + "request body."
            );

            JsonNode expectedNode =
                    objectMapper.readTree(
                            expectedJson
                    );

            JsonNode actualNode =
                    objectMapper.readTree(
                            actualBody
                    );

            assertEquals(
                    expectedNode,
                    actualNode,
                    "The WhatsApp request JSON "
                            + "did not match the "
                            + "expected payload."
            );
        };
    }
}