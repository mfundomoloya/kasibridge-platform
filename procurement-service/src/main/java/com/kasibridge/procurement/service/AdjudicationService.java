package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BidRankingResponse;

import java.util.List;

public interface AdjudicationService {
    List<BidRankingResponse> getEvaluationSummary(Long tenderId);
}
