package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.*;

import java.util.List;

public interface BidService {

    BidResponse submitBid(Long tenderId, SubmitBidRequest request);

    List<AnonymizedBidResponse> getAnonymizedBidsForTender(Long tenderId);

    List<BidResponse> getCurrentTraderBids();

    BidResponse withdrawCurrentTraderBid(Long bidId);

    TenderBidActivityResponse getTenderBidActivity(Long tenderId);

    TraderBidOutcomeResponse getCurrentTraderBidOutcome(Long bidId);
}
