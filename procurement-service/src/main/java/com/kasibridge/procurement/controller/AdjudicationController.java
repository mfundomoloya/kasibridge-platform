package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.AwardTenderRequest;
import com.kasibridge.procurement.dto.AwardTenderResponse;
import com.kasibridge.procurement.dto.BidRankingResponse;
import com.kasibridge.procurement.service.AdjudicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/award")
    public ResponseEntity<AwardTenderResponse> awardTender(@PathVariable("tenderId") Long tenderId,
                                                           @Valid @RequestBody AwardTenderRequest request) {
        log.info("POST /api/v1/tenders/{}/adjudication/award awarding tender", tenderId);

        return ResponseEntity.status(HttpStatus.CREATED).body(service.awardTender(tenderId, request));
    }
}
