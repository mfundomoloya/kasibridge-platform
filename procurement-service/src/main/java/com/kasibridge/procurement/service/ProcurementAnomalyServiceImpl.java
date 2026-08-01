package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BidRankingResponse;
import com.kasibridge.procurement.dto.ProcurementAnomalyRecordResponse;
import com.kasibridge.procurement.dto.ProcurementAnomalyResponse;
import com.kasibridge.procurement.dto.ReviewProcurementAnomalyRequest;
import com.kasibridge.procurement.entity.ProcurementAnomaly;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.exception.AnomalyStateException;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.repository.ProcurementAnomalyRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementAnomalyServiceImpl implements ProcurementAnomalyService {

    private final TenderRepository tenderRepository;
    private final AdjudicationService adjudicationService;
    private final ProcurementAnomalyRepository anomalyRepository;
    private final ProcurementAuditService auditService;
    private final CurrentUserService currentUserService;

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

    @Override
    public List<ProcurementAnomalyRecordResponse> detectAndPersistTenderAnomalies(Long tenderId) {
        List<ProcurementAnomalyResponse> detected = detectTenderAnomalies(tenderId);

        return detected.stream()
                .map(this::persistIfNew)
                .toList();
    }

    @Override
    public Page<ProcurementAnomalyRecordResponse> getAnomalies(Pageable pageable) {
        return anomalyRepository.findAll(pageable)
                .map(ProcurementAnomalyRecordResponse::from);
    }

    @Override
    public Page<ProcurementAnomalyRecordResponse> getAnomaliesByTender(Long tenderId, Pageable pageable) {
        return anomalyRepository.findByTenderId(tenderId, pageable)
                .map(ProcurementAnomalyRecordResponse::from);
    }

    @Override
    public Page<ProcurementAnomalyRecordResponse> getAnomaliesByStatus(ProcurementAnomaly.AnomalyStatus status, Pageable pageable) {
        return anomalyRepository.findByStatus(status, pageable)
                .map(ProcurementAnomalyRecordResponse::from);
    }

    @Override
    public ProcurementAnomalyRecordResponse markReviewed(Long anomalyId, ReviewProcurementAnomalyRequest request) {
        Long reviewerUserId = currentUserService.getCurrentUserId();

        ProcurementAnomaly anomaly = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Procurement anomaly not found with ID: " + anomalyId
                ));

        assertAnomalyIsOpen(anomaly);

        anomaly.setStatus(ProcurementAnomaly.AnomalyStatus.REVIEWED);
        anomaly.setReviewedByUserId(reviewerUserId);
        anomaly.setReviewNotes(request.getReviewNotes().trim());
        anomaly.setReviewedAt(LocalDateTime.now());

        ProcurementAnomaly saved = anomalyRepository.save(anomaly);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.PROCUREMENT_ANOMALY_REVIEWED,
                saved.getTenderId(),
                saved.getBidId(),
                reviewerUserId,
                "Procurement anomaly reviewed",
                "Anomaly reference=" + saved.getAnomalyReference()
        );

        return ProcurementAnomalyRecordResponse.from(saved);
    }

    @Override
    public ProcurementAnomalyRecordResponse dismiss(Long anomalyId, ReviewProcurementAnomalyRequest request) {
        Long reviewerUserId = currentUserService.getCurrentUserId();

        ProcurementAnomaly anomaly = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Procurement anomaly not found with ID: " + anomalyId
                ));

        assertAnomalyIsOpen(anomaly);

        anomaly.setStatus(ProcurementAnomaly.AnomalyStatus.DISMISSED);
        anomaly.setReviewedByUserId(reviewerUserId);
        anomaly.setReviewNotes(request.getReviewNotes().trim());
        anomaly.setReviewedAt(LocalDateTime.now());

        ProcurementAnomaly saved = anomalyRepository.save(anomaly);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.PROCUREMENT_ANOMALY_DISMISSED,
                saved.getTenderId(),
                saved.getBidId(),
                reviewerUserId,
                "Procurement anomaly dismissed",
                "Anomaly reference=" + saved.getAnomalyReference()
        );

        return ProcurementAnomalyRecordResponse.from(saved);
    }

        private void detectHighestScoreBypass(Tender tender, List<ProcurementAnomalyResponse> anomalies) {

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
                                .tenderId(tender.getId())
                                .bidId(tender.getAwardedBidId())
                                .type("HIGHEST_SCORE_BYPASS")
                                .severity("HIGH")
                                .message("Awarded bid is not the highest-ranked evaluated bid.")
                                .evidence("Awarded bidId=" + tender.getAwardedBidId()
                                                + ", highest ranked bidId=" + topRanked.getBidId())
                                .build()
                );
            }
        }

        private void detectRapidAward(Tender tender, List<ProcurementAnomalyResponse> anomalies) {

        if (tender.getAwardedAt() == null || tender.getUpdatedAt() == null) {
                return;

            }
            long minutesBetweenUpdateAndAward = Math.abs(
                    Duration.between(tender.getUpdatedAt(), tender.getAwardedAt()).toMinutes()
            );

            if (minutesBetweenUpdateAndAward <= RAPID_AWARD_THRESHOLD_MINUTES) {
                anomalies.add(

                        ProcurementAnomalyResponse.builder()
                                .tenderId(tender.getId())
                                .bidId(tender.getAwardedBidId())
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
                                .tenderId(tenderId)
                                .bidId(first.getBidId())
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

    private ProcurementAnomalyRecordResponse persistIfNew(ProcurementAnomalyResponse detected) {
        ProcurementAnomaly.AnomalyType type =
                ProcurementAnomaly.AnomalyType.valueOf(detected.getType());

        ProcurementAnomaly.Severity severity =
                ProcurementAnomaly.Severity.valueOf(detected.getSeverity());

        boolean exists = anomalyRepository.existsByTenderIdAndTypeAndEvidence(
                detected.getTenderId(),
                type,
                detected.getEvidence()
        );

        if (exists) {
            return anomalyRepository.findAll()
                    .stream()
                    .filter(a -> a.getTenderId().equals(detected.getTenderId()))
                    .filter(a -> a.getType() == type)
                    .filter(a -> detected.getEvidence().equals(a.getEvidence()))
                    .findFirst()
                    .map(ProcurementAnomalyRecordResponse::from)
                    .orElseThrow();
        }

        ProcurementAnomaly anomaly = ProcurementAnomaly.builder()
                .anomalyReference(generateAnomalyReference())
                .tenderId(detected.getTenderId())
                .bidId(detected.getBidId())
                .type(type)
                .severity(severity)
                .status(ProcurementAnomaly.AnomalyStatus.OPEN)
                .message(detected.getMessage())
                .evidence(detected.getEvidence())
                .build();

        ProcurementAnomaly saved = anomalyRepository.save(anomaly);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.PROCUREMENT_ANOMALY_DETECTED,
                saved.getTenderId(),
                saved.getBidId(),
                null,
                "Procurement anomaly detected",
                "Anomaly reference=" + saved.getAnomalyReference()
                        + ", type=" + saved.getType()
                        + ", severity=" + saved.getSeverity()
        );

        return ProcurementAnomalyRecordResponse.from(saved);
    }

    private String generateAnomalyReference() {
        return "KB-ANOM-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }

    private void assertAnomalyIsOpen(ProcurementAnomaly anomaly) {
        if (anomaly.getStatus() != ProcurementAnomaly.AnomalyStatus.OPEN) {
            Long actorUserId = currentUserService.getCurrentUserId();

            auditService.recordFailure(
                    ProcurementAuditEvent.AuditEventType.PROCUREMENT_ANOMALY_TRANSITION_REJECTED,
                    anomaly.getTenderId(),
                    anomaly.getBidId(),
                    actorUserId,
                    "Procurement anomaly transition rejected",
                    "Anomaly reference=" + anomaly.getAnomalyReference()
                            + ", currentStatus=" + anomaly.getStatus()
            );

            throw new AnomalyStateException(
                    "Only OPEN anomalies can be reviewed or dismissed."
            );
        }
    }
}