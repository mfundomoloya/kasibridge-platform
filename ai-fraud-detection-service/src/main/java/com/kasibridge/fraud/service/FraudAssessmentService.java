package com.kasibridge.fraud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.fraud.dto.FraudAssessmentResponse;
import com.kasibridge.fraud.dto.FraudAssessmentResult;
import com.kasibridge.fraud.dto.InvoiceFraudPredictionRequest;
import com.kasibridge.fraud.dto.InvoiceFraudPredictionResponse;
import com.kasibridge.fraud.entity.FraudAssessment;
import com.kasibridge.fraud.exception.FraudAssessmentConflictException;
import com.kasibridge.fraud.exception.FraudAssessmentNotFoundException;
import com.kasibridge.fraud.repository.FraudAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAssessmentService {

    private final InvoiceFraudPredictionService predictionService;

    private final FraudAssessmentRepository assessmentRepository;

    private final ObjectMapper objectMapper;

    @Transactional
    public FraudAssessmentResult assess(InvoiceFraudPredictionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Invoice fraud prediction "
                            + "request is required."
            );
        }

        /*
         * Resolve the business reference before
         * inference and persistence. The generated
         * value is used only when the caller did not
         * provide a source invoice reference.
         */
        String invoiceReference =
                resolveInvoiceReference(
                        request.getInvoiceReference()
                );

        /*
         * Snapshot the exact incoming camelCase
         * request before changing any request fields.
         */
        String featureSnapshotJson =
                serializeFeatureSnapshot(
                        request
                );

        String featureSnapshotSha256 =
                calculateSha256(
                        featureSnapshotJson
                );

        InvoiceFraudPredictionResponse prediction =
                predictionService.predict(
                        request
                );

        return assessmentRepository
                .findByInvoiceReferenceAndModelVersion(
                        invoiceReference,
                        prediction.modelVersion()
                )
                .map(
                        existing ->
                                mapExistingAssessment(
                                        existing,
                                        featureSnapshotSha256
                                )
                )
                .orElseGet(
                        () -> createAssessment(
                                request,
                                prediction,
                                invoiceReference,
                                featureSnapshotJson,
                                featureSnapshotSha256
                        )
                );
    }

    @Transactional(readOnly = true)
    public FraudAssessmentResponse getByAssessmentReference(String assessmentReference) {
        String normalizedReference =
                requireText(
                        assessmentReference,
                        "Assessment reference"
                );

        FraudAssessment assessment =
                assessmentRepository
                        .findByAssessmentReference(
                                normalizedReference
                        )
                        .orElseThrow(
                                () ->
                                        new FraudAssessmentNotFoundException(
                                                "Fraud assessment "
                                                        + "not found: "
                                                        + normalizedReference
                                        )
                        );

        return FraudAssessmentResponse.from(
                assessment
        );
    }

    private FraudAssessmentResult mapExistingAssessment(FraudAssessment existing, String submittedSnapshotSha256) {
        /*
         * Idempotency is valid only when the same
         * invoice reference and model version were
         * assessed using the same feature payload.
         *
         * Reusing an invoice reference with changed
         * evidence must not silently return the old
         * prediction.
         */
        if (
                !existing
                        .getFeatureSnapshotSha256()
                        .equalsIgnoreCase(
                                submittedSnapshotSha256
                        )
        ) {
            throw new FraudAssessmentConflictException(
                    "Invoice reference "
                            + existing
                            .getInvoiceReference()
                            + " was already assessed by "
                            + "model version "
                            + existing
                            .getModelVersion()
                            + " using a different "
                            + "feature snapshot."
            );
        }

        log.info(
                "Returning existing fraud assessment: "
                        + "assessmentReference={} "
                        + "invoiceReference={} "
                        + "modelVersion={}",
                existing.getAssessmentReference(),
                existing.getInvoiceReference(),
                existing.getModelVersion()
        );

        return FraudAssessmentResult.existing(
                FraudAssessmentResponse.from(
                        existing
                )
        );
    }

    private FraudAssessmentResult createAssessment(
            InvoiceFraudPredictionRequest request,
            InvoiceFraudPredictionResponse prediction,
            String invoiceReference,
            String featureSnapshotJson,
            String featureSnapshotSha256
    ) {
        FraudAssessment assessment =
                FraudAssessment.create(
                        invoiceReference,
                        normalizeOptionalText(request.getSupplierId()),
                        normalizeOptionalText(request.getDepartmentId()
                        ),
                        request.getInvoiceAmount(),
                        normalizeCountry(request.getSupplierCountry()),
                        normalizeOptionalText(request.getRegion()),
                        FraudAssessment
                                .Classification
                                .valueOf(prediction
                                                .classification()
                                                .name()
                                ),
                        FraudAssessment
                                .RiskLevel
                                .valueOf(
                                        prediction
                                                .riskLevel()
                                                .name()
                                ),
                        requireProbability(
                                prediction
                                        .riskProbability(),
                                "Risk probability"
                        ),
                        requireProbability(
                                prediction.threshold(),
                                "Decision threshold"
                        ),
                        FraudAssessment
                                .RecommendedAction
                                .valueOf(
                                        prediction
                                                .recommendedAction()
                                                .name()
                                ),
                        prediction.modelName(),
                        prediction.modelVersion(),
                        prediction.experiment(),
                        FraudAssessment
                                .DetectionSource
                                .valueOf(
                                        prediction
                                                .detectionSource()
                                                .name()
                                ),
                        prediction.modelSha256(),
                        featureSnapshotJson,
                        featureSnapshotSha256,
                        resolveAssessedAt(
                                prediction.assessedAt()
                        )
                );

        FraudAssessment savedAssessment =
                assessmentRepository.save(
                        assessment
                );

        log.info(
                "Fraud assessment persisted: "
                        + "assessmentReference={} "
                        + "invoiceReference={} "
                        + "classification={} "
                        + "riskLevel={} "
                        + "modelVersion={}",
                savedAssessment
                        .getAssessmentReference(),
                savedAssessment
                        .getInvoiceReference(),
                savedAssessment
                        .getClassification(),
                savedAssessment
                        .getRiskLevel(),
                savedAssessment
                        .getModelVersion()
        );

        return FraudAssessmentResult.created(
                FraudAssessmentResponse.from(
                        savedAssessment
                )
        );
    }

    private String serializeFeatureSnapshot(InvoiceFraudPredictionRequest request) {
        try {
            return objectMapper
                    .writeValueAsString(
                            request
                    );

        } catch (
                JsonProcessingException exception
        ) {
            throw new IllegalStateException(
                    "Could not serialize the "
                            + "fraud assessment "
                            + "feature snapshot.",
                    exception
            );
        }
    }

    private String calculateSha256(String value) {
        if (
                value == null
                        || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Feature snapshot JSON "
                            + "is required."
            );
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (
                NoSuchAlgorithmException exception
        ) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception
            );
        }
    }

    private String resolveInvoiceReference(String invoiceReference) {
        String normalized =
                normalizeOptionalText(
                        invoiceReference
                );

        if (normalized != null) {
            return normalized;
        }

        return "KB-INV-AUTO-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private LocalDateTime resolveAssessedAt(
            LocalDateTime assessedAt
    ) {
        return assessedAt != null
                ? assessedAt
                : LocalDateTime.now();
    }

    private BigDecimal requireProbability(
            BigDecimal probability,
            String fieldName
    ) {
        if (probability == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        if (
                probability.compareTo(
                        BigDecimal.ZERO
                ) < 0
                        || probability.compareTo(
                        BigDecimal.ONE
                ) > 0
        ) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be between "
                            + "0 and 1."
            );
        }

        return probability;
    }

    private String normalizeCountry(String country) {
        String normalized =
                requireText(
                        country,
                        "Supplier country"
                );

        normalized =
                normalized.toUpperCase(
                        Locale.ROOT
                );

        if (
                normalized.length() < 2
                        || normalized.length() > 3
        ) {
            throw new IllegalArgumentException(
                    "Supplier country must contain "
                            + "2 or 3 characters."
            );
        }

        return normalized;
    }

    private String requireText(String value, String fieldName) {

        String normalized = normalizeOptionalText(value);

        if (normalized == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}