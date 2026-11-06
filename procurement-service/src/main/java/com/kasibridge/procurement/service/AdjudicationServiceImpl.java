package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AwardTenderRequest;
import com.kasibridge.procurement.dto.AwardTenderResponse;
import com.kasibridge.procurement.dto.BidRankingResponse;
import com.kasibridge.procurement.dto.EvaluationProgressResponse;
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
public class AdjudicationServiceImpl
        implements AdjudicationService {

    private final TenderRepository
            tenderRepository;

    private final BidRepository
            bidRepository;

    private final BidEvaluationScoreRepository
            scoreRepository;

    private final ProcurementAuditService
            auditService;

    private final CurrentUserService
            currentUserService;

    private final TenderCommitteeAssignmentRepository
            assignmentRepository;

    private final ProcurementNotificationService
            procurementNotificationService;

    private final EvaluationService
            evaluationService;

    @Override
    @Transactional
    public List<BidRankingResponse>
    getEvaluationSummary(
            Long tenderId
    ) {
        Long adjudicatorUserId =
                currentUserService
                        .getCurrentUserId();

        log.info(
                "Generating adjudication summary: "
                        + "tenderId={} "
                        + "adjudicatorUserId={}",
                tenderId,
                adjudicatorUserId
        );

        Tender tender =
                tenderRepository
                        .findById(tenderId)
                        .orElseThrow(
                                () ->
                                        new TenderNotFoundException(
                                                "Tender not found "
                                                        + "with ID: "
                                                        + tenderId
                                        )
                        );

        assertAssignedAdjudicator(
                tenderId,
                adjudicatorUserId
        );

        if (
                tender.getStatus()
                        != Tender.TenderStatus.ADJUDICATION
                        && tender.getStatus()
                        != Tender.TenderStatus.AWARDED
        ) {
            throw new AdjudicationException(
                    "The adjudication summary is "
                            + "available only when "
                            + "the tender is in "
                            + "ADJUDICATION or "
                            + "AWARDED status."
            );
        }

        List<Object[]> averages =
                scoreRepository
                        .findAverageScoresByTenderId(
                                tenderId
                        );

        if (averages.isEmpty()) {
            throw new BidEvaluationException(
                    "No evaluation scores found "
                            + "for tender ID: "
                            + tenderId
            );
        }

        Map<Long, Bid> bidMap =
                bidRepository
                        .findByTenderId(
                                tenderId
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Bid::getId,
                                        bid -> bid
                                )
                        );

        List<BidRankingResponse> unranked =
                averages
                        .stream()
                        .map(
                                row ->
                                        createRankingResponse(
                                                tenderId,
                                                row,
                                                bidMap
                                        )
                        )
                        .sorted(
                                Comparator
                                        .comparing(
                                                BidRankingResponse
                                                        ::getAverageTotalScore
                                        )
                                        .reversed()
                                        .thenComparing(
                                                BidRankingResponse
                                                        ::getBidId
                                        )
                        )
                        .toList();

        List<BidRankingResponse> ranked =
                assignRanks(
                        unranked
                );

        auditService.recordSuccess(
                ProcurementAuditEvent
                        .AuditEventType
                        .TENDER_ADJUDICATION_SUMMARY_VIEWED,
                tenderId,
                null,
                adjudicatorUserId,
                "Adjudication summary viewed",
                "Tender reference="
                        + tender.getTenderReference()
                        + ", rankedBidCount="
                        + ranked.size()
        );

        return ranked;
    }

    @Override
    @Transactional
    public AwardTenderResponse awardTender(
            Long tenderId,
            AwardTenderRequest request
    ) {
        Long adjudicatorUserId =
                currentUserService
                        .getCurrentUserId();

        validateAwardRequest(
                request
        );

        log.info(
                "Awarding tender: tenderId={} "
                        + "winningBidId={} "
                        + "adjudicatorUserId={}",
                tenderId,
                request.getWinningBidId(),
                adjudicatorUserId
        );

        Tender tender =
                tenderRepository
                        .findById(tenderId)
                        .orElseThrow(
                                () ->
                                        new TenderNotFoundException(
                                                "Tender not found "
                                                        + "with ID: "
                                                        + tenderId
                                        )
                        );

        assertAssignedAdjudicator(
                tenderId,
                adjudicatorUserId
        );

        if (
                tender.getStatus()
                        == Tender.TenderStatus.AWARDED
        ) {
            recordRejectedAward(
                    tenderId,
                    request.getWinningBidId(),
                    adjudicatorUserId,
                    "Tender has already been awarded"
            );

            throw new AdjudicationException(
                    "Tender has already been awarded."
            );
        }

        if (
                tender.getStatus()
                        == Tender.TenderStatus.CANCELLED
        ) {
            recordRejectedAward(
                    tenderId,
                    request.getWinningBidId(),
                    adjudicatorUserId,
                    "Cancelled tender cannot "
                            + "be awarded"
            );

            throw new AdjudicationException(
                    "Cancelled tender cannot "
                            + "be awarded."
            );
        }

        if (
                tender.getStatus()
                        != Tender.TenderStatus.ADJUDICATION
        ) {
            recordRejectedAward(
                    tenderId,
                    request.getWinningBidId(),
                    adjudicatorUserId,
                    "Current tender status="
                            + tender.getStatus()
                            + ". Award is allowed "
                            + "only in ADJUDICATION."
            );

            throw new AdjudicationException(
                    "Tender can only be awarded "
                            + "when it is in "
                            + "ADJUDICATION status."
            );
        }

        EvaluationProgressResponse progress =
                evaluationService
                        .getEvaluationProgress(
                                tenderId
                        );

        if (
                !progress.isEvaluationComplete()
        ) {
            throw new AdjudicationException(
                    progress.getBlockingReason()
                            == null
                            || progress
                            .getBlockingReason()
                            .isBlank()
                            ? (
                            "Tender evaluation "
                                    + "is incomplete."
                    )
                            : progress
                            .getBlockingReason()
            );
        }

        Bid winningBid =
                bidRepository
                        .findById(
                                request
                                        .getWinningBidId()
                        )
                        .orElseThrow(
                                () ->
                                        new AdjudicationException(
                                                "Winning bid not "
                                                        + "found with ID: "
                                                        + request
                                                        .getWinningBidId()
                                        )
                        );

        if (
                !winningBid
                        .getTenderId()
                        .equals(tenderId)
        ) {
            throw new AdjudicationException(
                    "Winning bid does not belong "
                            + "to tender ID: "
                            + tenderId
            );
        }

        boolean hasEvaluationScore =
                scoreRepository
                        .existsByTenderIdAndBidId(
                                tenderId,
                                winningBid.getId()
                        );

        if (!hasEvaluationScore) {
            throw new AdjudicationException(
                    "Winning bid must have at least "
                            + "one evaluation score "
                            + "before award."
            );
        }

        if (
                winningBid.getStatus()
                        != Bid.BidStatus.UNDER_EVALUATION
                        && winningBid.getStatus()
                        != Bid.BidStatus.COMPLIANT
                        && winningBid.getStatus()
                        != Bid.BidStatus.RECOMMENDED
        ) {
            throw new AdjudicationException(
                    "Only compliant, evaluated or "
                            + "recommended bids can "
                            + "be awarded."
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        winningBid.setStatus(
                Bid.BidStatus.AWARDED
        );

        bidRepository.save(
                winningBid
        );

        rejectOtherEligibleBids(
                tenderId,
                winningBid.getId()
        );

        tender.setStatus(
                Tender.TenderStatus.AWARDED
        );

        tender.setAwardedBidId(
                winningBid.getId()
        );

        tender.setAwardedByUserId(
                adjudicatorUserId
        );

        tender.setAwardedAt(now);

        tender.setAwardReason(
                request
                        .getAwardReason()
                        .trim()
        );

        tender.setUpdatedAt(now);

        Tender savedTender =
                tenderRepository.save(
                        tender
                );

        auditService.recordSuccess(
                ProcurementAuditEvent
                        .AuditEventType
                        .TENDER_AWARDED,
                tenderId,
                winningBid.getId(),
                adjudicatorUserId,
                "Tender awarded",
                "Tender reference="
                        + savedTender
                        .getTenderReference()
                        + ", winningBidAlias="
                        + winningBid
                        .getBidderAlias()
                        + ", awardReason="
                        + savedTender
                        .getAwardReason()
        );

        procurementNotificationService
                .queueTenderAwarded(
                        winningBid,
                        savedTender
                );

        log.info(
                "Tender awarded: tenderId={} "
                        + "winningBidId={} "
                        + "adjudicatorUserId={}",
                tenderId,
                winningBid.getId(),
                adjudicatorUserId
        );

        return AwardTenderResponse
                .builder()
                .tenderId(
                        savedTender.getId()
                )
                .tenderReference(
                        savedTender
                                .getTenderReference()
                )
                .tenderStatus(
                        savedTender.getStatus()
                )
                .winningBidId(
                        winningBid.getId()
                )
                .winningBidReference(
                        winningBid
                                .getBidReference()
                )
                .winningBidderAlias(
                        winningBid
                                .getBidderAlias()
                )
                .winningBidStatus(
                        winningBid.getStatus()
                )
                .adjudicatorUserId(
                        adjudicatorUserId
                )
                .awardReason(
                        savedTender
                                .getAwardReason()
                )
                .awardedAt(
                        savedTender.getAwardedAt()
                )
                .build();
    }

    private BidRankingResponse
    createRankingResponse(
            Long tenderId,
            Object[] row,
            Map<Long, Bid> bidMap
    ) {
        Long bidId =
                (Long) row[0];

        BigDecimal averageTechnical =
                toBigDecimal(
                        row[1]
                );

        BigDecimal averagePrice =
                toBigDecimal(
                        row[2]
                );

        BigDecimal averageTotal =
                toBigDecimal(
                        row[3]
                );

        long scoreCount =
                toLong(
                        row[4]
                );

        Bid bid =
                bidMap.get(
                        bidId
                );

        if (bid == null) {
            throw new BidEvaluationException(
                    "Evaluation score references "
                            + "missing bid ID: "
                            + bidId
            );
        }

        return BidRankingResponse
                .builder()
                .rank(0)
                .bidId(bidId)
                .tenderId(tenderId)
                .bidderAlias(
                        bid.getBidderAlias()
                )
                .averageTechnicalScore(
                        averageTechnical
                )
                .averagePriceScore(
                        averagePrice
                )
                .averageTotalScore(
                        averageTotal
                )
                .scoreCount(
                        scoreCount
                )
                .build();
    }

    private void rejectOtherEligibleBids(
            Long tenderId,
            Long winningBidId
    ) {
        Set<Bid.BidStatus>
                eligibleStatuses =
                Set.of(
                        Bid.BidStatus.COMPLIANT,
                        Bid.BidStatus
                                .UNDER_EVALUATION,
                        Bid.BidStatus.RECOMMENDED
                );

        List<Bid> otherBids =
                bidRepository
                        .findByTenderIdAndStatusIn(
                                tenderId,
                                eligibleStatuses
                        );

        for (Bid bid : otherBids) {
            if (
                    !bid.getId()
                            .equals(
                                    winningBidId
                            )
            ) {
                bid.setStatus(
                        Bid.BidStatus.REJECTED
                );
            }
        }

        if (!otherBids.isEmpty()) {
            bidRepository.saveAll(
                    otherBids
            );
        }
    }

    private List<BidRankingResponse>
    assignRanks(
            List<BidRankingResponse> unranked
    ) {
        int rank = 1;

        for (
                BidRankingResponse response
                : unranked
        ) {
            response.setRank(rank);
            rank++;
        }

        return unranked;
    }

    private BigDecimal toBigDecimal(
            Object value
    ) {
        if (
                value
                        instanceof BigDecimal bigDecimal
        ) {
            return bigDecimal
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        if (
                value
                        instanceof Double doubleValue
        ) {
            return BigDecimal
                    .valueOf(
                            doubleValue
                    )
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        if (
                value
                        instanceof Number number
        ) {
            return BigDecimal
                    .valueOf(
                            number.doubleValue()
                    )
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        throw new BidEvaluationException(
                "Unable to convert score "
                        + "average to number."
        );
    }

    private long toLong(
            Object value
    ) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        throw new BidEvaluationException(
                "Unable to convert score "
                        + "count to number."
        );
    }

    private void validateAwardRequest(
            AwardTenderRequest request
    ) {
        if (request == null) {
            throw new AdjudicationException(
                    "An award request is required."
            );
        }

        if (
                request.getWinningBidId() == null
                        || request.getWinningBidId() <= 0
        ) {
            throw new AdjudicationException(
                    "A valid winning bid ID "
                            + "is required."
            );
        }

        if (
                request.getAwardReason() == null
                        || request
                        .getAwardReason()
                        .trim()
                        .length() < 10
        ) {
            throw new AdjudicationException(
                    "The award reason must contain "
                            + "at least 10 characters."
            );
        }

        if (
                request
                        .getAwardReason()
                        .trim()
                        .length() > 1000
        ) {
            throw new AdjudicationException(
                    "The award reason cannot "
                            + "exceed 1000 characters."
            );
        }
    }

    private void recordRejectedAward(
            Long tenderId,
            Long bidId,
            Long adjudicatorUserId,
            String details
    ) {
        auditService.recordFailure(
                ProcurementAuditEvent
                        .AuditEventType
                        .TENDER_AWARD_REJECTED,
                tenderId,
                bidId,
                adjudicatorUserId,
                "Tender award rejected",
                details
        );
    }

    private void assertAssignedAdjudicator(
            Long tenderId,
            Long adjudicatorUserId
    ) {
        if (
                adjudicatorUserId == null
                        || adjudicatorUserId <= 0
        ) {
            throw new ProcurementAuthorizationException(
                    "An authenticated adjudicator "
                            + "user ID is required."
            );
        }

        boolean assigned =
                assignmentRepository
                        .existsByTenderIdAndUserIdAndCommitteeRoleAndActiveTrue(
                                tenderId,
                                adjudicatorUserId,
                                TenderCommitteeAssignment
                                        .CommitteeRole
                                        .ADJUDICATOR
                        );

        if (!assigned) {
            auditService.recordFailure(
                    ProcurementAuditEvent
                            .AuditEventType
                            .TENDER_AWARD_REJECTED,
                    tenderId,
                    null,
                    adjudicatorUserId,
                    "Adjudicator access rejected",
                    "User is not assigned as "
                            + "adjudicator to tenderId="
                            + tenderId
            );

            throw new ProcurementAuthorizationException(
                    "User is not assigned as "
                            + "adjudicator to this tender."
            );
        }
    }
}