package com.kasibridge.fraud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.fraud.dto.MlInferenceRequest;
import com.kasibridge.fraud.dto.MlInferenceResponse;
import com.kasibridge.fraud.exception.MlInferenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class MlInferenceClient {

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final String baseUrl;

    private final long readTimeoutMilliseconds;

    public MlInferenceClient(
            HttpClient mlInferenceHttpClient,
            ObjectMapper objectMapper,
            @Value(
                    "${kasibridge.fraud.ml.base-url:"
                            + "http://127.0.0.1:8090}"
            )
            String baseUrl,
            @Value(
                    "${kasibridge.fraud.ml.read-timeout-ms:5000}"
            )
            long readTimeoutMilliseconds
    ) {
        this.httpClient =
                mlInferenceHttpClient;

        this.objectMapper =
                objectMapper;

        this.baseUrl =
                removeTrailingSlash(
                        baseUrl
                );

        if (readTimeoutMilliseconds <= 0) {
            throw new IllegalArgumentException(
                    "ML inference read timeout "
                            + "must be positive."
            );
        }

        this.readTimeoutMilliseconds =
                readTimeoutMilliseconds;
    }

    public MlInferenceResponse predict(
            MlInferenceRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "ML inference request is required."
            );
        }

        String jsonBody =
                serializeRequest(
                        request
                );

        if (jsonBody.isBlank()) {
            throw new MlInferenceException(
                    "ML inference request serialized "
                            + "to an empty JSON body."
            );
        }

        String endpoint =
                baseUrl + "/predict";

        HttpRequest httpRequest =
                HttpRequest
                        .newBuilder()
                        .uri(
                                URI.create(
                                        endpoint
                                )
                        )
                        .timeout(
                                Duration.ofMillis(
                                        readTimeoutMilliseconds
                                )
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .POST(
                                HttpRequest
                                        .BodyPublishers
                                        .ofString(
                                                jsonBody
                                        )
                        )
                        .build();

        log.debug(
                "Calling ML inference service: "
                        + "endpoint={} bodyLength={} "
                        + "contentType=application/json",
                endpoint,
                jsonBody.getBytes(
                        java.nio.charset
                                .StandardCharsets
                                .UTF_8
                ).length
        );

        try {
            HttpResponse<String> httpResponse =
                    httpClient.send(
                            httpRequest,
                            HttpResponse
                                    .BodyHandlers
                                    .ofString()
                    );

            int statusCode =
                    httpResponse.statusCode();

            String responseBody =
                    httpResponse.body();

            if (
                    statusCode < 200
                            || statusCode >= 300
            ) {
                log.warn(
                        "ML inference request rejected: "
                                + "status={} response={}",
                        statusCode,
                        sanitizeResponseBody(
                                responseBody
                        )
                );

                throw new MlInferenceException(
                        buildProviderFailure(
                                statusCode,
                                responseBody
                        )
                );
            }

            MlInferenceResponse response =
                    deserializeResponse(
                            responseBody
                    );

            validateResponse(
                    response
            );

            return response;

        } catch (
                InterruptedException exception
        ) {
            Thread.currentThread()
                    .interrupt();

            throw new MlInferenceException(
                    "Procurement fraud model "
                            + "request was interrupted.",
                    exception
            );

        } catch (
                IOException exception
        ) {
            log.warn(
                    "ML inference service could "
                            + "not be reached: {}",
                    exception.getMessage()
            );

            throw new MlInferenceException(
                    "Procurement fraud model "
                            + "is unavailable.",
                    exception
            );

        } catch (
                MlInferenceException exception
        ) {
            throw exception;

        } catch (Exception exception) {
            log.error(
                    "Unexpected ML inference "
                            + "client failure.",
                    exception
            );

            throw new MlInferenceException(
                    "Unexpected procurement fraud "
                            + "model error.",
                    exception
            );
        }
    }

    private String serializeRequest(
            MlInferenceRequest request
    ) {
        try {
            return objectMapper
                    .writeValueAsString(
                            request
                    );

        } catch (
                JsonProcessingException exception
        ) {
            throw new MlInferenceException(
                    "Could not serialize the ML "
                            + "inference request.",
                    exception
            );
        }
    }

    private MlInferenceResponse
    deserializeResponse(
            String responseBody
    ) {
        if (
                responseBody == null
                        || responseBody.isBlank()
        ) {
            throw new MlInferenceException(
                    "ML inference service returned "
                            + "an empty response."
            );
        }

        try {
            return objectMapper.readValue(
                    responseBody,
                    MlInferenceResponse.class
            );

        } catch (
                JsonProcessingException exception
        ) {
            throw new MlInferenceException(
                    "Could not parse the ML "
                            + "inference response.",
                    exception
            );
        }
    }

    private void validateResponse(
            MlInferenceResponse response
    ) {
        if (response == null) {
            throw new MlInferenceException(
                    "ML inference service returned "
                            + "an empty response."
            );
        }

        if (
                response.classification() == null
                        || response
                        .classification()
                        .isBlank()
        ) {
            throw new MlInferenceException(
                    "ML inference response has no "
                            + "classification."
            );
        }

        if (
                response.riskLevel() == null
                        || response
                        .riskLevel()
                        .isBlank()
        ) {
            throw new MlInferenceException(
                    "ML inference response has no "
                            + "risk level."
            );
        }

        if (
                response.riskProbability()
                        == null
        ) {
            throw new MlInferenceException(
                    "ML inference response has no "
                            + "risk probability."
            );
        }

        if (
                response.riskProbability()
                        .compareTo(
                                java.math.BigDecimal.ZERO
                        ) < 0
                        || response
                        .riskProbability()
                        .compareTo(
                                java.math.BigDecimal.ONE
                        ) > 0
        ) {
            throw new MlInferenceException(
                    "ML inference response contains "
                            + "an invalid probability."
            );
        }

        if (
                response.modelName() == null
                        || response
                        .modelName()
                        .isBlank()
        ) {
            throw new MlInferenceException(
                    "ML inference response has no "
                            + "model name."
            );
        }

        if (
                response.modelVersion() == null
                        || response
                        .modelVersion()
                        .isBlank()
        ) {
            throw new MlInferenceException(
                    "ML inference response has no "
                            + "model version."
            );
        }

        if (
                response.modelSha256() == null
                        || response
                        .modelSha256()
                        .isBlank()
        ) {
            throw new MlInferenceException(
                    "ML inference response has no "
                            + "model checksum."
            );
        }
    }

    private String buildProviderFailure(
            int statusCode,
            String responseBody
    ) {
        if (
                responseBody == null
                        || responseBody.isBlank()
        ) {
            return "ML inference service "
                    + "rejected the request with HTTP "
                    + statusCode;
        }

        return "ML inference service rejected "
                + "the request with HTTP "
                + statusCode
                + ": "
                + sanitizeResponseBody(
                responseBody
        );
    }

    private String sanitizeResponseBody(
            String responseBody
    ) {
        if (
                responseBody == null
                        || responseBody.isBlank()
        ) {
            return "<empty>";
        }

        String sanitized =
                responseBody
                        .replaceAll(
                                "[\\r\\n\\t]+",
                                " "
                        )
                        .trim();

        return sanitized.length() > 800
                ? sanitized.substring(
                0,
                800
        )
                : sanitized;
    }

    private String removeTrailingSlash(
            String value
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "ML inference base URL "
                            + "is required."
            );
        }

        String normalized =
                value.trim();

        while (
                normalized.endsWith("/")
        ) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        return normalized;
    }
}