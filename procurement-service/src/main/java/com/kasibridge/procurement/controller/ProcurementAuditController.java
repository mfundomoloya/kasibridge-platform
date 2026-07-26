package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.ProcurementAuditResponse;
import com.kasibridge.procurement.service.ProcurementAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProcurementAuditController {
    private final ProcurementAuditService service;

    @GetMapping("/api/v1/procurement/audit-events")
    public ResponseEntity<Page<ProcurementAuditResponse>> getAuditEvents(@PageableDefault(sort = "createdAt",
            direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("GET /api/v1/procurement/audit-events - fetching audit events");
        return ResponseEntity.ok(service.getAuditEvents(pageable));
    }

    @GetMapping("/api/v1/tenders/{tenderId}/audit-events")
    public ResponseEntity<Page<ProcurementAuditResponse>> getAuditEventsByTender(@PathVariable("tenderId") Long tenderId,
                                                                                 @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        log.info("GET /api/v1/tenders/{}/audit-events - fetching audit events", tenderId);
        return ResponseEntity.ok(service.getAuditEventsByTender(tenderId, pageable));
    }
    @GetMapping("/api/v1/bids/{bidId}/audit-events")
    public ResponseEntity<Page<ProcurementAuditResponse>> getAuditEventsByBid(@PathVariable("bidId") Long bidId,
                                                                              @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        log.info("GET /api/v1/bids/{}/audit-events - fetching audit events", bidId);
        return ResponseEntity.ok(service.getAuditEventsByBid(bidId, pageable));
    }

    @GetMapping("/api/v1/procurement/audit-events/actor/{actorUserId}")
    public ResponseEntity<Page<ProcurementAuditResponse>> getAuditEventsByActor(
            @PathVariable("actorUserId") Long actorUserId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        log.info("GET /api/v1/procurement/audit-events/actor/{} - fetching audit events", actorUserId);
        return ResponseEntity.ok(service.getAuditEventsByActor(actorUserId, pageable));
    }
}
