package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.BidEvaluationResponse;
import com.kasibridge.procurement.dto.BlindBidResponse;
import com.kasibridge.procurement.dto.EvaluateBidRequest;
import com.kasibridge.procurement.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenders/{tenderId}/evaluation")
@RequiredArgsConstructor
@Slf4j
public class EvaluationController {

    private final EvaluationService service;

    @GetMapping("/bids")
    public ResponseEntity<List<BlindBidResponse>> getBlindBidsForEvaluation(@PathVariable("tenderId") Long tenderId) {
        log.info(
                "GET /api/v1/tenders/{}/evaluation/bids - fetching blind bids", tenderId
        );

        return ResponseEntity.ok(service.getBlindBidsForEvaluation(tenderId));
    }

    @PostMapping("/bids/{bidId}/score")
    public ResponseEntity<BidEvaluationResponse> scoreBid(@PathVariable("tenderId") Long tenderId,
                                                          @PathVariable("bidId") Long bidId,
                                                          @Valid @RequestBody EvaluateBidRequest request) {

        log.info(
                "POST /api/v1/tenders/{}/evaluation/bids/{}/score - scoring bid",
                tenderId,
                bidId
        );

        return ResponseEntity.ok(service.scoreBid(tenderId, bidId, request));
    }
}
