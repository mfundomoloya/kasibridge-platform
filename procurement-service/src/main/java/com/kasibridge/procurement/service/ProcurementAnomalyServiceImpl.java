package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BidRankingResponse;
import com.kasibridge.procurement.dto.ProcurementAnomalyResponse;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementAnomalyServiceImpl implements ProcurementAnomalyService {

    private final TenderRepository tenderRepository;
    private final AdjudicationService adjudicationService;

    private static final long RAPID_AWARD_THRESHOLD_MINUTES = 10;

    @Override
    public List<ProcurementAnomalyResponse> detectTenderAnomalies(Long tenderId) {

            log.info("Detecting procurement anomalies for tenderId={}", tenderId);

            Tender tender = tenderRepository.findById(tenderId)
                    .orElseThrow(() -> new TenderNotFoundException(
                            "Tender not found with ID: " + tenderId
                    ));

            List<ProcurementAnomalyResponse> anomalies = new ArrayList<>();

            detectHighestScoreBypass(tender, anomalies);
            detectRapidAward(tender, anomalies);
            detectLargeScoreGap(tenderId, anomalies);
            return anomalies;
        }

        private void detectHighestScoreBypass(
        Tender tender,
        List<ProcurementAnomalyResponse> anomalies
) {

            if (tender.getAwardedBidId() == null) {
                return;
            }

            List<BidRankingResponse> ranking = adjudicationService.getEvaluationSummary(
                    tender.getId()
            );

            if (ranking.isEmpty()) {
                return;
            }

            BidRankingResponse topRanked = ranking.get(0);

            if (!topRanked.getBidId().equals(tender.getAwardedBidId())) {
                anomalies.add(
                        ProcurementAnomalyResponse.builder()
                                .type("HIGHEST_SCORE_BYPASS")
                                .severity("HIGH")
                                .message("Awarded bid is not the highest-ranked evaluated bid.")
                                .evidence("Awarded bidId=" + tender.getAwardedBidId()
                                                + ", highest ranked bidId=" + topRanked.getBidId())
                                .build()
                );
            }
        }


        private void detectRapidAward(
        Tender tender,
        List<ProcurementAnomalyResponse> anomalies
) {

            if (tender.getAwardedAt() == null || tender.getUpdatedAt() == null) {
                return;

            }
            long minutesBetweenUpdateAndAward = Math.abs(
                    Duration.between(tender.getUpdatedAt(), tender.getAwardedAt()).toMinutes()
            );

            if (minutesBetweenUpdateAndAward <= RAPID_AWARD_THRESHOLD_MINUTES) {
                anomalies.add(

                        ProcurementAnomalyResponse.builder()
                                .type("RAPID_AWARD")
                                .severity("MEDIUM")
                                .message("Tender was awarded very soon after the latest tender update.")
                                .evidence("Award occurred within "
                                                + minutesBetweenUpdateAndAward
                                                + " minutes of the last tender update.")
                                .build()
                );
            }
        }

        private void detectLargeScoreGap(
        Long tenderId,
        List<ProcurementAnomalyResponse> anomalies
        )

        {
            List<BidRankingResponse> ranking = adjudicationService.getEvaluationSummary(tenderId);
            if (ranking.size() < 2) {
                return;
            }
            BidRankingResponse first = ranking.get(0);
            BidRankingResponse second = ranking.get(1);
            var scoreGap = first.getAverageTotalScore()
                    .subtract(second.getAverageTotalScore())
                    .abs();
            if (scoreGap.doubleValue() >= 25.0) {
                anomalies.add(
                        ProcurementAnomalyResponse.builder()
                                .type("LARGE_SCORE_GAP")
                                .severity("LOW")
                                .message("Top-ranked bid has a large score gap over second-ranked bid.")
                                .evidence("Top bidId=" + first.getBidId()
                                                + ", second bidId=" + second.getBidId()
                                                + ", score gap=" + scoreGap)
                                .build()
                );
            }
        }

}