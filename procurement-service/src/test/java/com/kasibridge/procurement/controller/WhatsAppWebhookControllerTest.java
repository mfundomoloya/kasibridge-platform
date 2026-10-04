package com.kasibridge.procurement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.procurement.service.WhatsAppWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WhatsAppWebhookControllerTest {

    private static final String APP_SECRET =
            "test-whatsapp-app-secret";

    private ObjectMapper objectMapper;
    private WhatsAppWebhookService webhookService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        webhookService = mock(WhatsAppWebhookService.class);

        WhatsAppWebhookController controller =
                new WhatsAppWebhookController(
                        objectMapper,
                        webhookService
                );

        ReflectionTestUtils.setField(
                controller,
                "appSecret",
                APP_SECRET
        );

        ReflectionTestUtils.setField(
                controller,
                "configuredVerifyToken",
                "test-verify-token"
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void receiveWebhookWithValidSignatureReturnsOk()
            throws Exception {

        byte[] rawBody = """
                {
                  "object": "whatsapp_business_account",
                  "entry": []
                }
                """.getBytes(StandardCharsets.UTF_8);

        String signature = generateSignature(
                APP_SECRET,
                rawBody
        );

        mockMvc.perform(
                        post("/api/v1/webhooks/whatsapp")
                                .contentType("application/json")
                                .header(
                                        "X-Hub-Signature-256",
                                        signature
                                )
                                .content(rawBody)
                )
                .andExpect(status().isOk());

        verify(webhookService)
                .processWebhook(any(JsonNode.class));
    }

    @Test
    void receiveWebhookWithInvalidSignatureReturnsUnauthorized()
            throws Exception {

        byte[] rawBody = """
                {
                  "object": "whatsapp_business_account",
                  "entry": []
                }
                """.getBytes(StandardCharsets.UTF_8);

        String invalidSignature =
                "sha256="
                        + "0".repeat(64);

        mockMvc.perform(
                        post("/api/v1/webhooks/whatsapp")
                                .contentType("application/json")
                                .header(
                                        "X-Hub-Signature-256",
                                        invalidSignature
                                )
                                .content(rawBody)
                )
                .andExpect(status().isUnauthorized());

        verify(webhookService, never())
                .processWebhook(any(JsonNode.class));
    }

    @Test
    void receiveWebhookWithoutSignatureReturnsUnauthorized()
            throws Exception {

        byte[] rawBody = """
                {
                  "object": "whatsapp_business_account",
                  "entry": []
                }
                """.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(
                        post("/api/v1/webhooks/whatsapp")
                                .contentType("application/json")
                                .content(rawBody)
                )
                .andExpect(status().isUnauthorized());

        verify(webhookService, never())
                .processWebhook(any(JsonNode.class));
    }

    @Test
    void receiveWebhookWithMalformedJsonReturnsBadRequest()
            throws Exception {

        byte[] rawBody =
                "{ invalid-json"
                        .getBytes(StandardCharsets.UTF_8);

        String signature = generateSignature(
                APP_SECRET,
                rawBody
        );

        mockMvc.perform(
                        post("/api/v1/webhooks/whatsapp")
                                .contentType("application/json")
                                .header(
                                        "X-Hub-Signature-256",
                                        signature
                                )
                                .content(rawBody)
                )
                .andExpect(status().isBadRequest());

        verify(webhookService, never())
                .processWebhook(any(JsonNode.class));
    }


    @Test
    void verifyWebhookWithValidTokenReturnsChallenge()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/webhooks/whatsapp")
                                .queryParam(
                                        "hub.mode",
                                        "subscribe"
                                )
                                .queryParam(
                                        "hub.challenge",
                                        "123456789"
                                )
                                .queryParam(
                                        "hub.verify_token",
                                        "test-verify-token"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().string("123456789"));
    }

    @Test
    void verifyWebhookWithInvalidTokenReturnsForbidden()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/webhooks/whatsapp")
                                .queryParam(
                                        "hub.mode",
                                        "subscribe"
                                )
                                .queryParam(
                                        "hub.challenge",
                                        "123456789"
                                )
                                .queryParam(
                                        "hub.verify_token",
                                        "incorrect-token"
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void verifyWebhookWithIncorrectModeReturnsForbidden()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/webhooks/whatsapp")
                                .queryParam(
                                        "hub.mode",
                                        "unsubscribe"
                                )
                                .queryParam(
                                        "hub.challenge",
                                        "123456789"
                                )
                                .queryParam(
                                        "hub.verify_token",
                                        "test-verify-token"
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void verifyWebhookWithBlankChallengeReturnsBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/webhooks/whatsapp")
                                .queryParam(
                                        "hub.mode",
                                        "subscribe"
                                )
                                .queryParam(
                                        "hub.challenge",
                                        ""
                                )
                                .queryParam(
                                        "hub.verify_token",
                                        "test-verify-token"
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void receiveWebhookWithEmptyBodyReturnsBadRequest()
            throws Exception {

        byte[] rawBody = new byte[0];

        String signature = generateSignature(
                APP_SECRET,
                rawBody
        );

        mockMvc.perform(
                        post("/api/v1/webhooks/whatsapp")
                                .contentType("application/json")
                                .header(
                                        "X-Hub-Signature-256",
                                        signature
                                )
                                .content(rawBody)
                )
                .andExpect(status().isBadRequest());

        verify(webhookService, never())
                .processWebhook(any(JsonNode.class));
    }

    @Test
    void receiveWebhookWhenServiceFailsReturnsInternalServerError()
            throws Exception {

        byte[] rawBody = """
            {
              "object": "whatsapp_business_account",
              "entry": []
            }
            """.getBytes(StandardCharsets.UTF_8);

        String signature = generateSignature(
                APP_SECRET,
                rawBody
        );

        doThrow(
                new IllegalStateException(
                        "Webhook processing failed."
                )
        )
                .when(webhookService)
                .processWebhook(any(JsonNode.class));

        mockMvc.perform(
                        post("/api/v1/webhooks/whatsapp")
                                .contentType("application/json")
                                .header(
                                        "X-Hub-Signature-256",
                                        signature
                                )
                                .content(rawBody)
                )
                .andExpect(
                        status().isInternalServerError()
                );

        verify(webhookService)
                .processWebhook(any(JsonNode.class));
    }

    @Test
    void verifyWebhookWithoutVerifyTokenReturnsBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/webhooks/whatsapp")
                                .queryParam(
                                        "hub.mode",
                                        "subscribe"
                                )
                                .queryParam(
                                        "hub.challenge",
                                        "123456789"
                                )
                )
                .andExpect(status().isBadRequest());
    }

    private String generateSignature(String secret, byte[] rawBody) throws Exception {

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(
                new SecretKeySpec(
                        secret.getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                )
        );

        return "sha256="
                + HexFormat.of().formatHex(
                        mac.doFinal(rawBody)
                );
    }
}
