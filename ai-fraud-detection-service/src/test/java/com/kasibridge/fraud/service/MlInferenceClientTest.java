package com.kasibridge.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.fraud.dto.MlInferenceRequest;
import com.kasibridge.fraud.dto.MlInferenceResponse;
import com.kasibridge.fraud.exception.MlInferenceException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MlInferenceClientTest {

    private HttpServer server;

    private MlInferenceClient client;

    private ObjectMapper objectMapper;

    private AtomicReference<String>
            capturedRequestBody;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper =
                new ObjectMapper();

        capturedRequestBody =
                new AtomicReference<>();

        server =
                HttpServer.create(
                        new InetSocketAddress(
                                "127.0.0.1",
                                0
                        ),
                        0
                );

        server.start();

        HttpClient httpClient =
                HttpClient
                        .newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(2)
                        )
                        .version(
                                HttpClient.Version.HTTP_1_1
                        )
                        .build();

        String baseUrl =
                "http://127.0.0.1:"
                        + server
                        .getAddress()
                        .getPort();

        client =
                new MlInferenceClient(
                        httpClient,
                        objectMapper,
                        baseUrl,
                        5000
                );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void predict_shouldSendJsonBodyAndMapHighRiskResponse()
            throws Exception {
        server.createContext(
                "/predict",
                exchange -> {
                    captureRequest(exchange);

                    sendJsonResponse(
                            exchange,
                            200,
                            """
                            {
                              "classification": "HIGH_RISK",
                              "risk_level": "CRITICAL",
                              "risk_probability": 0.964699,
                              "model_name": "Random Forest",
                              "model_version": "procurement-fraud-v1",
                              "experiment": "Hybrid full-signal",
                              "detection_source": "SUPERVISED_MODEL",
                              "recommended_action": "BLOCK_AND_ESCALATE",
                              "threshold": 0.5,
                              "model_sha256": "test-checksum"
                            }
                            """
                    );
                }
        );

        MlInferenceResponse response =
                client.predict(
                        createRequest()
                );

        assertNotNull(response);

        assertEquals(
                "HIGH_RISK",
                response.classification()
        );

        assertEquals(
                "CRITICAL",
                response.riskLevel()
        );

        assertEquals(
                0,
                response.riskProbability()
                        .compareTo(
                                new BigDecimal(
                                        "0.964699"
                                )
                        )
        );

        assertEquals(
                "Random Forest",
                response.modelName()
        );

        assertEquals(
                "procurement-fraud-v1",
                response.modelVersion()
        );

        assertEquals(
                "BLOCK_AND_ESCALATE",
                response.recommendedAction()
        );

        String requestBody =
                capturedRequestBody.get();

        assertNotNull(requestBody);

        assertTrue(
                requestBody.length() > 0
        );

        assertEquals(
                "SERVICES",
                objectMapper
                        .readTree(requestBody)
                        .get("invoice_type")
                        .asText()
        );

        assertEquals(
                1,
                objectMapper
                        .readTree(requestBody)
                        .get("split_invoice_flag")
                        .asInt()
        );

        assertEquals(
                "application/json",
                serverRequestContentType
        );
    }

    private String serverRequestContentType;

    @Test
    void predict_shouldAcceptNullImageTamperFlag()
            throws Exception {
        server.createContext(
                "/predict",
                exchange -> {
                    captureRequest(exchange);

                    sendJsonResponse(
                            exchange,
                            200,
                            """
                            {
                              "classification": "LEGITIMATE",
                              "risk_level": "LOW",
                              "risk_probability": 0.068023,
                              "model_name": "Random Forest",
                              "model_version": "procurement-fraud-v1",
                              "experiment": "Hybrid full-signal",
                              "detection_source": "SUPERVISED_MODEL",
                              "recommended_action": "ALLOW_WITH_MONITORING",
                              "threshold": 0.5,
                              "model_sha256": "test-checksum"
                            }
                            """
                    );
                }
        );

        MlInferenceRequest request =
                createRequestWithNullImageData();

        MlInferenceResponse response =
                client.predict(request);

        assertEquals(
                "LEGITIMATE",
                response.classification()
        );

        String requestBody =
                capturedRequestBody.get();

        assertTrue(
                objectMapper
                        .readTree(requestBody)
                        .get("image_tamper_flag")
                        .isNull()
        );
    }

    @Test
    void predict_shouldRejectInvalidProbability() {
        server.createContext(
                "/predict",
                exchange -> {
                    captureRequest(exchange);

                    sendJsonResponse(
                            exchange,
                            200,
                            """
                            {
                              "classification": "HIGH_RISK",
                              "risk_level": "CRITICAL",
                              "risk_probability": 1.25,
                              "model_name": "Random Forest",
                              "model_version": "procurement-fraud-v1",
                              "experiment": "Hybrid full-signal",
                              "detection_source": "SUPERVISED_MODEL",
                              "recommended_action": "BLOCK_AND_ESCALATE",
                              "threshold": 0.5,
                              "model_sha256": "test-checksum"
                            }
                            """
                    );
                }
        );

        MlInferenceException exception =
                assertThrows(
                        MlInferenceException.class,
                        () ->
                                client.predict(
                                        createRequest()
                                )
                );

        assertEquals(
                "ML inference response contains "
                        + "an invalid probability.",
                exception.getMessage()
        );
    }

    @Test
    void predict_shouldMapProviderFailure() {
        server.createContext(
                "/predict",
                exchange ->
                        sendJsonResponse(
                                exchange,
                                422,
                                """
                                {
                                  "detail": "Invalid model input"
                                }
                                """
                        )
        );

        MlInferenceException exception =
                assertThrows(
                        MlInferenceException.class,
                        () ->
                                client.predict(
                                        createRequest()
                                )
                );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "HTTP 422"
                        )
        );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "Invalid model input"
                        )
        );
    }

    @Test
    void predict_shouldRejectEmptyResponse() {
        server.createContext(
                "/predict",
                exchange ->
                        sendJsonResponse(
                                exchange,
                                200,
                                ""
                        )
        );

        MlInferenceException exception =
                assertThrows(
                        MlInferenceException.class,
                        () ->
                                client.predict(
                                        createRequest()
                                )
                );

        assertEquals(
                "ML inference service returned "
                        + "an empty response.",
                exception.getMessage()
        );
    }

    @Test
    void predict_shouldRejectNullRequest() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                client.predict(null)
                );

        assertEquals(
                "ML inference request is required.",
                exception.getMessage()
        );
    }

    private void captureRequest(
            HttpExchange exchange
    ) throws IOException {
        assertEquals(
                "POST",
                exchange
                        .getRequestMethod()
        );

        serverRequestContentType =
                exchange
                        .getRequestHeaders()
                        .getFirst(
                                "Content-Type"
                        );

        byte[] requestBytes =
                exchange
                        .getRequestBody()
                        .readAllBytes();

        capturedRequestBody.set(
                new String(
                        requestBytes,
                        StandardCharsets.UTF_8
                )
        );
    }

    private void sendJsonResponse(
            HttpExchange exchange,
            int statusCode,
            String responseBody
    ) throws IOException {
        byte[] responseBytes =
                responseBody.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange
                .getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json"
                );

        exchange.sendResponseHeaders(
                statusCode,
                responseBytes.length
        );

        exchange
                .getResponseBody()
                .write(responseBytes);

        exchange.close();
    }

    private MlInferenceRequest createRequest() {
        return new MlInferenceRequest(
                new BigDecimal("20346.26"),
                23,
                new BigDecimal("18"),
                new BigDecimal("4252.16"),
                new BigDecimal("3.4"),
                61,
                new BigDecimal("5590.25"),
                new BigDecimal("50000000"),
                new BigDecimal("20346.26"),
                10,
                5,
                24,
                1,
                1,
                1,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                1,
                1,
                new BigDecimal("0.82"),
                0,
                0,
                "NET30",
                "SERVICES",
                "ZA",
                "Gauteng"
        );
    }

    private MlInferenceRequest
    createRequestWithNullImageData() {
        return new MlInferenceRequest(
                new BigDecimal("2599.13"),
                6,
                new BigDecimal("2"),
                new BigDecimal("4252.16"),
                new BigDecimal("-0.4"),
                841,
                new BigDecimal("5590.25"),
                new BigDecimal("50000000"),
                null,
                10,
                6,
                8,
                1,
                0,
                0,
                null,
                null,
                null,
                0,
                0,
                0,
                new BigDecimal("0.24"),
                0,
                null,
                "NET30",
                "GOODS",
                "ZA",
                "Gauteng"
        );
    }
}