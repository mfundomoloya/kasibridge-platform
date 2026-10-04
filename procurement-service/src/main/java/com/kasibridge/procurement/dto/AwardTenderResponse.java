package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.Tender;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AwardTenderResponse {

    private Long tenderId;
    private String tenderReference;
    private Tender.TenderStatus tenderStatus;

    private Long winningBidId;
    private String winningBidReference;
    private String winningBidderAlias;
    private Bid.BidStatus winningBidStatus;

    private Long adjudicatorUserId;
    private String awardReason;
    private LocalDateTime awardedAt;
}
