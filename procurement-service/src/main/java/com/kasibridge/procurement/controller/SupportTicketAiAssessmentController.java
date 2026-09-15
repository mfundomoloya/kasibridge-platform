package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.ApproveTicketAiAssessmentRequest;
import com.kasibridge.procurement.dto.EscalateTicketAiAssessmentRequest;
import com.kasibridge.procurement.dto.RejectTicketAiAssessmentRequest;
import com.kasibridge.procurement.dto.SupportTicketAiAssessmentResponse;
import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import com.kasibridge.procurement.service.SupportTicketAiAssessmentService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class SupportTicketAiAssessmentController {

    private final SupportTicketAiAssessmentService service;

    @PostMapping("/tickets/{ticketId}/ai-assessment")
    public ResponseEntity<SupportTicketAiAssessmentResponse> generateAssessment(@PathVariable("ticketId") Long ticketId) {

        log.info("POST /api/v1/tickets/{}/ai-assessment - generating assessment", ticketId);

        SupportTicketAiAssessmentResponse response = service.generateAssessment(ticketId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/tickets/{ticketId}/ai-assessment")
    public ResponseEntity<SupportTicketAiAssessmentResponse> getAssessmentByTicketId(@PathVariable("ticketId") Long ticketId
    ) {

        log.info("GET /api/v1/tickets/{}/ai-assessment", ticketId);

        return ResponseEntity.ok(
                service.getAssessmentByTicketId(ticketId)
        );
    }

    @GetMapping("/ticket-ai-assessments/{assessmentId}")
    public ResponseEntity<SupportTicketAiAssessmentResponse> getAssessmentById(@PathVariable("assessmentId") Long assessmentId
    ) {
        log.info("GET /api/v1/ticket-ai-assessments/{}", assessmentId);

        return ResponseEntity.ok(service.getAssessmentById(assessmentId));
    }

    @GetMapping("/ticket-ai-assessments")
    public ResponseEntity<Page<SupportTicketAiAssessmentResponse>> getAssessments(@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                                                                  Pageable pageable) {
        log.info("GET /api/v1/ticket-ai-assessments");

        return ResponseEntity.ok(
                service.getAssessments(pageable)
        );
    }

    @GetMapping("/ticket-ai-assessments/status/{status}")
    public ResponseEntity<Page<SupportTicketAiAssessmentResponse>> getAssessmentsByStatus(@PathVariable("status")
                                                                                              SupportTicketAiAssessment.AiAssessmentStatus status,
                                                                                          @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
    Pageable pageable
        ) {
        log.info(
                "GET /api/v1/ticket-ai-assessments/status/{}",
                status
        );

        return ResponseEntity.ok(
                service.getAssessmentsByStatus(
                        status,
                        pageable
                )
        );
    }

    @GetMapping("/ticket-ai-assessments/priority/{priority}")
    public ResponseEntity<Page<SupportTicketAiAssessmentResponse>> getAssessmentsByPriority(@PathVariable("priority")
                                                                                                SupportTicketAiAssessment.TicketPriority priority,
                                                                                            @PageableDefault(sort = "createdAt",
                                                                                                    direction = Sort.Direction.DESC)
                                                                                            Pageable pageable
                ) {
    log.info("GET /api/v1/ticket-ai-assessments/priority/{}", priority);

        return ResponseEntity.ok(
                service.getAssessmentsByPriority(
                        priority,
                        pageable
                )
        );
    }

    @GetMapping("/ticket-ai-assessments/resolution-mode/{resolutionMode}")
    public ResponseEntity<Page<SupportTicketAiAssessmentResponse>> getAssessmentsByResolutionMode(@PathVariable("resolutionMode")
                                                                                                  SupportTicketAiAssessment.AiResolutionMode resolutionMode,
                                                                                                  @PageableDefault(sort = "createdAt",
                                                                                                          direction = Sort.Direction.DESC)
                                                                                                  Pageable pageable) {
        log.info(
                "GET /api/v1/ticket-ai-assessments/resolution-mode/{}",
                resolutionMode
        );

        return ResponseEntity.ok(
                service.getAssessmentsByResolutionMode(
                        resolutionMode,
                        pageable
                        ));
    }

    @PatchMapping("/ticket-ai-assessments/{assessmentId}/approve")
    public ResponseEntity<SupportTicketAiAssessmentResponse> approveAssessment(@PathVariable("assessmentId") Long assessmentId,
                                                                               @Valid @RequestBody ApproveTicketAiAssessmentRequest request
    ) {
        log.info("PATCH /api/v1/ticket-ai-assessments/{}/approve", assessmentId);

        return ResponseEntity.ok(
                service.approveAssessment(
                        assessmentId,
                        request
                )
        );
    }

    @PatchMapping("/ticket-ai-assessments/{assessmentId}/reject")
    public ResponseEntity<SupportTicketAiAssessmentResponse> rejectAssessment(@PathVariable("assessmentId") Long assessmentId,
                                                                              @Valid @RequestBody RejectTicketAiAssessmentRequest request) {

    log.info("PATCH /api/v1/ticket-ai-assessments/{}/reject", assessmentId
        );

        return ResponseEntity.ok(
                service.rejectAssessment(
                        assessmentId,
                        request
                )
        );
    }

    @PatchMapping("/ticket-ai-assessments/{assessmentId}/escalate")
    public ResponseEntity<SupportTicketAiAssessmentResponse> escalateAssessment(@PathVariable("assessmentId")
                                                                                    Long assessmentId,
                                                                                @Valid @RequestBody EscalateTicketAiAssessmentRequest request
    ) {
        log.info("PATCH /api/v1/ticket-ai-assessments/{}/escalate", assessmentId);
       return ResponseEntity.ok(
                service.escalateAssessment(
                        assessmentId,
                        request
                )
        );
    }
}
