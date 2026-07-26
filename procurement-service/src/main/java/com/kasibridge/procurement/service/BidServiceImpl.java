package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.BidComplianceResponse;
import com.kasibridge.procurement.dto.BidResponse;
import com.kasibridge.procurement.dto.SubmitBidRequest;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.BidComplianceResult;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.exception.DuplicateBidException;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.exception.TenderStateException;
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

    @Override
    @Transactional
    public BidResponse submitBid(Long tenderId, SubmitBidRequest request) {


        log.info("Submitting bid for tenderId={} traderId={}", tenderId, request.getTraderId());

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new TenderNotFoundException(
                        "Tender not found with ID: " + tenderId
                ));

        if (tender.getStatus() != Tender.TenderStatus.PUBLISHED) {
            throw new TenderStateException(
                    "Bids can only be submitted for tenders in PUBLISHED status."
            );
        }

        if (bidRepository.existsByTenderIdAndTraderId(tenderId, request.getTraderId())) {
            throw new DuplicateBidException(
                    "Trader has already submitted a bid for this tender."
            );
        }

        long existingBidCount = bidRepository.countByTenderId(tenderId);
        String alias = generateBidderAlias(existingBidCount);

        Bid bid = Bid.builder()
                .bidReference(generateBidReference())
                .tenderId(tenderId)
                .traderId(request.getTraderId())
                .bidderAlias(alias)
                .technicalProposal(request.getTechnicalProposal())
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
                request.getTraderId(),
                "Bid submitted",
                "Bid reference: " + finalBid.getBidReference() + ", bidderAlias: " + finalBid.getBidderAlias()
        );

        if (decision.passed()) {
            auditService.recordSuccess(
                    ProcurementAuditEvent.AuditEventType.BID_COMPLIANCE_PASSED,
                    tenderId,
                    finalBid.getId(),
                    request.getTraderId(),
                    "Bid compliance passed",
                    "All baseline compliance checks passed"
            );
        } else {
            auditService.recordFailure(
                    ProcurementAuditEvent.AuditEventType.BID_COMPLIANCE_FAILED,
                    tenderId,
                    finalBid.getId(),
                    request.getTraderId(),
                    "Bid compliance failed",
                    decision.failureReason()
            );
        }

        return BidResponse.from(
                finalBid,
                BidComplianceResponse.from(savedCompliance)
        );
    }

    @Override
    public List<BidResponse> getBidsForTender(Long tenderId) {
        if (!tenderRepository.existsById(tenderId)) {
            throw new TenderNotFoundException("Tender not found with ID: " + tenderId);
        }

        return bidRepository.findByTenderId(tenderId)
                .stream()
                .map(bid -> {
                    BidComplianceResponse compliance = complianceRepository.findByBidId(bid.getId())
                            .map(BidComplianceResponse::from)
                            .orElse(null);

                    return BidResponse.from(bid, compliance);
                })
                .toList();
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
