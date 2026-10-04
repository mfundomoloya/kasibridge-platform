package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.CreateTenderRequest;
import com.kasibridge.procurement.dto.SpecificationIntegrityResponse;
import com.kasibridge.procurement.dto.TenderResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.exception.TenderNotFoundException;
import com.kasibridge.procurement.exception.TenderStateException;
import com.kasibridge.procurement.repository.BidEvaluationScoreRepository;
import com.kasibridge.procurement.repository.TenderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenderServiceImpl implements TenderService {

    private final TenderRepository repository;
    private final SpecificationHashService hashService;
    private final ProcurementAuditService auditService;
    private final BidEvaluationScoreRepository scoreRepository;

    @Override
    @Transactional
    public TenderResponse createTender(CreateTenderRequest request) {
        log.info("Creating tender for buyerOrgId={}", request.getBuyerOrgId());

        Tender tender = Tender.builder()
                .tenderReference(generateTenderReference())
                .title(request.getTitle())
                .description(request.getDescription())
                .evaluationCriteria(request.getEvaluationCriteria())
                .budgetAmount(request.getBudgetAmount())
                .buyerOrgId(request.getBuyerOrgId())
                .createdByUserId(request.getCreatedByUserId())
                .status(Tender.TenderStatus.DRAFT)
                .build();

        Tender saved = repository.save(tender);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.TENDER_CREATED,
                saved.getId(),
                null,
                saved.getCreatedByUserId(),
                "Tender created",
                "Tender reference: " + saved.getTenderReference()
        );

        log.info("Tender created with id={} reference={}", saved.getId(), saved.getTenderReference());

        return TenderResponse.from(saved);
    }

    @Override
    public Page<TenderResponse> getTenders(Pageable pageable) {
        return repository.findAll(pageable)
                .map(TenderResponse::from);
    }

    @Override
    public TenderResponse getTenderById(Long id) {
        return TenderResponse.from(findTender(id));
    }

    @Override
    public TenderResponse getTenderByReference(String tenderReference) {
        Tender tender = repository.findByTenderReference(tenderReference)
                .orElseThrow(() -> new TenderNotFoundException(
                        "Tender not found with reference: " + tenderReference
                ));

        return TenderResponse.from(tender);
    }

    @Override
    @Transactional
    public TenderResponse publishTender(Long id) {
        Tender tender = findTender(id);

        if (tender.getStatus() != Tender.TenderStatus.DRAFT) {
            throw new TenderStateException(
                    "Only tenders in DRAFT status can be published."
            );
        }

        String hash = hashService.generateHash(tender);

        LocalDateTime now = LocalDateTime.now();
        tender.setSpecificationHash(hash);
        tender.setPublishedAt(now);
        tender.setUpdatedAt(now);
        tender.setStatus(Tender.TenderStatus.PUBLISHED);

        Tender saved = repository.save(tender);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.TENDER_PUBLISHED,
                saved.getId(),
                null,
                saved.getCreatedByUserId(),
                "Tender published with specification hash",
                "Specification hash: " + saved.getSpecificationHash()
        );

        log.info("Tender id={} published with specificationHash={}", saved.getId(), hash);

        return TenderResponse.from(saved);
    }

    @Override
    @Transactional
    public TenderResponse closeBidding(Long id) {
        Tender tender = findTender(id);

        if (tender.getStatus() != Tender.TenderStatus.PUBLISHED) {
            throw new TenderStateException(
                    "Only PUBLISHED tenders can have bidding closed."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        tender.setStatus(Tender.TenderStatus.BIDDING_CLOSED);
        tender.setClosedAt(now);
        tender.setUpdatedAt(now);

        Tender saved = repository.save(tender);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.TENDER_BIDDING_CLOSED,
                saved.getId(),
                null,
                saved.getCreatedByUserId(),
                "Tender bidding closed",
                "Tender reference: " + saved.getTenderReference()
        );

        return TenderResponse.from(saved);
    }

    @Override
    @Transactional
    public TenderResponse startEvaluation(Long id) {
        Tender tender = findTender(id);

        if (tender.getStatus() != Tender.TenderStatus.BIDDING_CLOSED) {
            throw new TenderStateException(
                    "Only BIDDING_CLOSED tenders can move to EVALUATION."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        tender.setStatus(Tender.TenderStatus.EVALUATION);
        tender.setClosedAt(now);
        tender.setUpdatedAt(now);

        Tender saved = repository.save(tender);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.TENDER_EVALUATION_STARTED,
                saved.getId(),
                null,
                saved.getCreatedByUserId(),
                "Tender moved to evaluation",
                "Tender reference: " + saved.getTenderReference()
        );

        return TenderResponse.from(saved);
    }

    @Override
    public TenderResponse startAdjudication(Long id) {
        Tender tender = findTender(id);

        if (tender.getStatus() != Tender.TenderStatus.EVALUATION) {
            throw new TenderStateException(
                    "Only EVALUATION tenders can move to ADJUDICATION."
            );
        }

        boolean hasEvaluationScores = scoreRepository.existsByTenderId(id);

        if(!hasEvaluationScores){
            throw new TenderStateException("Tender cannot move to ADJUDICATION until at least one bid has been evaluated.");
        }

        LocalDateTime now = LocalDateTime.now();

        tender.setStatus(Tender.TenderStatus.ADJUDICATION);
        tender.setClosedAt(now);
        tender.setUpdatedAt(now);

        Tender saved = repository.save(tender);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.TENDER_EVALUATION_STARTED,
                saved.getId(),
                null,
                saved.getCreatedByUserId(),
                "Tender moved to adjudication",
                "Tender reference: " + saved.getTenderReference()
        );

        return TenderResponse.from(saved);
    }

    @Override
    public SpecificationIntegrityResponse verifySpecificationIntegrity(Long id) {
        Tender tender = findTender(id);

        if(tender.getSpecificationHash() == null || tender.getSpecificationHash().isBlank()){
            throw new TenderStateException("Tender has not been published and does not have a specification hash.");
        }

        String currentHash = hashService.generateHash(tender);
        boolean integrityValid = tender.getSpecificationHash().equals(currentHash);

        String message = integrityValid
                ? "Tender specification has been verified."
                : "Tender specification integrity check failed. Current specification does not match published hash.";

        auditService.recordSuccess(
                integrityValid
                        ?
                ProcurementAuditEvent.AuditEventType.TENDER_SPECIFICATION_VERIFIED
                        :
                ProcurementAuditEvent.AuditEventType.TENDER_SPECIFICATION_TAMPER_DETECTED,
                tender.getId(),
                null,
                null,
                message,
                "Stored hash=" + tender.getSpecificationHash() + ", current hash=" + currentHash
        );

        return SpecificationIntegrityResponse.builder()
                .tenderId(tender.getId())
                .tenderReference(tender.getTenderReference())
                .storedHash(tender.getSpecificationHash())
                .currentHash(currentHash)
                .integrityValid(integrityValid)
                .message(message)
                .build();
    }

    private Tender findTender(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TenderNotFoundException(
                        "Tender not found with ID: " + id
                ));
    }

    private String generateTenderReference() {
        return "KB-TENDER-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }

}
