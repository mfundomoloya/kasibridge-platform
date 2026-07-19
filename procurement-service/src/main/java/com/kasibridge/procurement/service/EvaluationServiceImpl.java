package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BidEvaluationResponse;
import com.kasibridge.procurement.dto.BlindBidResponse;
import com.kasibridge.procurement.dto.EvaluateBidRequest;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.BidEvaluationScore;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.exception.BidEvaluationException;
import com.kasibridge.procurement.exception.BidNotFoundException;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.repository.BidEvaluationScoreRepository;
import com.kasibridge.procurement.repository.BidRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationServiceImpl implements EvaluationService {

    private final TenderRepository tenderRepository;
    private final BidRepository bidRepository;
    private final BidEvaluationScoreRepository scoreRepository;

    @Override
    public List<BlindBidResponse> getBlindBidsForEvaluation(Long tenderId) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));

        if(tender.getStatus() != Tender.TenderStatus.PUBLISHED &&  tender.getStatus() != Tender.TenderStatus.EVALUATION){

                throw new BidEvaluationException("Bids can only be evaluated when tender is PUBLISHED or EVALUATION");
        }

        return bidRepository.findByTenderId(tenderId)
                .stream()
                .filter(bid -> bid.getStatus() == Bid.BidStatus.COMPLIANT || bid.getStatus() == Bid.BidStatus.UNDER_EVALUATION)
                .map(BlindBidResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public BidEvaluationResponse scoreBid(Long tenderId, Long bidId, EvaluateBidRequest request) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));

        if(tender.getStatus() != Tender.TenderStatus.PUBLISHED &&  tender.getStatus() != Tender.TenderStatus.EVALUATION){

            throw new BidEvaluationException("Bid scoring is only allowed when tender is PUBLISHED or EVALUATION");
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException("Bid not found with ID: " + bidId));

        if(!bid.getTenderId().equals(tenderId)){
            throw new BidEvaluationException("Bid does not belong to this tender ID: " + tenderId);
        }

        if(bid.getStatus() != Bid.BidStatus.COMPLIANT && bid.getStatus() != Bid.BidStatus.UNDER_EVALUATION){
            throw new BidEvaluationException("Only compliant bids can be evaluated");
        }

        boolean alreadyScored = scoreRepository.existsByBidIdAndEvaluatorUserId(bidId, request.getEvaluatorUserId());

        if(alreadyScored){
            throw new BidEvaluationException("Evaluation has already scored this bid");
        }

        BigDecimal totalScore = request.getTechnicalScore()
                .add(request.getPriceScore());

        BidEvaluationScore score = BidEvaluationScore.builder()
                .tenderId(tenderId)
                .bidId(bidId)
                .evaluatorUserId(request.getEvaluatorUserId())
                .technicalScore(request.getTechnicalScore())
                .priceScore(request.getPriceScore())
                .totalScore(totalScore)
                .comments(request.getComments())
                .build();

        BidEvaluationScore savedScore = scoreRepository.save(score);

        if(bid.getStatus() == Bid.BidStatus.COMPLIANT){
            bid.setStatus(Bid.BidStatus.UNDER_EVALUATION);
            bidRepository.save(bid);
        }

        log.info(
                "Bid scored: tenderId={} bidId={} evaluatorUserId={} totalScore={}",
                tenderId,
                bidId,
                request.getEvaluatorUserId(),
                totalScore
        );

        return BidEvaluationResponse.from(savedScore);
    }
}
