package com.kasibridge.procurement.controller;


import com.kasibridge.procurement.dto.BidResponse;
import com.kasibridge.procurement.dto.SubmitBidRequest;
import com.kasibridge.procurement.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenders/{tenderId}/bids")
@RequiredArgsConstructor
@Slf4j
public class BidController {

    private final BidService service;


    @PostMapping
    public ResponseEntity<BidResponse> submitBid(
            @PathVariable("tenderId") Long tenderId,
            @Valid @RequestBody SubmitBidRequest request
    ) {
        log.info(
                "POST /api/v1/tenders/{}/bids - submitting bid for traderId={}",
                tenderId,
                request.getTraderId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.submitBid(tenderId, request));
    }

    @GetMapping
    public ResponseEntity<List<BidResponse>> getBidsForTender(
            @PathVariable("tenderId") Long tenderId
    ) {
        log.info("GET /api/v1/tenders/{}/bids - fetching bids", tenderId);

        return ResponseEntity.ok(service.getBidsForTender(tenderId));
    }

}
