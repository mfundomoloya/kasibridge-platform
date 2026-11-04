package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AnonymizedBidResponse;
import com.kasibridge.procurement.dto.BidResponse;
import com.kasibridge.procurement.dto.SubmitBidRequest;
import com.kasibridge.procurement.dto.TenderBidActivityResponse;

import java.util.List;

public interface BidService {

    BidResponse submitBid(Long tenderId, SubmitBidRequest request);

    List<AnonymizedBidResponse> getAnonymizedBidsForTender(Long tenderId);

    List<BidResponse> getCurrentTraderBids();

    BidResponse withdrawCurrentTraderBid(Long bidId);

    TenderBidActivityResponse getTenderBidActivity(Long tenderId);
}
