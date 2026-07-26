package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProcurementAuditResponse {

    private Long id;
    private ProcurementAuditEvent.AuditEventType eventType;
    private Long tenderId;
    private Long bidId;
    private Long actorUserId;
    private ProcurementAuditEvent.AuditResult result;
    private String message;
    private String details;
    private LocalDateTime createdAt;

    public static ProcurementAuditResponse from(ProcurementAuditEvent event) {

        return ProcurementAuditResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .tenderId(event.getTenderId())
                .bidId(event.getBidId())
                .actorUserId(event.getActorUserId())
                .result(event.getResult())
                .message(event.getMessage())
                .details(event.getDetails())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
