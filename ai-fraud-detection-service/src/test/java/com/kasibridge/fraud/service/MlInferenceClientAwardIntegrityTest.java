package com.kasibridge.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kasibridge.fraud.dto.AwardIntegrityAssessmentRequest;
import com.kasibridge.fraud.dto.AwardIntegrityAssessmentResponse;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MlInferenceClientAwardIntegrityTest {

    private HttpServer server;

    private String baseUrl;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        server =
                HttpServer.create(
                        new InetSocketAddress(
                                "127.0.0.1",
                                0
                        ),
                        0
                );

        server.setExecutor(
                Executors.newCachedThreadPool()
        );

        server.start();

        baseUrl =
                "http://127.0.0.1:"
                        + server
                        .getAddress()
                        .getPort();

        objectMapper =
                new ObjectMapper()
                        .registerModule(
                                new JavaTimeModule()
                        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void assessAwardIntegrityReturnsValidatedResponse()
            throws Exception {
        registerJsonResponse(
                200,
                successfulResponseJson()
        );

        MlInferenceClient client =
                createClient(
                        baseUrl,
                        5_000
                );

        AwardIntegrityAssessmentResponse response =
                client.assessAwardIntegrity(
                        tenderElevenRequest()
                );

        assertEquals(
                11L,
                response.tenderId()
        );

        assertEquals(
                "DETERMINISTIC_AWARD_INTEGRITY",
                response.assessmentType()
        );

        assertEquals(
                0,
                new BigDecimal("0.32")
                        .compareTo(
                                response.riskScore()
                        )
        );

        assertEquals(
                "MEDIUM",
                response.riskLevel()
        );

        assertEquals(
                "REVIEW_COMPETITION",
                response.recommendation()
        );

        assertEquals(
                "award-integrity-rules-v1",
                response.ruleVersion()
        );

        assertFalse(
                response.modelUsed()
        );

        assertTrue(
                response
                        .awardConsistentWithRules()
        );

        assertEquals(
                7,
                response.signals().size()
        );

        assertNotNull(
                response.assessedAt()
        );

        assertTrue(
                response
                        .signals()
                        .stream()
                        .anyMatch(
                                signal ->
                                        "SINGLE_ELIGIBLE_BID"
                                                .equals(
                                                        signal.code()
                                                )
                                                && signal.riskPoints()
                                                == 25
                        )
        );

        assertTrue(
                response
                        .signals()
                        .stream()
                        .anyMatch(
                                signal ->
                                        "HIGH_BUDGET_UTILISATION"
                                                .equals(
                                                        signal.code()
                                                )
                                                && signal.riskPoints()
                                                == 7
                        )
        );
    }

    @Test
    void assessAwardIntegrityRejectsMismatchedTenderId() {
        registerJsonResponse(
                200,
                successfulResponseJson()
                        .replace(
                                "\"tender_id\": 11",
                                "\"tender_id\": 12"
                        )
        );

        MlInferenceClient client =
                createClient(
                        baseUrl,
                        5_000
                );

        MlInferenceException exception =
                assertThrows(
                        MlInferenceException.class,
                        () ->
                                client
                                        .assessAwardIntegrity(
                                                tenderElevenRequest()
                                        )
                );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "tender ID does not match"
                        )
        );
    }

    @Test
    void assessAwardIntegrityRejectsEmptySignals() {
        registerJsonResponse(
                200,
                successfulResponseJson()
                        .replace(
                                signalsJson(),
                                "[]"
                        )
        );

        MlInferenceClient client =
                createClient(
                        baseUrl,
                        5_000
                );

        MlInferenceException exception =
                assertThrows(
                        MlInferenceException.class,
                        () ->
                                client
                                        .assessAwardIntegrity(
                                                tenderElevenRequest()
                                        )
                );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "no assessment signals"
                        )
        );
    }

    @Test
    void assessAwardIntegrityRejectsRiskScoreAboveOne() {
        registerJsonResponse(
                200,
                successfulResponseJson()
                        .replace(
                                "\"risk_score\": 0.32",
                                "\"risk_score\": 1.25"
                        )
        );

        MlInferenceClient client =
                createClient(
                        baseUrl,
                        5_000
                );

        MlInferenceException exception =
                assertThrows(
                        MlInferenceException.class,
                        () ->
                                client
                                        .assessAwardIntegrity(
                                                tenderElevenRequest()
                                        )
                );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "between zero and one"
                        )
        );
    }

    @Test
    void assessAwardIntegrityMapsPythonValidationFailure() {
        registerJsonResponse(
                422,
                """
                {
                  "detail": [
                    {
                      "type": "greater_than",
                      "loc": [
                        "body",
                        "tender_id"
                      ],
                      "msg": "Input should be greater than 0",
                      "input": 0
                    }
                  ]
                }
                """
        );

        MlInferenceClient client =
                createClient(
                        baseUrl,
                        5_000
                );

        MlInferenceException exception =
                assertThrows(
                        MlInferenceException.class,
                        () ->
                                client
                                        .assessAwardIntegrity(
                                                tenderElevenRequest()
                                        )
                );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "rejected"
                        )
        );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "422"
                        )
        );
    }

    @Test
    void assessAwardIntegrityReportsUnavailableService() {
        String unavailableBaseUrl =
                "http://127.0.0.1:"
                        + findUnusedPort();

        MlInferenceClient client =
                createClient(
                        unavailableBaseUrl,
                        1_000
                );

        MlInferenceException exception =
                assertThrows(
                        MlInferenceException.class,
                        () ->
                                client
                                        .assessAwardIntegrity(
                                                tenderElevenRequest()
                                        )
                );

        assertTrue(
                exception
                        .getMessage()
                        .contains(
                                "service is unavailable"
                        )
        );
    }

    @Test
    void assessAwardIntegrityRejectsNullRequest() {
        MlInferenceClient client =
                createClient(
                        baseUrl,
                        5_000
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                client
                                        .assessAwardIntegrity(
                                                null
                                        )
                );

        assertEquals(
                "Award integrity assessment "
                        + "request is required.",
                exception.getMessage()
        );
    }

    private MlInferenceClient createClient(
            String configuredBaseUrl,
            long readTimeoutMilliseconds
    ) {
        HttpClient httpClient =
                HttpClient
                        .newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(1)
                        )
                        .build();

        return new MlInferenceClient(
                httpClient,
                objectMapper,
                configuredBaseUrl,
                readTimeoutMilliseconds
        );
    }

    private AwardIntegrityAssessmentRequest
    tenderElevenRequest() {
        return new AwardIntegrityAssessmentRequest(
                11L,
                new BigDecimal(
                        "1850000.00"
                ),
                new BigDecimal(
                        "1720000.00"
                ),
                2,
                1,
                1,
                1,
                1,
                1,
                new BigDecimal(
                        "86.00"
                ),
                new BigDecimal(
                        "20.00"
                ),
                new BigDecimal(
                        "88.80"
                ),
                new BigDecimal(
                        "70.00"
                ),
                1,
                5L,
                List.of(
                        6L
                ),
                7L,
                LocalDateTime.parse(
                        "2026-10-31T21:28:01.897001"
                ),
                LocalDateTime.parse(
                        "2026-11-04T23:26:23.250630"
                ),
                LocalDateTime.parse(
                        "2026-11-06T16:13:01.094631"
                )
        );
    }

    private void registerJsonResponse(
            int statusCode,
            String responseBody
    ) {
        server.createContext(
                "/assess-award-integrity",
                exchange -> {
                    assertRequest(
                            exchange
                    );

                    byte[] responseBytes =
                            responseBody
                                    .getBytes(
                                            StandardCharsets
                                                    .UTF_8
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
                            .write(
                                    responseBytes
                            );

                    exchange.close();
                }
        );
    }

    private void assertRequest(
            HttpExchange exchange
    ) throws IOException {
        assertEquals(
                "POST",
                exchange
                        .getRequestMethod()
        );

        assertEquals(
                "/assess-award-integrity",
                exchange
                        .getRequestURI()
                        .getPath()
        );

        assertTrue(
                exchange
                        .getRequestHeaders()
                        .getFirst(
                                "Content-Type"
                        )
                        .startsWith(
                                "application/json"
                        )
        );

        String requestBody =
                new String(
                        exchange
                                .getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );

        assertTrue(
                requestBody.contains(
                        "\"tender_id\":11"
                )
        );

        assertTrue(
                requestBody.contains(
                        "\"eligible_bid_count\":1"
                )
        );

        assertTrue(
                requestBody.contains(
                        "\"adjudicator_user_id\":7"
                )
        );
    }

    private int findUnusedPort() {
        try (
                java.net.ServerSocket socket =
                        new java.net.ServerSocket(
                                0
                        )
        ) {
            return socket
                    .getLocalPort();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not allocate test port.",
                    exception
            );
        }
    }

    private String successfulResponseJson() {
        return (
                """
                {
                  "tender_id": 11,
                  "assessment_type": "DETERMINISTIC_AWARD_INTEGRITY",
                  "risk_score": 0.32,
                  "risk_level": "MEDIUM",
                  "recommendation": "REVIEW_COMPETITION",
                  "rule_version": "award-integrity-rules-v1",
                  "model_used": false,
                  "award_consistent_with_rules": true,
                  "signals":
                """
                        + signalsJson()
                        + """
                  ,
                  "assessed_at": "2026-11-07T07:50:01.924412Z"
                }
                """
        );
    }

    private String signalsJson() {
        return """
                [
                  {
                    "code": "SINGLE_ELIGIBLE_BID",
                    "severity": "MEDIUM",
                    "message": "Only one eligible bid remained for evaluation.",
                    "observed_value": 1,
                    "threshold": 2,
                    "risk_points": 25
                  },
                  {
                    "code": "HIGH_BUDGET_UTILISATION",
                    "severity": "LOW",
                    "message": "The awarded amount uses at least 90 percent of the published budget.",
                    "observed_value": 92.97,
                    "threshold": 90,
                    "risk_points": 7
                  },
                  {
                    "code": "EVALUATION_MATRIX_COMPLETE",
                    "severity": "INFO",
                    "message": "All expected evaluator scores were submitted.",
                    "observed_value": 100.0,
                    "threshold": 100,
                    "risk_points": 0
                  },
                  {
                    "code": "TECHNICAL_THRESHOLD_PASSED",
                    "severity": "INFO",
                    "message": "The awarded bid passed the published technical threshold.",
                    "observed_value": 86.0,
                    "threshold": 70.0,
                    "risk_points": 0
                  },
                  {
                    "code": "TOP_RANKED_BID_AWARDED",
                    "severity": "INFO",
                    "message": "The highest-ranked bid was selected.",
                    "observed_value": 1,
                    "threshold": 1,
                    "risk_points": 0
                  },
                  {
                    "code": "SEGREGATION_OF_DUTIES_PASSED",
                    "severity": "INFO",
                    "message": "Specification, evaluation and adjudication used different users.",
                    "observed_value": null,
                    "threshold": null,
                    "risk_points": 0
                  },
                  {
                    "code": "WORKFLOW_TIMELINE_VALID",
                    "severity": "INFO",
                    "message": "Publication, closure and award timestamps follow the expected order.",
                    "observed_value": null,
                    "threshold": null,
                    "risk_points": 0
                  }
                ]
                """;
    }
}