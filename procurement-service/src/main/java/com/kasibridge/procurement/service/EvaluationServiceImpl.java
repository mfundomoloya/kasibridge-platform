package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BidEvaluationResponse;
import com.kasibridge.procurement.dto.BlindBidResponse;
import com.kasibridge.procurement.dto.EvaluateBidRequest;
import com.kasibridge.procurement.entity.*;
import com.kasibridge.procurement.exception.BidEvaluationException;
import com.kasibridge.procurement.exception.BidNotFoundException;
import com.kasibridge.procurement.exception.ProcurementAuthorizationException;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.repository.BidEvaluationScoreRepository;
import com.kasibridge.procurement.repository.BidRepository;
import com.kasibridge.procurement.repository.TenderCommitteeAssignmentRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationServiceImpl implements EvaluationService {

    private final TenderRepository tenderRepository;
    private final BidRepository bidRepository;
    private final BidEvaluationScoreRepository scoreRepository;
    private final TenderCommitteeAssignmentRepository assignmentRepository;
    private final ProcurementAuditService auditService;
    private final CurrentUserService currentUserService;

    @Override
    public List<BlindBidResponse> getBlindBidsForEvaluation(Long tenderId) {

        Long evaluatorUserId = currentUserService.getCurrentUserId();

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));

        assertAssignedEvaluator(tenderId, evaluatorUserId);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.BID_SCORE_VIEWED,
                tenderId,
                null,
                evaluatorUserId,
                "Evaluator viewed blind bids",
                "Blind bid list accessed for tenderId=" + tenderId
        );

        if(tender.getStatus() != Tender.TenderStatus.EVALUATION){
                throw new BidEvaluationException("Blind bids can only be viewed when tender is in EVALUATION status");
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

        Long evaluatorUserId = currentUserService.getCurrentUserId();

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));

        assertAssignedEvaluator(tenderId, evaluatorUserId);

        if(tender.getStatus() != Tender.TenderStatus.EVALUATION){
            auditService.recordFailure(
                    ProcurementAuditEvent.AuditEventType.BID_SCORE_REJECTED_INVALID_TENDER_STATUS,
                    tenderId,
                    bidId,
                    evaluatorUserId,
                    "Bid scoring rejected due to invalid tender status",
                    "Current tender status=" + tender.getStatus() + ". Scoring is only allowed when tender is in EVALUATION."
            );
            throw new BidEvaluationException("Bid scoring is only allowed when tender is in EVALUATION status.");
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException("Bid not found with ID: " + bidId));

        if(!bid.getTenderId().equals(tenderId)){
            throw new BidEvaluationException("Bid does not belong to this tender ID: " + tenderId);
        }

        if(bid.getStatus() != Bid.BidStatus.COMPLIANT && bid.getStatus() != Bid.BidStatus.UNDER_EVALUATION){
            throw new BidEvaluationException("Only compliant bids can be evaluated.");
        }

        boolean alreadyScored = scoreRepository.existsByTenderIdAndBidIdAndEvaluatorUserId(tenderId,bidId, evaluatorUserId);

        if(alreadyScored){
            auditService.recordFailure(
                    ProcurementAuditEvent.AuditEventType.BID_SCORE_REJECTED_DUPLICATE,
                    tenderId,
                    bidId,
                    evaluatorUserId,
                    "Duplicate bid score rejected",
                    "Evaluator has already scored this bid"
            );

            throw new BidEvaluationException("Evaluator has already scored this bid.");
        }


        BigDecimal lowestBidPrice = bidRepository.findLowestBidPriceByTenderId(tenderId);

        BigDecimal maxPricePoints = new BigDecimal("20.00");

        BigDecimal priceScore = calculatePriceScore(
                lowestBidPrice,
                bid.getPriceAmount(),
                maxPricePoints
        );

        BigDecimal totalScore = calculateTotalScore(
                request.getTechnicalScore(),
                priceScore
        );

        BidEvaluationScore score = BidEvaluationScore.builder()
                .tenderId(tenderId)
                .bidId(bidId)
                .evaluatorUserId(evaluatorUserId)
                .technicalScore(request.getTechnicalScore())
                .priceScore(priceScore)
                .totalScore(totalScore)
                .comments(request.getComments())
                .build();

        BidEvaluationScore savedScore = scoreRepository.save(score);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.BID_SCORE_SUBMITTED,
                tenderId,
                bidId,
                evaluatorUserId,
                "Bid score submitted",
                "technicalScore=" + savedScore.getTechnicalScore()
                        +", priceScore=" + savedScore.getPriceScore()
                        +", totalScore=" + savedScore.getTotalScore()
        );

        if(bid.getStatus() == Bid.BidStatus.COMPLIANT){
            bid.setStatus(Bid.BidStatus.UNDER_EVALUATION);
            bidRepository.save(bid);
        }

        log.info(
                "Bid scored: tenderId={} bidId={} evaluatorUserId={} technicalScore={} totalScore={} priceScore={}",
                tenderId,
                bidId,
                evaluatorUserId,
                request.getTechnicalScore(),
                priceScore,
                totalScore
        );

        return BidEvaluationResponse.from(savedScore);
    }

    //helper method
    private void assertAssignedEvaluator(Long tenderId, Long evaluatorUserId) {

        if(currentUserService.hasRole("ROLE_PLATFORM_ADMIN")) {
            return;
        }

        boolean assigned = assignmentRepository.
                existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(tenderId, evaluatorUserId, TenderCommitteeAssignment.CommitteeRole.EVALUATOR);

        if(!assigned){
            auditService.recordFailure(
                    ProcurementAuditEvent.AuditEventType.BID_SCORE_REJECTED_UNASSIGNED_EVALUATOR,
                    tenderId,
                    null,
                    evaluatorUserId,
                    "Evaluator access rejected",
                    "User is not assigned as evaluator to tenderId=" + tenderId
            );

            throw new ProcurementAuthorizationException("User is not assigned as evaluator to this tender.");
        }
    }


    private BigDecimal calculatePriceScore(
            BigDecimal lowestBidPrice,
            BigDecimal currentBidPrice,
            BigDecimal maxPricePoints
    ) {
        if (lowestBidPrice == null || lowestBidPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Lowest bid price must be greater than 0");
        }

        if (currentBidPrice == null || currentBidPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Current bid price must be greater than 0");
        }

        return lowestBidPrice
                .divide(currentBidPrice, 6, RoundingMode.HALF_UP)
                .multiply(maxPricePoints)
                .setScale(2, RoundingMode.HALF_UP);
    }


    private BigDecimal calculateWeightedTechnicalScore(BigDecimal technicalScore) {
        BigDecimal technicalWeight = new BigDecimal("0.80");

        return technicalScore
                .multiply(technicalWeight)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalScore(
            BigDecimal technicalScore,
            BigDecimal priceScore
    ) {
        BigDecimal weightedTechnicalScore = calculateWeightedTechnicalScore(technicalScore);

        return weightedTechnicalScore
                .add(priceScore)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
