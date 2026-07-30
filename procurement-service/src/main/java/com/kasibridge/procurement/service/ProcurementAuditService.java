package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AuditChainVerificationResponse;
import com.kasibridge.procurement.dto.ProcurementAuditResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProcurementAuditService {

    void recordSuccess(
            ProcurementAuditEvent.AuditEventType eventType,
            Long tenderId,
            Long bidId,
            Long actorUserId,
            String message,
            String details
    );

    void recordFailure(
            ProcurementAuditEvent.AuditEventType eventType,
            Long tenderId,
            Long bidId,
            Long actorUserId,
            String message,
            String details
    );

    Page<ProcurementAuditResponse> getAuditEvents(Pageable pageable);
    Page<ProcurementAuditResponse> getAuditEventsByTender(Long tenderId, Pageable pageable);
    Page<ProcurementAuditResponse> getAuditEventsByBid(Long bidId, Pageable pageable);
    Page<ProcurementAuditResponse> getAuditEventsByActor(Long actorUserId, Pageable pageable);
    AuditChainVerificationResponse verifyAuditChain();
}
