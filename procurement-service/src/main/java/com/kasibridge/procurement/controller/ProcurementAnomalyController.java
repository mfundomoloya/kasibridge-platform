package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.ProcurementAnomalyRecordResponse;
import com.kasibridge.procurement.dto.ProcurementAnomalyResponse;
import com.kasibridge.procurement.dto.ReviewProcurementAnomalyRequest;
import com.kasibridge.procurement.entity.ProcurementAnomaly;
import com.kasibridge.procurement.service.ProcurementAnomalyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProcurementAnomalyController {
    private final ProcurementAnomalyService service;

    @GetMapping("/api/v1/tenders/{tenderId}/anomalies")
    public ResponseEntity<List<ProcurementAnomalyResponse>> detectTenderAnomalies(@PathVariable Long tenderId) {
        log.info(
                "GET /api/v1/tenders/{}/anomalies - detecting anomalies",
                tenderId
        );

        return ResponseEntity.ok(
                service.detectTenderAnomalies(tenderId)
        );
    }

    @PostMapping("/api/v1/tenders/{tenderId}/anomalies/detect")
    public ResponseEntity<List<ProcurementAnomalyRecordResponse>> detectAndPersistTenderAnomalies(@PathVariable("tenderId") Long tenderId)
    {
        return ResponseEntity.ok(service.detectAndPersistTenderAnomalies(tenderId));
    }

    @GetMapping("/api/v1/procurement/anomalies")
    public ResponseEntity<Page<ProcurementAnomalyRecordResponse>> getAnomalies(@PageableDefault(sort = "detectedAt", direction = Sort.Direction.DESC) Pageable pageable)
    {
        return ResponseEntity.ok(service.getAnomalies(pageable));
    }

    @GetMapping("/api/v1/tenders/{tenderId}/anomaly-records")
    public ResponseEntity<Page<ProcurementAnomalyRecordResponse>> getAnomaliesByTender(@PathVariable("tenderId") Long tenderId,
                                                                                       @PageableDefault(sort = "detectedAt", direction = Sort.Direction.DESC)
        Pageable pageable)
    {
        return ResponseEntity.ok(service.getAnomaliesByTender(tenderId, pageable));
    }

    @GetMapping("/api/v1/procurement/anomalies/status/{status}")
    public ResponseEntity<Page<ProcurementAnomalyRecordResponse>> getAnomaliesByStatus(@PathVariable("status") ProcurementAnomaly.AnomalyStatus status,
                                                                                       @PageableDefault(sort = "detectedAt", direction = Sort.Direction.DESC)Pageable pageable)
    {
        return ResponseEntity.ok(service.getAnomaliesByStatus(status, pageable));
    }

    @PatchMapping("/api/v1/procurement/anomalies/{anomalyId}/review")
    public ResponseEntity<ProcurementAnomalyRecordResponse> markReviewed(@PathVariable("anomalyId") Long anomalyId,
                                                                         @Valid @RequestBody ReviewProcurementAnomalyRequest request)
    {
        return ResponseEntity.ok(service.markReviewed(anomalyId, request));
    }

    @PatchMapping("/api/v1/procurement/anomalies/{anomalyId}/dismiss")
    public ResponseEntity<ProcurementAnomalyRecordResponse> dismiss(@PathVariable("anomalyId") Long anomalyId,
                                                                    @Valid @RequestBody ReviewProcurementAnomalyRequest request)
    {
        return ResponseEntity.ok(service.dismiss(anomalyId, request));
    }
}
