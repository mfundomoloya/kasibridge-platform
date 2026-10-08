package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.*;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.BidComplianceResult;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.exception.*;
import com.kasibridge.procurement.repository.BidComplianceResultRepository;
import com.kasibridge.procurement.repository.BidRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidServiceImpl implements BidService {

    private final BidRepository bidRepository;
    private final BidComplianceResultRepository complianceRepository;
    private final TenderRepository tenderRepository;
    private final ComplianceGatekeeperService gatekeeperService;
    private final ProcurementAuditService auditService;
    private final CurrentUserService currentUserService;
    private final TraderProfileClient traderProfileClient;
    private final ProcurementNotificationService procurementNotificationService;

    @Override
    @Transactional
    public BidResponse submitBid(Long tenderId, SubmitBidRequest request) {

        Long submittedByUserId = currentUserService.getCurrentUserId();
        TraderProfileClientResponse trader = traderProfileClient.getCurrentTraderProfile();

        if (trader.getId() == null) {
            throw new BidSubmissionException(
                    "Authenticated user does not have a linked trader profile."
            );
        }

        log.info("Submitting bid for tenderId={} traderProfileId={} submittedByUserId={}",
                tenderId,
                trader.getId(),
                submittedByUserId
        );

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new TenderNotFoundException(
                        "Tender not found with ID: " + tenderId
                ));

        if (tender.getStatus() != Tender.TenderStatus.PUBLISHED) {
            throw new TenderStateException(
                    "Bids can only be submitted for tenders in PUBLISHED status."
            );
        }

        boolean activeBidExists =
                bidRepository.existsActiveBidForTrader(
                        tenderId,
                        trader.getId(),
                        Bid.BidStatus.WITHDRAWN
                );

        if (activeBidExists) {
            throw new DuplicateBidException(
                    "Trader already has an active bid for this tender."
            );
        }

        long existingBidCount = bidRepository.countByTenderId(tenderId);
        String alias = generateBidderAlias(existingBidCount);


        Bid bid = Bid.builder()
                .bidReference(generateBidReference())
                .tenderId(tenderId)
                .traderProfileId(trader.getId())
                .submittedByUserId(submittedByUserId)
                .bidderAlias(alias)
                .technicalProposal(request.getTechnicalProposal().trim())
                .priceAmount(request.getPriceAmount())
                .status(Bid.BidStatus.SUBMITTED)
                .build();

        Bid savedBid = bidRepository.save(bid);

        ComplianceGatekeeperService.ComplianceDecision decision =
                gatekeeperService.evaluate(request);

        BidComplianceResult compliance = BidComplianceResult.builder()
                .bidId(savedBid.getId())
                .csdValid(request.isCsdValid())
                .taxClearanceValid(request.isTaxClearanceValid())
                .bbbeeValid(request.isBbbeeValid())
                .requiredDocumentsUploaded(request.isRequiredDocumentsUploaded())
                .passed(decision.passed())
                .failureReason(decision.failureReason())
                .build();

        BidComplianceResult savedCompliance = complianceRepository.save(compliance);

        if (decision.passed()) {
            savedBid.setStatus(Bid.BidStatus.COMPLIANT);
        } else {
            savedBid.setStatus(Bid.BidStatus.COMPLIANCE_FAILED);
        }

        Bid finalBid = bidRepository.save(savedBid);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.BID_SUBMITTED,
                tenderId,
                finalBid.getId(),
                submittedByUserId,
                "Bid submitted",
                "Bid reference: " + finalBid.getBidReference() + ", bidderAlias: " + finalBid.getBidderAlias()
        );

        if (decision.passed()) {
            auditService.recordSuccess(
                    ProcurementAuditEvent.AuditEventType.BID_COMPLIANCE_PASSED,
                    tenderId,
                    finalBid.getId(),
                    submittedByUserId,
                    "Bid compliance passed",
                    "All baseline compliance checks passed"
            );
        } else {
            auditService.recordFailure(
                    ProcurementAuditEvent.AuditEventType.BID_COMPLIANCE_FAILED,
                    tenderId,
                    finalBid.getId(),
                    submittedByUserId,
                    "Bid compliance failed",
                    decision.failureReason()
            );
        }

        procurementNotificationService.queueBidReceived(
                finalBid,
                tender
        );

        if (decision.passed()) {
            procurementNotificationService.queueCompliancePassed(
                    finalBid,
                    tender
            );
        } else {
            procurementNotificationService.queueComplianceFailed(
                    finalBid,
                    tender,
                    decision.failureReason()
            );
        }
        return BidResponse.from(
                finalBid,
                BidComplianceResponse.from(savedCompliance)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnonymizedBidResponse> getAnonymizedBidsForTender(Long tenderId) {
        if (!tenderRepository.existsById(tenderId)) {
            throw new TenderNotFoundException("Tender not found with ID: " + tenderId);
        }

        log.info("Fetching active anonymized bids for tenderId={}", tenderId);

        return bidRepository.findByTenderId(tenderId)
                .stream()
                .filter(bid ->
                        bid.getStatus() != Bid.BidStatus.WITHDRAWN
                )
                .map(bid -> {
                    BidComplianceResponse compliance =
                            complianceRepository
                                    .findByBidId(bid.getId())
                                    .map(BidComplianceResponse::from)
                                    .orElse(null);

                    return AnonymizedBidResponse.from(
                            bid,
                            compliance
                    );
                })
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<BidResponse> getCurrentTraderBids() {

        TraderProfileClientResponse trader = traderProfileClient.getCurrentTraderProfile();

        if (trader == null || trader.getId() == null) {
            throw new BidSubmissionException("Authenticated user does not have a linked trader profile.");
        }

        log.info(
                "Fetching bids for authenticated traderProfileId={}",
                trader.getId()
        );

        return bidRepository
                .findByTraderProfileIdOrderBySubmittedAtDesc(
                        trader.getId()
                )
                .stream()
                .map(bid -> {
                    BidComplianceResponse compliance =
                            complianceRepository
                                    .findByBidId(bid.getId())
                                    .map(BidComplianceResponse::from)
                                    .orElse(null);

                    return BidResponse.from(
                            bid,
                            compliance
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public BidResponse withdrawCurrentTraderBid(Long bidId) {
        Long currentUserId = currentUserService.getCurrentUserId();

        TraderProfileClientResponse trader = traderProfileClient.getCurrentTraderProfile();

        if (trader == null || trader.getId() == null) {
            throw new BidSubmissionException(
                    "Authenticated user does not have a linked trader profile."
            );
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException(
                        "Bid not found with ID: " + bidId
                ));

        if (!bid.getTraderProfileId().equals(trader.getId())) {
            log.warn(
                    "Bid withdrawal denied for bidId={} authenticatedTraderProfileId={}",
                    bidId,
                    trader.getId()
            );

            throw new BidNotFoundException(
                    "Bid not found or does not belong to the authenticated trader."
            );
        }

        Tender tender = tenderRepository.findById(
                        bid.getTenderId()
                )
                .orElseThrow(() -> new TenderNotFoundException(
                        "Tender not found with ID: "
                                + bid.getTenderId()
                ));

        if (tender.getStatus() != Tender.TenderStatus.PUBLISHED) {
            throw new TenderStateException(
                    "A bid can only be withdrawn while the tender is open for bidding."
            );
        }

        if (bid.getStatus() == Bid.BidStatus.WITHDRAWN) {
            throw new BidStateException(
                    "Bid has already been withdrawn."
            );
        }

        if (!isWithdrawableStatus(bid.getStatus())) {
            throw new BidStateException(
                    "A bid in status "
                            + bid.getStatus()
                            + " cannot be withdrawn."
            );
        }

        bid.setStatus(Bid.BidStatus.WITHDRAWN);

        Bid withdrawnBid =
                bidRepository.save(bid);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.BID_WITHDRAWN,
                withdrawnBid.getTenderId(),
                withdrawnBid.getId(),
                currentUserId,
                "Bid withdrawn",
                "Bid reference: "
                        + withdrawnBid.getBidReference()
                        + ", bidder alias: "
                        + withdrawnBid.getBidderAlias()
        );

        BidComplianceResponse compliance =
                complianceRepository.findByBidId(
                                withdrawnBid.getId()
                        )
                        .map(BidComplianceResponse::from)
                        .orElse(null);

        log.info(
                "Bid withdrawn successfully: bidId={} tenderId={} traderProfileId={}",
                withdrawnBid.getId(),
                withdrawnBid.getTenderId(),
                trader.getId()
        );

        return BidResponse.from(
                withdrawnBid,
                compliance
        );
    }

    private boolean isWithdrawableStatus(
            Bid.BidStatus status
    ) {
        return status == Bid.BidStatus.SUBMITTED
                || status == Bid.BidStatus.COMPLIANT
                || status == Bid.BidStatus.COMPLIANCE_FAILED;
    }


    private String generateBidReference() {
        return "KB-BID-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }

    private String generateBidderAlias(long existingBidCount) {
        char aliasLetter = (char) ('A' + existingBidCount);
        return "Bidder " + aliasLetter;
    }

}
