package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.CloseTicketRequest;
import com.kasibridge.procurement.dto.CreateSupportTicketRequest;
import com.kasibridge.procurement.dto.RespondToTicketRequest;
import com.kasibridge.procurement.dto.SupportTicketResponse;
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
}
