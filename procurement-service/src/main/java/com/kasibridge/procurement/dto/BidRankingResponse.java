package com.kasibridge.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BidRankingResponse {

    private int rank;
    private Long bidId;
    private Long tenderId;
    private String bidderAlias;

    private BigDecimal averageTechnicalScore;
    private BigDecimal averagePriceScore;
    private BigDecimal averageTotalScore;

    private long scoreCount;
}
