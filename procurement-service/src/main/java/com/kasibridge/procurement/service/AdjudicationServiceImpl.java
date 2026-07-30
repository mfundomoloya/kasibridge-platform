package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AwardTenderRequest;
import com.kasibridge.procurement.dto.AwardTenderResponse;
import com.kasibridge.procurement.dto.BidRankingResponse;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import com.kasibridge.procurement.exception.AdjudicationException;
import com.kasibridge.procurement.exception.BidEvaluationException;
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
    private final ProcurementAuditService auditService;
    private final CurrentUserService currentUserService;
    private final TenderCommitteeAssignmentRepository assignmentRepository;

    @Override
    public List<BidRankingResponse> getEvaluationSummary(Long tenderId) {

        log.info("Generating adjudication summary for tenderId={}", tenderId);

        if (!tenderRepository.existsById(tenderId)) {
            throw new TenderNotFoundException("Tender not found with ID: " + tenderId);
        }

        Long adjudicatorUserId = currentUserService.getCurrentUserId();

        assertAssignedAdjudicator(tenderId, adjudicatorUserId);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.TENDER_ADJUDICATION_SUMMARY_VIEWED,
                tenderId,
                null,
                adjudicatorUserId,
                "Adjudication summary viewed",
                "Evaluation ranking summary generated for tenderId=" + tenderId
        );

        List<Object[]> averages = scoreRepository.findAverageScoresByTenderId(tenderId);

        if (averages.isEmpty()) {
            throw new BidEvaluationException("No evaluation scores found for tender ID: " + tenderId
            );
        }

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.BID_SCORE_VIEWED,
                tenderId,
                null,
                null,
                "Adjudication summary viewed",
                "Evaluation ranking summary generated for tenderId=" + tenderId
        );

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

        Long adjudicatorUserId = currentUserService.getCurrentUserId();

        log.info("Awarding tenderId={} to bidId={} by adjudicatorUserId={}",
                tenderId,
                request.getWinningBidId(),
                adjudicatorUserId);

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new TenderNotFoundException("Tender not found with ID: " + tenderId));

        assertAssignedAdjudicator(tenderId, adjudicatorUserId);

        if(tender.getStatus() == Tender.TenderStatus.AWARDED){
            auditService.recordFailure(
                    ProcurementAuditEvent.AuditEventType.TENDER_AWARD_REJECTED,
                    tenderId,
                    request.getWinningBidId(),
                    adjudicatorUserId,
                    "Tender award rejected",
                    "Tender has already been awarded"
            );
            throw new AdjudicationException("Tender has already been awarded.");
        }

        if(tender.getStatus() == Tender.TenderStatus.CANCELLED){
            throw new AdjudicationException("Cancelled tender cannot be awarded.");
        }


        if(tender.getStatus() != Tender.TenderStatus.ADJUDICATION){
            auditService.recordFailure(
                    ProcurementAuditEvent.AuditEventType.TENDER_AWARD_REJECTED,
                    tenderId,
                    request.getWinningBidId(),
                    adjudicatorUserId,
                    "Tender award rejected",
                    "Current tender status=" +  tender.getStatus() + ". Award is only allowed when tender is in ADJUDICATION status."
            );
            throw new AdjudicationException("Tender can only be awarded when it is in ADJUDICATION status.");
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
        tender.setAwardedByUserId(adjudicatorUserId);
        tender.setAwardedAt(now);
        tender.setAwardReason(request.getAwardReason().trim());
        tender.setUpdatedAt(now);

        Tender savedTender = tenderRepository.save(tender);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.BID_SCORE_VIEWED,
                tenderId,
                winningBid.getId(),
                adjudicatorUserId,
                "Tender awarded",
                "Winning bid alias: " + winningBid.getBidderAlias() + ", reason: " + savedTender.getAwardReason()
        );

        log.info("Tender awarded: tenderId={} winningBidId={} adjudicatorUserId={}",
                tenderId,
                winningBid.getId(),
                adjudicatorUserId);

        return AwardTenderResponse.builder()
                .tenderId(savedTender.getId())
                .tenderReference(savedTender.getTenderReference())
                .tenderStatus(savedTender.getStatus())
                .winningBidId(winningBid.getId())
                .winningBidReference(winningBid.getBidReference())
                .winningBidderAlias(winningBid.getBidderAlias())
                .winningBidStatus(winningBid.getStatus())
                .adjudicatorUserId(adjudicatorUserId)
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

    private void assertAssignedAdjudicator(Long tenderId, Long adjudicatorUserId) {

        if(currentUserService.hasRole("ROLE_PLATFORM_ADMIN")) {
            return;
        }

        boolean assigned = assignmentRepository.existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
                tenderId,
                adjudicatorUserId,
                TenderCommitteeAssignment.CommitteeRole.ADJUDICATOR
        );

        if(!assigned) {
            auditService.recordFailure(
                    ProcurementAuditEvent.AuditEventType.TENDER_AWARD_REJECTED,
                    tenderId,
                   null,
                    adjudicatorUserId,
                    "Tender award rejected",
                    "User is not assigned as adjudicator to tenderId=" + tenderId
            );

            throw new ProcurementAuthorizationException("User is not assigned as adjudicator to this tender.");
        }
    }
}
