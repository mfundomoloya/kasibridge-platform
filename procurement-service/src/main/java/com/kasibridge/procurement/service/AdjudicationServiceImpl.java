package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AwardTenderRequest;
import com.kasibridge.procurement.dto.AwardTenderResponse;
import com.kasibridge.procurement.dto.BidRankingResponse;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.exception.AdjudicationException;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdjudicationServiceImpl implements AdjudicationService {

    private final TenderRepository tenderRepository;
    private final BidRepository bidRepository;
    private final BidEvaluationScoreRepository scoreRepository;

    @Override
    public List<BidRankingResponse> getEvaluationSummary(Long tenderId) {
        log.info("Generating adjudication summary for tenderId={}", tenderId);

        if (!tenderRepository.existsById(tenderId)) {
            throw new TenderNotFoundException("Tender not found with ID: " + tenderId);
        }

        List<Object[]> averages = scoreRepository.findAverageScoresByTenderId(tenderId);

        if (averages.isEmpty()) {
            throw new BidEvaluationException("No evaluation scores found for tender ID: " + tenderId
            );
        }

        Map<Long, Bid> bidMap = bidRepository.findByTenderId(tenderId)
                .stream()
                .collect(Collectors.toMap(Bid::getId, bid -> bid));

        List<BidRankingResponse> unranked = averages.stream()
                .map(row -> {
                    Long bidId = (Long) row[0];

                    BigDecimal avgTechnical = toBigDecimal(row[1]);
                    BigDecimal avgPrice = toBigDecimal(row[2]);
                    BigDecimal avgTotal = toBigDecimal(row[3]);
                    Long scoreCount = (Long) row[4];

                    Bid bid = bidMap.get(bidId);

                    if (bid == null) {
                        throw new BidEvaluationException("Evaluation score references missing bid ID: " + bidId);
                    }

                    return BidRankingResponse.builder()
                            .rank(0)
                            .bidId(bidId)
                            .tenderId(tenderId)
                            .bidderAlias(bid.getBidderAlias())
                            .averageTechnicalScore(avgTechnical)
                            .averagePriceScore(avgPrice)
                            .averageTotalScore(avgTotal)
                            .scoreCount(scoreCount)
                            .build();

                })
                .sorted(
                        Comparator.comparing(
                                BidRankingResponse::getAverageTotalScore
                        ).reversed()
                )
                .toList();

        return assignRanks(unranked);
    }

    @Override
    @Transactional
    public AwardTenderResponse awardTender(Long tenderId, AwardTenderRequest request) {
        log.info("Awarding tenderId={} to bidId={} by adjudicatorUserId={}",
                tenderId,
                request.getWinningBidId(),
                request.getAdjudicatorUserId());

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));

        if(tender.getStatus() == Tender.TenderStatus.AWARDED){
            throw new AdjudicationException("Tender has already been awarded.");
        }

        if(tender.getStatus() == Tender.TenderStatus.CANCELLED){
            throw new AdjudicationException("Cancelled tender cannot be awarded.");
        }

        Bid winningBid = bidRepository.findById(request.getWinningBidId())
                .orElseThrow(() -> new AdjudicationException("Winning bid not found with ID: " + request.getWinningBidId()));

        if(!winningBid.getTenderId().equals(tenderId)) {
            throw new AdjudicationException("Winning bid does not belong to tender ID: " + tenderId);
        }

        boolean hasEvaluationScore = scoreRepository.existsByTenderIdAndBidId(tenderId, winningBid.getId());

        if(!hasEvaluationScore){
            throw new AdjudicationException("Winning bid must have at least one evaluation score before award.");
        }

        if(winningBid.getStatus() != Bid.BidStatus.UNDER_EVALUATION && winningBid.getStatus() != Bid.BidStatus.COMPLIANT){
            throw new AdjudicationException("Only compliant or evaluated bids can be awarded.");
        }

        LocalDateTime now = LocalDateTime.now();

        winningBid.setStatus(Bid.BidStatus.AWARDED);
        bidRepository.save(winningBid);

        rejectOtherEligibleBids(tenderId, winningBid.getId());

        tender.setStatus(Tender.TenderStatus.AWARDED);
        tender.setAwardedBidId(winningBid.getId());
        tender.setAwardedByUserId(request.getAdjudicatorUserId());
        tender.setAwardedAt(now);
        tender.setAwardReason(request.getAwardReason().trim());
        tender.setUpdatedAt(now);

        Tender savedTender = tenderRepository.save(tender);

        log.info("Tender awarded: tenderId={} winningBidId={} adjudicatorUserId={}",
                tenderId,
                winningBid.getId(),
                request.getAdjudicatorUserId());

        return AwardTenderResponse.builder()
                .tenderId(savedTender.getId())
                .tenderReference(savedTender.getTenderReference())
                .tenderStatus(savedTender.getStatus())
                .winningBidId(winningBid.getId())
                .winningBidReference(winningBid.getBidReference())
                .winningBidAlias(winningBid.getBidderAlias())
                .winningBidStatus(winningBid.getStatus())
                .adjudicatorUserId(request.getAdjudicatorUserId())
                .awardReason(savedTender.getAwardReason())
                .awardedAt(savedTender.getAwardedAt())
                .build();
    }


    private void rejectOtherEligibleBids(Long tenderId, Long winningBidId) {
        Set<Bid.BidStatus> eligibleStatuses = Set.of(
                Bid.BidStatus.COMPLIANT,
                Bid.BidStatus.UNDER_EVALUATION,
                Bid.BidStatus.RECOMMENDED
        );

        List<Bid> otherBids = bidRepository.findByTenderIdAndStatusIn(tenderId, eligibleStatuses);

        for(Bid bid : otherBids) {
            if(!bid.getId().equals(winningBidId)) {
                bid.setStatus(Bid.BidStatus.REJECTED);
            }
        }

        bidRepository.saveAll(otherBids);
    }

    private List<BidRankingResponse> assignRanks(List<BidRankingResponse> unranked) {
        int rank = 1;

        for(BidRankingResponse response: unranked) {
            response.setRank(rank);
            rank++;
        }
        return unranked;
    }

    private BigDecimal toBigDecimal(Object value) {

            if (value instanceof BigDecimal bigDecimal) {
                return bigDecimal.setScale(2, RoundingMode.HALF_UP);

            }
            if (value instanceof Double doubleValue) {
                return BigDecimal.valueOf(doubleValue)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue())
                        .setScale(2, RoundingMode.HALF_UP);
            }
            throw new BidEvaluationException("Unable to convert score average to number");
    }
}
