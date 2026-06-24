package com.kasibridge.fraud.controller;

import com.kasibridge.fraud.dto.*;
import com.kasibridge.fraud.entity.FraudAlert;
import com.kasibridge.fraud.service.FraudDetectionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud")
@Slf4j
public class FraudController {

    private final FraudDetectionService service;

    public FraudController(FraudDetectionService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    public ResponseEntity<List<FraudAlertResponse>> analyzeTransaction(
            @Valid @RequestBody AnalyzeTransactionRequest request) {
        log.info("POST /api/v1/fraud/analyze - transaction ID: {}", request.getTransactionId());

        List<FraudAlert> alerts = service.detect(request.toTransactionEvent());

        List<FraudAlertResponse> response = alerts.stream()
                .map(FraudAlertMapper::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/alerts")
    public ResponseEntity<Page<FraudAlertResponse>> getAlerts(
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/fraud/alerts");
        return ResponseEntity.ok(service.getAlerts(pageable).map(FraudAlertMapper::toResponse));
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<FraudAlertResponse> getAlertById(@PathVariable Long id) {
        log.info("GET /api/v1/fraud/alerts/{}", id);
        return ResponseEntity.ok(FraudAlertMapper.toResponse(service.getAlertById(id)));
    }

    @GetMapping("/alerts/reference/{reference}")
    public ResponseEntity<FraudAlertResponse> getAlertByReference(@PathVariable String reference) {
        log.info("GET /api/v1/fraud/alerts/reference/{}", reference);
        return ResponseEntity.ok(FraudAlertMapper.toResponse(service.getAlertByReference(reference)));
    }

    @GetMapping("/alerts/trader/{traderId}")
    public ResponseEntity<Page<FraudAlertResponse>> getAlertsByTrader(
            @PathVariable Long traderId,
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/fraud/alerts/trader/{}", traderId);
        return ResponseEntity.ok(
                service.getAlertsByTrader(traderId, pageable).map(FraudAlertMapper::toResponse)
        );
    }

    @GetMapping("/alerts/status/{status}")
    public ResponseEntity<Page<FraudAlertResponse>> getAlertsByStatus(
            @PathVariable FraudAlert.AlertStatus status,
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/fraud/alerts/status/{}", status);
        return ResponseEntity.ok(service.getAlertsByStatus(status, pageable).map(FraudAlertMapper::toResponse));
    }

    @GetMapping("/alerts/pattern/{patternType}")
    public ResponseEntity<Page<FraudAlertResponse>> getAlertsByPatternType(
            @PathVariable FraudAlert.FraudPatternType patternType,
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/fraud/alerts/pattern/{}", patternType);
        return ResponseEntity.ok(
                service.getAlertsByPatternType(patternType, pageable).map(FraudAlertMapper::toResponse)
        );
    }

    @GetMapping("/alerts/trader/{traderId}/status/{status}")
    public ResponseEntity<Page<FraudAlertResponse>> getAlertsByTraderAndStatus(
            @PathVariable Long traderId,
            @PathVariable FraudAlert.AlertStatus status,
            @PageableDefault(size = 10, sort = "detectedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("GET /api/v1/fraud/alerts/trader/{}/status/{}", traderId, status);
        return ResponseEntity.ok(
                service.getAlertsByTraderAndStatus(traderId, status, pageable).map(FraudAlertMapper::toResponse)
        );
    }


    @PatchMapping("/alerts/{id}/review")
    public ResponseEntity<FraudAlertResponse> reviewAlert(
            @PathVariable Long id,
            @Valid @RequestBody ReviewAlertRequest request) {
        log.info("PATCH /api/v1/fraud/alerts/{}/review", id);
        FraudAlert reviewed = service.reviewAlert(id, request.getReviewedBy(), request.getReviewNotes());
        return ResponseEntity.ok(FraudAlertMapper.toResponse(reviewed));
    }

    @PatchMapping("/alerts/{id}/dismiss")
    public ResponseEntity<FraudAlertResponse> dismissAlert(
            @PathVariable Long id,
            @Valid @RequestBody ReviewAlertRequest request) {
        log.info("PATCH /api/v1/fraud/alerts/{}/dismiss", id);
        FraudAlert dismissed = service.dismissAlert(id, request.getReviewedBy(), request.getReviewNotes());
        return ResponseEntity.ok(FraudAlertMapper.toResponse(dismissed));
    }

    @PatchMapping("/alerts/{id}/escalate")
    public ResponseEntity<FraudAlertResponse> escalateAlert(@PathVariable Long id) {
        log.info("PATCH /api/v1/fraud/alerts/{}/escalate", id);
        return ResponseEntity.ok(FraudAlertMapper.toResponse(service.escalateAlert(id)));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<FraudDashboardSummary> getDashboardSummary() {
        log.info("GET /api/v1/fraud/dashboard/summary");
        return ResponseEntity.ok(service.getDashboardSummary());
    }

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
}
