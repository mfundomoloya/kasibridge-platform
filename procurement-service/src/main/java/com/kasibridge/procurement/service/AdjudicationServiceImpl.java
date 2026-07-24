package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BidRankingResponse;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.exception.BidEvaluationException;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.repository.BidEvaluationScoreRepository;
import com.kasibridge.procurement.repository.BidRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
