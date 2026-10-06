package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.Bid;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnonymizedBidResponse {

    private Long id;
    private String bidReference;
    private Long tenderId;
    private String bidderAlias;
    private String technicalProposal;
    private Bid.BidStatus status;
    private LocalDateTime submittedAt;
    private BidComplianceResponse compliance;

    public static AnonymizedBidResponse from(
            Bid bid,
            BidComplianceResponse compliance
    ) {
        return AnonymizedBidResponse.builder()
                .id(bid.getId())
                .bidReference(bid.getBidReference())
                .tenderId(bid.getTenderId())
                .bidderAlias(bid.getBidderAlias())
                .technicalProposal(bid.getTechnicalProposal())
                .status(bid.getStatus())
                .submittedAt(bid.getSubmittedAt())
                .compliance(compliance)
                .build();
    }
}