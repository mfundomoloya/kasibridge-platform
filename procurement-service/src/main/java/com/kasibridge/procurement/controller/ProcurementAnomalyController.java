package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.ProcurementAnomalyResponse;
import com.kasibridge.procurement.service.ProcurementAnomalyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenders/{tenderId}/anomalies")
@RequiredArgsConstructor
@Slf4j
public class ProcurementAnomalyController {
    private final ProcurementAnomalyService service;

    @GetMapping
    public ResponseEntity<List<ProcurementAnomalyResponse>> detectTenderAnomalies(@PathVariable Long tenderId) {
        log.info(
                "GET /api/v1/tenders/{}/anomalies - detecting anomalies",
                tenderId
        );

        return ResponseEntity.ok(
                service.detectTenderAnomalies(tenderId)
        );
    }
}
