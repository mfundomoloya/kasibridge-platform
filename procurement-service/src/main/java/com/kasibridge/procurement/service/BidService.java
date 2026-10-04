package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BidResponse;
import com.kasibridge.procurement.dto.SubmitBidRequest;

import java.util.List;

public interface BidService {

    BidResponse submitBid(Long tenderId, SubmitBidRequest request);

    List<BidResponse> getBidsForTender(Long tenderId);
}
