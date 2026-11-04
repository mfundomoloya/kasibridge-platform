package com.kasibridge.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TenderBidActivityResponse {

    private Long tenderId;

    private String tenderReference;

    private long totalBids;

    private long activeBids;

    private long compliantBids;

    private long complianceFailedBids;

    private long withdrawnBids;

    private long underEvaluationBids;

    private LocalDateTime lastBidSubmittedAt;
}