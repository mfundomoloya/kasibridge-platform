package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.Bid;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BidResponse {

    private Long id;
    private String bidReference;
    private Long tenderId;
    private Long traderId;
    private String bidderAlias;
    private String technicalProposal;
    private BigDecimal priceAmount;
    private Bid.BidStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private BidComplianceResponse compliance;

    public static BidResponse from(Bid bid, BidComplianceResponse compliance) {

        return BidResponse.builder()
                .id(bid.getId())
                .bidReference(bid.getBidReference())
                .tenderId(bid.getTenderId())
                .traderId(bid.getTraderProfileId())
                .bidderAlias(bid.getBidderAlias())
                .technicalProposal(bid.getTechnicalProposal())
                .priceAmount(bid.getPriceAmount())
                .status(bid.getStatus())
                .submittedAt(bid.getSubmittedAt())
                .updatedAt(bid.getUpdatedAt())
                .compliance(compliance)
                .build();
    }
}
