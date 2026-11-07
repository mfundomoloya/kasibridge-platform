package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.BidResponse;
import com.kasibridge.procurement.dto.TraderBidOutcomeResponse;
import com.kasibridge.procurement.service.BidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
@Slf4j
public class TraderBidController {

    private final BidService bidService;

    @GetMapping("/my")
    public ResponseEntity<List<BidResponse>> getCurrentTraderBids() {

        log.info(
                "GET /api/v1/bids/my - fetching bids for authenticated trader"
        );

        return ResponseEntity.ok(
                bidService.getCurrentTraderBids()
        );
    }

    @PatchMapping("/{bidId}/withdraw")
    public ResponseEntity<BidResponse> withdrawCurrentTraderBid(@PathVariable("bidId") Long bidId) {
        log.info(
                "PATCH /api/v1/bids/{}/withdraw - withdrawing authenticated trader's bid",
                bidId
        );

        return ResponseEntity.ok(
                bidService.withdrawCurrentTraderBid(bidId)
        );
    }

    @GetMapping("/{bidId}/outcome")
    public ResponseEntity<TraderBidOutcomeResponse> getCurrentTraderBidOutcome(@PathVariable("bidId") Long bidId) {
        log.info(
                "GET /api/v1/bids/{}/outcome "
                        + "- loading authenticated "
                        + "trader bid outcome",
                bidId
        );

        return ResponseEntity.ok(
                bidService
                        .getCurrentTraderBidOutcome(
                                bidId
                        )
        );
    }
}