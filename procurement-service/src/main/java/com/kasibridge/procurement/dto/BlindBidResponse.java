package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.Bid;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BlindBidResponse {

    private Long bidId;
    private Long tenderId;
    private String bidderAlias;
    private String technicalProposal;

    public static BlindBidResponse from(
            Bid bid
    ) {
        return BlindBidResponse.builder()
                .bidId(bid.getId())
                .tenderId(bid.getTenderId())
                .bidderAlias(bid.getBidderAlias())
                .technicalProposal(bid.getTechnicalProposal())
                .build();
    }
}