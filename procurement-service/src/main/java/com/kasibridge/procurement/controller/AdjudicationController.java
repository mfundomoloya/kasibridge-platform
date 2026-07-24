package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.BidRankingResponse;
import com.kasibridge.procurement.service.AdjudicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenders/{tenderId}/adjudication")
@RequiredArgsConstructor
@Slf4j
public class AdjudicationController {
    private final AdjudicationService service;

    @GetMapping("/summary")
    public ResponseEntity<List<BidRankingResponse>> getEvaluationSummary(@PathVariable("tenderId") Long tenderId) {
        log.info("GET /api/v1/tenders/{}/adjudication/summary - generating summary", tenderId);

        return ResponseEntity.ok(service.getEvaluationSummary(tenderId));
    }
}
