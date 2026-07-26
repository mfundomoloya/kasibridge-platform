package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.ProcurementAuditResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.repository.ProcurementAuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementAuditServiceImpl implements ProcurementAuditService {

    private final ProcurementAuditEventRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(ProcurementAuditEvent.AuditEventType eventType, Long tenderId, Long bidId, Long actorUserId, String message, String details) {
        record(
                eventType,
                tenderId,
                bidId,
                actorUserId,
                ProcurementAuditEvent.AuditResult.SUCCESS,
                message,
                details
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(ProcurementAuditEvent.AuditEventType eventType, Long tenderId, Long bidId, Long actorUserId, String message, String details) {
        record(
                eventType,
                tenderId,
                bidId,
                actorUserId,
                ProcurementAuditEvent.AuditResult.FAILED,
                message,
                details
        );
    }

    private void record(
            ProcurementAuditEvent.AuditEventType eventType,
            Long tenderId,
            Long bidId,
            Long actorUserId,
            ProcurementAuditEvent.AuditResult result,
            String message,
            String details
    ) {
        try {
            ProcurementAuditEvent event = ProcurementAuditEvent.builder()
                    .eventType(eventType)
                    .tenderId(tenderId)
                    .bidId(bidId)
                    .actorUserId(actorUserId)
                    .result(result)
                    .message(message)
                    .details(details)
                    .build();

            repository.saveAndFlush(event);

            log.info(
                    "Procurement audit event recorded: type={} tenderId={} bidId={} actorUserId={} result={}",
                    eventType,
                    tenderId,
                    bidId,
                    actorUserId,
                    result
            );
        } catch (Exception ex) {
            log.error(
                    "Failed to record procurement audit event: type={} tenderId={} bidId={} actorUserId={} result={}",
                    eventType,
                    tenderId,
                    bidId,
                    actorUserId,
                    result,
                    ex
            );
        }
    }

    private String trimToLength(String value, int maxLength){
        if(value == null){
            return null;
        }

        if(value.length() <= maxLength){
            return value;
        }

        return value.substring(0, maxLength);
    }

    @Override
    public Page<ProcurementAuditResponse> getAuditEvents(Pageable pageable) {
        return repository.findAll(pageable)
                .map(ProcurementAuditResponse::from);
    }

    @Override
    public Page<ProcurementAuditResponse> getAuditEventsByTender(Long tenderId, Pageable pageable) {
        return repository.findByTenderId(tenderId, pageable)
                .map(ProcurementAuditResponse::from);
    }

    @Override
    public Page<ProcurementAuditResponse> getAuditEventsByBid(Long bidId, Pageable pageable) {
        return repository.findByBidId(bidId, pageable)
                .map(ProcurementAuditResponse::from);
    }

    @Override
    public Page<ProcurementAuditResponse> getAuditEventsByActor(Long actorUserId, Pageable pageable) {
        return repository.findByActorUserId(actorUserId, pageable)
                .map(ProcurementAuditResponse::from);
    }
}
