package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BidEvaluationResponse;
import com.kasibridge.procurement.dto.BlindBidResponse;
import com.kasibridge.procurement.dto.EvaluateBidRequest;

import java.util.List;

public interface EvaluationService {

    List<BlindBidResponse> getBlindBidsForEvaluation(Long tenderId);

    BidEvaluationResponse scoreBid(
            Long tenderId,
            Long bidId,
            EvaluateBidRequest request
    );
}
