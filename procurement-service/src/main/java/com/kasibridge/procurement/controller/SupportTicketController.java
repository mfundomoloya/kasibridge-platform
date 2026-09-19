package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.*;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SupportTicketController {
    private final SupportTicketService service;

    @PostMapping("/api/v1/tenders/{tenderId}/tickets")
    public ResponseEntity<SupportTicketResponse> createTicket(@PathVariable("tenderId") Long tenderId,
                                                              @Valid @RequestBody CreateSupportTicketRequest request) {

    return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTicket(tenderId, request));
        }

    @GetMapping("/api/v1/tenders/{tenderId}/tickets")
    public ResponseEntity<Page<SupportTicketResponse>> getTicketsForTender(@PathVariable("tenderId") Long tenderId,
                                                                           @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                                                           Pageable pageable) {
        return ResponseEntity.ok(service.getTicketsForTender(tenderId, pageable));

    }

    @GetMapping("/api/v1/tickets/my")
    public ResponseEntity<Page<SupportTicketResponse>> getMyTickets(@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                                                    Pageable pageable) {
        return ResponseEntity.ok(service.getMyTickets(pageable));
    }

    @GetMapping("/api/v1/tickets/{ticketId}")
    public ResponseEntity<SupportTicketResponse> getTicketById(@PathVariable("ticketId") Long ticketId) {
        return ResponseEntity.ok(service.getTicketById(ticketId));
    }

    @PatchMapping("/api/v1/tickets/{ticketId}/respond")
    public ResponseEntity<SupportTicketResponse> respondToTicket(@PathVariable("ticketId") Long ticketId,
                                                                 @Valid @RequestBody RespondToTicketRequest request) {
        return ResponseEntity.ok(service.respondToTicket(ticketId, request));
    }

    @PatchMapping("/api/v1/tickets/{ticketId}/close")
    public ResponseEntity<SupportTicketResponse> closeTicket(@PathVariable("ticketId") Long ticketId,
                                                             @Valid @RequestBody CloseTicketRequest request) {
        return ResponseEntity.ok(service.closeTicket(ticketId, request));
    }

    @GetMapping("/api/v1/tenders/{tenderId}/clarifications")
    public ResponseEntity<Page<SupportTicketResponse>> getPublicClarifications(@PathVariable("tenderId") Long tenderId,
                                                                               @PageableDefault(sort = "respondedAt", direction = Sort.Direction.DESC)
                                                                               Pageable pageable) {
        return ResponseEntity.ok(service.getPublicClarifications(tenderId, pageable));
    }

    @PatchMapping("/api/v1/tickets/{ticketId}/start-review")
    public ResponseEntity<SupportTicketResponse> startReview(@PathVariable("ticketId") Long ticketId) {

        log.info("PATCH /api/v1/tickets/{}/start-review", ticketId);

        return ResponseEntity.ok(service.startReview(ticketId));
    }

    @PatchMapping("/api/v1/tickets/{ticketId}/reject")
    public ResponseEntity<SupportTicketResponse> rejectTicket(@PathVariable("ticketId") Long ticketId,
                                                              @Valid @RequestBody RejectSupportTicketRequest request
    ) {
        log.info("PATCH /api/v1/tickets/{}/reject", ticketId);

        return ResponseEntity.ok(service.rejectTicket(ticketId, request));
    }

    @PatchMapping("/api/v1/tickets/{ticketId}/assign")
    public ResponseEntity<SupportTicketResponse> assignTicket(@PathVariable("ticketId") Long ticketId,
                                                              @Valid @RequestBody AssignSupportTicketRequest request
    ) {
        log.info("PATCH /api/v1/tickets/{}/assign assignedToUserId={}", ticketId, request.getAssignedToUserId());

        return ResponseEntity.ok(service.assignTicket(ticketId, request)
        );
    }

    @GetMapping("/api/v1/tickets/assigned-to-me")
    public ResponseEntity<Page<SupportTicketResponse>> getMyAssignedTickets(@PageableDefault(
            sort = "assignedAt",
            direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.getMyAssignedTickets(pageable));
    }

    @GetMapping("/api/v1/tickets/assigned-to-me/status/{status}")
    public ResponseEntity<Page<SupportTicketResponse>> getMyAssignedTicketsByStatus(@PathVariable("status")
                                                                                        SupportTicket.TicketStatus status,
                                                                                    @PageableDefault(
                                                                                            sort = "assignedAt",
                                                                                            direction = Sort.Direction.DESC)
                                                                                    Pageable pageable) {
        return ResponseEntity.ok(
                service.getMyAssignedTicketsByStatus(
                        status,
                        pageable
                )
        );
    }

    @GetMapping("/api/v1/tickets/unassigned")
    public ResponseEntity<Page<SupportTicketResponse>> getUnassignedTickets(@PageableDefault(sort = "createdAt",
            direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.getUnassignedTickets(pageable));
    }

    @PatchMapping("/api/v1/tickets/{ticketId}/return-to-queue")
    public ResponseEntity<SupportTicketResponse> returnTicketToQueue(@PathVariable Long ticketId,
                                                                     @Valid
                                                                     @RequestBody ReturnSupportTicketToQueueRequest request) {

        return ResponseEntity.ok(service.returnTicketToQueue(ticketId, request)
        );
    }
}
