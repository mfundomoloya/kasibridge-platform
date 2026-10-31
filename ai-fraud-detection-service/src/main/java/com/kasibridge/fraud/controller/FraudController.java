package com.kasibridge.fraud.controller;

import com.kasibridge.fraud.dto.*;
import com.kasibridge.fraud.entity.FraudAlert;
import com.kasibridge.fraud.service.FraudAssessmentService;
import com.kasibridge.fraud.service.FraudDetectionService;
import com.kasibridge.fraud.service.InvoiceFraudPredictionService;
import com.kasibridge.security.KasiBridgeUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Tag(
        name = "Fraud Management",
        description = "Fraud detection, alert management, and fraud operations")
@RestController
@RequestMapping("/api/v1/fraud")
@Slf4j
public class FraudController {

    private final FraudDetectionService service;
    private final InvoiceFraudPredictionService invoiceFraudPredictionService;
    private final FraudAssessmentService fraudAssessmentService;

    public FraudController(FraudDetectionService service,
                           InvoiceFraudPredictionService invoiceFraudPredictionService,
                           FraudAssessmentService fraudAssessmentService) {
        this.service = service;
        this.invoiceFraudPredictionService = invoiceFraudPredictionService;
        this.fraudAssessmentService = fraudAssessmentService;
    }

    @Operation(
            summary = "Analyze transaction for fraud",
            description = "Runs fraud detection rules and creates alerts if suspicious activity is detected")
    @Tag(name = "Fraud Analysis")
    @PostMapping("/analyze")
    public ResponseEntity<List<FraudAlertResponse>> analyzeTransaction(@Valid @RequestBody AnalyzeTransactionRequest request) {
        log.info("POST /api/v1/fraud/analyze - transaction ID: {}", request.getTransactionId());

        List<FraudAlert> alerts = service.detect(request.toTransactionEvent());

        List<FraudAlertResponse> response = alerts.stream()
                .map(FraudAlertMapper::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary =
                    "Predict procurement invoice fraud risk",
            description =
                    "Calls the validated supervised-learning "
                            + "model and returns a procurement "
                            + "invoice risk classification."
    )
    @Tag(name = "Fraud Analysis")
    @PostMapping("/invoices/predict")
    public ResponseEntity<InvoiceFraudPredictionResponse> predictInvoiceFraud(@Valid @RequestBody InvoiceFraudPredictionRequest request
    ) {
        log.info(
                "POST /api/v1/fraud/invoices/predict"
        );

        return ResponseEntity.ok(
                invoiceFraudPredictionService
                        .predict(request)
        );
    }


    @Operation(summary = "Assess and persist procurement invoice risk",
            description =
                    "Runs the supervised procurement fraud "
                            + "model and persists immutable "
                            + "assessment evidence. Repeated "
                            + "requests with the same invoice "
                            + "reference, model version, and "
                            + "feature snapshot return the "
                            + "existing assessment."
    )
    @Tag(name = "Fraud Analysis")
    @PostMapping("/invoices/assess")
    public ResponseEntity<FraudAssessmentResponse> assessInvoiceFraud(@Valid @RequestBody InvoiceFraudPredictionRequest request) {

        log.info(
                "POST /api/v1/fraud/invoices/assess "
                        + "- invoiceReference={}",
                request.getInvoiceReference()
        );

        FraudAssessmentResult result =
                fraudAssessmentService.assess(
                        request
                );

        if (result.created()) {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(
                            result.assessment()
                    );
        }

        return ResponseEntity.ok(
                result.assessment()
        );
    }

    @Operation(summary = "Retrieve fraud assessment dashboard metrics",
            description =
                    "Returns aggregate procurement fraud "
                            + "assessment metrics for the analyst "
                            + "dashboard, including review-status "
                            + "counts, risk counts, open high-risk "
                            + "work, and pending-assessment age."
    )
    @Tag(name = "Fraud Assessment Review")
    @GetMapping("/assessments/dashboard")
    public ResponseEntity<FraudAssessmentDashboardResponse> getFraudAssessmentDashboard() {
        log.debug(
                "GET /api/v1/fraud/assessments/dashboard"
        );

        return ResponseEntity.ok(
                fraudAssessmentService
                        .getAssessmentDashboard()
        );
    }

    @Operation(summary = "Search procurement fraud assessments",
            description =
                    "Returns a paginated analyst queue with "
                            + "optional filters for review status, "
                            + "risk level, classification, supplier, "
                            + "department, model version, and "
                            + "assessment time. When no explicit "
                            + "sort is supplied, the queue prioritises "
                            + "pending and escalated assessments, "
                            + "then higher risk levels and older "
                            + "assessments."
    )
    @Tag(name = "Fraud Assessment Review")
    @GetMapping("/assessments")
    public ResponseEntity<Page<FraudAssessmentResponse>> searchFraudAssessments(@ParameterObject
                                                                                    FraudAssessmentSearchCriteria criteria,

            @ParameterObject
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        log.debug(
                "GET /api/v1/fraud/assessments "
                        + "- reviewStatus={} "
                        + "riskLevel={} "
                        + "classification={} "
                        + "supplierId={} "
                        + "departmentId={} "
                        + "modelVersion={} "
                        + "assessedFrom={} "
                        + "assessedTo={} "
                        + "page={} "
                        + "size={} "
                        + "sort={}",
                criteria != null
                        ? criteria.reviewStatus()
                        : null,
                criteria != null
                        ? criteria.riskLevel()
                        : null,
                criteria != null
                        ? criteria.classification()
                        : null,
                criteria != null
                        ? criteria.supplierId()
                        : null,
                criteria != null
                        ? criteria.departmentId()
                        : null,
                criteria != null
                        ? criteria.modelVersion()
                        : null,
                criteria != null
                        ? criteria.assessedFrom()
                        : null,
                criteria != null
                        ? criteria.assessedTo()
                        : null,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );

        return ResponseEntity.ok(
                fraudAssessmentService
                        .searchAssessments(
                                criteria,
                                pageable
                        )
        );
    }

    @Operation(summary = "Retrieve a procurement fraud assessment",
            description =
                    "Returns a persisted procurement invoice "
                            + "fraud assessment by its public "
                            + "assessment reference. The full "
                            + "feature snapshot is excluded "
                            + "from the standard response."
    )
    @Tag(name = "Fraud Assessment Review")
    @GetMapping("/assessments/{assessmentReference}")
    public ResponseEntity<FraudAssessmentResponse> getFraudAssessment(@PathVariable String assessmentReference) {
        log.info(
                "GET /api/v1/fraud/assessments/{}",
                assessmentReference
        );

        return ResponseEntity.ok(
                fraudAssessmentService
                        .getByAssessmentReference(
                                assessmentReference
                        )
        );
    }

    @Operation(summary = "Confirm a high-risk assessment as fraud",
            description =
                    "Records an authorised analyst decision "
                            + "that a pending or escalated "
                            + "assessment was confirmed as "
                            + "fraud. The reviewer identity is "
                            + "derived from the authenticated "
                            + "JWT principal."
    )
    @Tag(name = "Fraud Assessment Review")
    @PostMapping("/assessments/{assessmentReference}/confirm")
    public ResponseEntity<FraudAssessmentResponse> confirmAssessmentFraud(@PathVariable String assessmentReference,
                                                                          @Valid
                                                                          @RequestBody
                                                                          FraudAssessmentReviewRequest request,
                                                                          Authentication authentication
    ) {
        String reviewer =
                requireAuthenticatedReviewer(
                        authentication
                );

        log.info(
                "POST /api/v1/fraud/assessments/"
                        + "{}/confirm - reviewer={}",
                assessmentReference,
                reviewer
        );

        return ResponseEntity.ok(
                fraudAssessmentService
                        .confirmFraud(
                                assessmentReference,
                                request,
                                reviewer
                        )
        );
    }

    @Operation(summary = "Mark a fraud assessment as false positive",
            description =
                    "Records an authorised analyst decision "
                            + "that a pending or escalated "
                            + "assessment is a false positive. "
                            + "The reviewer identity is derived "
                            + "from the authenticated JWT "
                            + "principal."
    )
    @Tag(name = "Fraud Assessment Review")
    @PostMapping("/assessments/{assessmentReference}/false-positive")
    public ResponseEntity<FraudAssessmentResponse> markAssessmentFalsePositive(@PathVariable String assessmentReference,
                                                                               @Valid
                                                                               @RequestBody
                                                                               FraudAssessmentReviewRequest request,
                                                                               Authentication authentication
    ) {
        String reviewer =
                requireAuthenticatedReviewer(
                        authentication
                );

        log.info(
                "POST /api/v1/fraud/assessments/"
                        + "{}/false-positive - reviewer={}",
                assessmentReference,
                reviewer
        );

        return ResponseEntity.ok(
                fraudAssessmentService
                        .markFalsePositive(
                                assessmentReference,
                                request,
                                reviewer
                        )
        );
    }

    @Operation(summary = "Escalate a fraud assessment",
            description =
                    "Escalates a pending or automatically "
                            + "cleared assessment for senior "
                            + "investigation. The reviewer "
                            + "identity is derived from the "
                            + "authenticated JWT principal."
    )
    @Tag(name = "Fraud Assessment Review")
    @PostMapping("/assessments/{assessmentReference}/escalate")
    public ResponseEntity<FraudAssessmentResponse> escalateAssessment(@PathVariable String assessmentReference,
                                                                      @Valid
                                                                      @RequestBody
                                                                      FraudAssessmentReviewRequest request, Authentication authentication
    ) {
        String reviewer =
                requireAuthenticatedReviewer(
                        authentication
                );

        log.info(
                "POST /api/v1/fraud/assessments/"
                        + "{}/escalate - reviewer={}",
                assessmentReference,
                reviewer
        );

        return ResponseEntity.ok(
                fraudAssessmentService
                        .escalate(
                                assessmentReference,
                                request,
                                reviewer
                        )
        );
    }

    @Operation(summary = "Get all fraud alerts (paginated)")
    @Tag(name = "Alert Queries")
    @GetMapping("/alerts")
    public ResponseEntity<Page<FraudAlertResponse>> getAlerts(
            @Parameter(
                    description = "Pagination and sorting. Format: page=0&size=10&sort=detectedAt,desc"
            )
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            @ParameterObject Pageable pageable) {
        log.info("GET /api/v1/fraud/alerts");
        return ResponseEntity.ok(service.getAlerts(pageable).map(FraudAlertMapper::toResponse));
    }


    @Operation(summary = "Get alert by ID")
    @Tag(name = "Alert Queries")
    @GetMapping("/alerts/{id}")
    public ResponseEntity<FraudAlertResponse> getAlertById(@PathVariable("id") Long id) {
        log.info("GET /api/v1/fraud/alerts/{}", id);
        return ResponseEntity.ok(FraudAlertMapper.toResponse(service.getAlertById(id)));
    }


    @Operation(summary = "Get alert by reference")
    @Tag(name = "Alert Queries")
    @GetMapping("/alerts/reference/{reference}")
    public ResponseEntity<FraudAlertResponse> getAlertByReference(@PathVariable("reference") String reference) {
        log.info("GET /api/v1/fraud/alerts/reference/{}", reference);
        return ResponseEntity.ok(FraudAlertMapper.toResponse(service.getAlertByReference(reference)));
    }


    @Operation(summary = "Get alert by trader ID")
    @Tag(name = "Alert Queries")

    @GetMapping("/alerts/trader/{traderId}")
    public ResponseEntity<Page<FraudAlertResponse>> getAlertsByTrader(
            @PathVariable("traderId") Long traderId,
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/fraud/alerts/trader/{}", traderId);
        return ResponseEntity.ok(
                service.getAlertsByTrader(traderId, pageable).map(FraudAlertMapper::toResponse)
        );
    }


    @Operation(summary = "Get alert by status")
    @Tag(name = "Alert Queries")
    @GetMapping("/alerts/status/{status}")
    public ResponseEntity<Page<FraudAlertResponse>> getAlertsByStatus(
            @PathVariable("status") FraudAlert.AlertStatus status,
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/fraud/alerts/status/{}", status);
        return ResponseEntity.ok(service.getAlertsByStatus(status, pageable).map(FraudAlertMapper::toResponse));
    }


    @Operation(summary = "Get alert by pattern type")
    @Tag(name = "Alert Queries")

    @GetMapping("/alerts/pattern/{patternType}")
    public ResponseEntity<Page<FraudAlertResponse>> getAlertsByPatternType(
            @PathVariable("patternType") FraudAlert.FraudPatternType patternType,
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/fraud/alerts/pattern/{}", patternType);
        return ResponseEntity.ok(
                service.getAlertsByPatternType(patternType, pageable).map(FraudAlertMapper::toResponse)
        );
    }


    @Operation(summary = "Get alert by Trader and Status")
    @Tag(name = "Alert Queries")
    @GetMapping("/alerts/trader/{traderId}/status/{status}")
    public ResponseEntity<Page<FraudAlertResponse>> getAlertsByTraderAndStatus(
            @PathVariable("traderId") Long traderId,
            @PathVariable("status") FraudAlert.AlertStatus status,
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/fraud/alerts/trader/{}/status/{}", traderId, status);
        return ResponseEntity.ok(
                service.getAlertsByTraderAndStatus(traderId, status, pageable).map(FraudAlertMapper::toResponse)
        );
    }


    @Operation(summary = "Review a fraud alert")
    @Tag(name = "Alert Actions")
    @PatchMapping("/alerts/{id}/review")
    public ResponseEntity<FraudAlertResponse> reviewAlert(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReviewAlertRequest request) {
        log.info("PATCH /api/v1/fraud/alerts/{}/review", id);
        FraudAlert reviewed = service.reviewAlert(id, request.getReviewedBy(), request.getReviewNotes());
        return ResponseEntity.ok(FraudAlertMapper.toResponse(reviewed));
    }


    @Operation(summary = "Dismiss a fraud alert")
    @Tag(name = "Alert Actions")
    @PatchMapping("/alerts/{id}/dismiss")
    public ResponseEntity<FraudAlertResponse> dismissAlert(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReviewAlertRequest request) {
        log.info("PATCH /api/v1/fraud/alerts/{}/dismiss", id);
        FraudAlert dismissed = service.dismissAlert(id, request.getReviewedBy(), request.getReviewNotes());
        return ResponseEntity.ok(FraudAlertMapper.toResponse(dismissed));
    }


    @Operation(summary = "Escalate a fraud alert")
    @Tag(name = "Alert Actions")
    @PatchMapping("/alerts/{id}/escalate")
    public ResponseEntity<FraudAlertResponse> escalateAlert(@PathVariable("id") Long id) {
        log.info("PATCH /api/v1/fraud/alerts/{}/escalate", id);
        return ResponseEntity.ok(FraudAlertMapper.toResponse(service.escalateAlert(id)));
    }


    @Operation(summary = "Get fraud dashboard summary")
    @Tag(name = "Dashboard")
    @GetMapping("/dashboard/summary")
    public ResponseEntity<FraudDashboardSummary> getDashboardSummary() {
        log.info("GET /api/v1/fraud/dashboard/summary");
        return ResponseEntity.ok(service.getDashboardSummary());
    }


    @Operation(
            summary = "Search fraud alerts",
            description = "Filter alerts by trader, status, severity, date range, and amount")
    @Tag(name = "Alert Queries")
    @GetMapping("/alerts/search")
    public ResponseEntity<Page<FraudAlertResponse>> searchAlerts(
            @RequestParam(required = false) Long traderId,
            @RequestParam(required = false) FraudAlert.AlertStatus status,
            @RequestParam(required = false) FraudAlert.FraudPatternType patternType,
            @RequestParam(required = false) FraudAlert.Severity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) String alertReference,
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        FraudAlertSearchCriteria criteria = new FraudAlertSearchCriteria();
        criteria.setTraderId(traderId);
        criteria.setStatus(status);
        criteria.setPatternType(patternType);
        criteria.setSeverity(severity);
        criteria.setFromDate(fromDate);
        criteria.setToDate(toDate);
        criteria.setAmountMin(amountMin);
        criteria.setAmountMax(amountMax);
        criteria.setAlertReference(alertReference);

        log.info("GET /api/v1/fraud/alerts/search - criteria: {}", criteria);

        return ResponseEntity.ok(service.searchAlerts(criteria, pageable).map(FraudAlertMapper::toResponse));
    }

    private String requireAuthenticatedReviewer(
            Authentication authentication
    ) {
        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {
            throw new IllegalArgumentException(
                    "An authenticated reviewer "
                            + "is required."
            );
        }

        Object principal = authentication.getPrincipal();

        if (
                principal
                        instanceof KasiBridgeUserPrincipal
                        kasiBridgePrincipal
        ) {
            String username =
                    kasiBridgePrincipal.username();

            if (
                    username == null
                            || username.isBlank()
            ) {
                throw new IllegalArgumentException(
                        "The authenticated reviewer "
                                + "has no username."
                );
            }

            return username.trim();
        }

        String reviewer =
                authentication.getName();

        if (
                reviewer == null
                        || reviewer.isBlank()
                        || "anonymousUser"
                        .equalsIgnoreCase(reviewer)
        ) {
            throw new IllegalArgumentException(
                    "An authenticated reviewer "
                            + "is required."
            );
        }

        return reviewer.trim();
    }
}
