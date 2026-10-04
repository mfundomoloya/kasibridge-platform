package com.kasibridge.procurement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.procurement.service.WhatsAppWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhooks/whatsapp")
@Slf4j
public class WhatsAppWebhookController {

    @Value("${kasibridge.notifications.whatsapp.webhook.verify-token:}")
    private String configuredVerifyToken;

    private final ObjectMapper objectMapper;
    private final WhatsAppWebhookService webhookService;

    @Value("${kasibridge.notifications.whatsapp.webhook.app-secret:}")
    private String appSecret;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(@RequestParam(name = "hub.mode") String mode,
                                                @RequestParam(name = "hub.challenge") String challenge,
                                                @RequestParam(name = "hub.verify_token") String suppliedVerifyToken) {

        boolean subscribeRequest = "subscribe".equals(mode);

        boolean validToken =
                configuredVerifyToken != null
                        && !configuredVerifyToken.isBlank()
                        && suppliedVerifyToken != null
                        && MessageDigest.isEqual(
                        configuredVerifyToken.getBytes(StandardCharsets.UTF_8),
                        suppliedVerifyToken.getBytes(StandardCharsets.UTF_8)
                );

        if (!subscribeRequest || !validToken) {
            log.warn(
                    "WhatsApp webhook verification rejected: mode={}",
                    mode
            );

            return ResponseEntity
                    .status(403)
                    .build();
        }

        if (challenge == null || challenge.isBlank()) {
            log.warn(
                    "WhatsApp webhook verification rejected "
                            + "because the challenge is blank."
            );

            return ResponseEntity
                    .badRequest()
                    .build();
        }

        log.info("WhatsApp webhook verification succeeded.");

        return ResponseEntity.ok(challenge);
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(
                    name = "X-Hub-Signature-256",
                    required = false
            )
            String suppliedSignature,

            @RequestBody
            byte[] rawBody
    ) {
        if (rawBody == null || rawBody.length == 0) {
            log.warn(
                    "WhatsApp webhook rejected "
                            + "because the request body is empty."
            );

            return ResponseEntity
                    .badRequest()
                    .build();
        }

        if (!isValidSignature(
                suppliedSignature,
                rawBody
        )) {
            log.warn(
                    "WhatsApp webhook rejected "
                            + "because the signature is invalid."
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        try {
            JsonNode payload =
                    objectMapper.readTree(rawBody);

            if (payload == null || payload.isNull()) {
                log.warn(
                        "WhatsApp webhook payload is empty."
                );

                return ResponseEntity
                        .badRequest()
                        .build();
            }

            webhookService.processWebhook(payload);

            return ResponseEntity.ok().build();

        } catch (IOException exception) {
            log.warn(
                    "WhatsApp webhook contained invalid JSON: "
                            + "errorType={}",
                    exception.getClass().getSimpleName()
            );

            return ResponseEntity
                    .badRequest()
                    .build();

        } catch (RuntimeException exception) {
            log.error(
                    "WhatsApp webhook processing failed: "
                            + "errorType={}",
                    exception.getClass().getSimpleName(),
                    exception
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }


    private boolean isValidSignature(String suppliedSignature, byte[] rawBody) {
        if (appSecret == null
                || appSecret.isBlank()
                || suppliedSignature == null
                || !suppliedSignature.startsWith("sha256=")) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(
                    new SecretKeySpec(
                            appSecret.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            "HmacSHA256"
                    )
            );

            String expectedSignature =
                    "sha256="
                            + HexFormat.of().formatHex(
                            mac.doFinal(rawBody)
                    );

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(
                            StandardCharsets.UTF_8
                    ),
                    suppliedSignature.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

        } catch (Exception ex) {
            log.error(
                    "Unable to validate WhatsApp webhook signature.",
                    ex
            );

            return false;
        }
    }
}