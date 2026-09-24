package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.*;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import com.kasibridge.procurement.exception.SupportTicketException;
import com.kasibridge.procurement.exception.TicketAiAssessmentException;
import com.kasibridge.procurement.exception.TicketAiAssessmentStateException;
import com.kasibridge.procurement.repository.SupportTicketAiAssessmentRepository;
import com.kasibridge.procurement.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketAiAssessmentServiceImpl implements SupportTicketAiAssessmentService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketAiAssessmentRepository assessmentRepository;
    private final TicketAiTriageService triageService;
    private final CurrentUserService currentUserService;
    private final ProcurementAuditService auditService;
    private final SupportTicketService supportTicketService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SupportTicketAiAssessmentResponse generateAssessment(Long ticketId) {
        SupportTicket ticket = findTicket(ticketId);

        if (assessmentRepository.existsByTicketId(ticketId)) {
            throw new TicketAiAssessmentStateException("An AI assessment already exists for ticket ID: " + ticketId);
        }

        TicketAiTriageResult triageResult = triageService.assessTicket(ticket);

        validateTriageResult(triageResult);

        SupportTicketAiAssessment assessment = SupportTicketAiAssessment.builder()
                .assessmentReference(generateAssessmentReference()
                )
                .ticketId(ticket.getId())
                .detectedCategory(triageResult.getDetectedCategory()
                )
                .priority(triageResult.getPriority()
                )
                .resolutionMode(triageResult.getResolutionMode()
                )
                .assessmentStatus(
                        SupportTicketAiAssessment.AiAssessmentStatus.GENERATED
                )
                .summary(triageResult.getSummary().trim()
                )
                .suggestedResponse(trimToNull(triageResult.getSuggestedResponse())
                )
                .escalationReason(
                        trimToNull(
                                triageResult.getEscalationReason()              )
                )
                .confidenceScore(triageResult.getConfidenceScore()
                )
                .modelVersion(triageResult.getModelVersion().trim()
                )
                .build();

        SupportTicketAiAssessment saved = assessmentRepository.saveAndFlush(assessment);

        Long actorUserId = currentUserService.getCurrentUserId();

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType
                        .SUPPORT_TICKET_AI_ASSESSMENT_CREATED,
                ticket.getTenderId(),
                ticket.getBidId(),
                actorUserId,
                "Support ticket AI assessment created",
                "Assessment reference="
                        + saved.getAssessmentReference()
                        + ", ticket reference="
                        + ticket.getTicketReference()
                        + ", category="
                        + saved.getDetectedCategory()
                        + ", priority="
                        + saved.getPriority()
                        + ", resolutionMode="
                        + saved.getResolutionMode()
        );

        log.info(
                "Ticket AI assessment created: assessmentId={} assessmentReference={} ticketId={} resolutionMode={}",
                saved.getId(),
                saved.getAssessmentReference(),
                saved.getTicketId(),
                saved.getResolutionMode()
        );

        return SupportTicketAiAssessmentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketAiAssessmentResponse getAssessmentByTicketId(Long ticketId) {
        SupportTicketAiAssessment assessment = assessmentRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new TicketAiAssessmentException(
                "AI assessment not found for ticket ID: " + ticketId));

        return SupportTicketAiAssessmentResponse.from(assessment);
    }

    @Override
    public SupportTicketAiAssessmentResponse getAssessmentById(Long assessmentId) {

        SupportTicketAiAssessment assessment = findAssessment(assessmentId);

        return SupportTicketAiAssessmentResponse.from(assessment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketAiAssessmentResponse> getAssessments(Pageable pageable) {
        return assessmentRepository.findAll(pageable)
                .map(SupportTicketAiAssessmentResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketAiAssessmentResponse> getAssessmentsByStatus(SupportTicketAiAssessment.AiAssessmentStatus status, Pageable pageable) {
        return assessmentRepository.findByAssessmentStatus(status, pageable)
                .map(SupportTicketAiAssessmentResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketAiAssessmentResponse> getAssessmentsByPriority(SupportTicketAiAssessment.TicketPriority priority, Pageable pageable) {
        return assessmentRepository.findByPriority(priority, pageable)
                .map(SupportTicketAiAssessmentResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketAiAssessmentResponse> getAssessmentsByResolutionMode(SupportTicketAiAssessment.AiResolutionMode resolutionMode, Pageable pageable) {
        return assessmentRepository.findByResolutionMode(resolutionMode, pageable)
                .map(SupportTicketAiAssessmentResponse::from);
    }

    @Override
    @Transactional
    public SupportTicketAiAssessmentResponse approveAssessment(Long assessmentId, ApproveTicketAiAssessmentRequest request) {
        Long actorUserId = currentUserService.getCurrentUserId();

        SupportTicketAiAssessment assessment = findAssessment(assessmentId);

        assertAssessmentIsGenerated(assessment);

        if (assessment.getResolutionMode()
                == SupportTicketAiAssessment
                .AiResolutionMode.HUMAN_ONLY) {

            throw new TicketAiAssessmentStateException("HUMAN_ONLY assessments cannot be approved as AI responses.");
        }

        String approvedResponse = resolveApprovedResponse(assessment, request);

        boolean edited = isEditedResponse(assessment, request);

        LocalDateTime now = LocalDateTime.now();

        assessment.setApprovedResponse(approvedResponse);
        assessment.setApprovedByUserId(actorUserId);
        assessment.setApprovedAt(now);

        assessment.setRejectedByUserId(null);
        assessment.setRejectedAt(null);
        assessment.setRejectionReason(null);
        assessment.setAssessmentStatus(
                edited
                        ? SupportTicketAiAssessment.AiAssessmentStatus.EDITED_AND_APPROVED
                        : SupportTicketAiAssessment.AiAssessmentStatus.APPROVED
);

        SupportTicketAiAssessment saved = assessmentRepository.saveAndFlush(assessment);

        SupportTicket ticket = findTicket(saved.getTicketId());

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_AI_ASSESSMENT_APPROVED,
                ticket.getTenderId(),
                ticket.getBidId(),
                actorUserId,
                "Support ticket AI assessment approved",
                "Assessment reference="
                        + saved.getAssessmentReference()
                        + ", ticket reference="
                        + ticket.getTicketReference()
                        + ", status="
                        + saved.getAssessmentStatus()
        );

        log.info("Ticket AI assessment approved: assessmentId={} status={} approvedByUserId={}",
                saved.getId(),
                saved.getAssessmentStatus(),
                actorUserId
        );

        return SupportTicketAiAssessmentResponse.from(saved);
    }

    @Override
    @Transactional
    public SupportTicketAiAssessmentResponse rejectAssessment(Long assessmentId, RejectTicketAiAssessmentRequest request) {

            Long actorUserId = currentUserService.getCurrentUserId();

            SupportTicketAiAssessment assessment = findAssessment(assessmentId);

            assertAssessmentIsGenerated(assessment);

            LocalDateTime now = LocalDateTime.now();

            assessment.setAssessmentStatus(SupportTicketAiAssessment.AiAssessmentStatus.REJECTED);

            assessment.setRejectedByUserId(actorUserId);
            assessment.setRejectedAt(now);
            assessment.setRejectionReason(request.getRejectionReason().trim());

            assessment.setApprovedResponse(null);
            assessment.setApprovedByUserId(null);
            assessment.setApprovedAt(null);

            SupportTicketAiAssessment saved = assessmentRepository.saveAndFlush(assessment);

            SupportTicket ticket = findTicket(saved.getTicketId());

            auditService.recordSuccess(
                    ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_AI_ASSESSMENT_REJECTED,
                    ticket.getTenderId(),
                    ticket.getBidId(),
                    actorUserId,
                    "Support ticket AI assessment rejected",
                    "Assessment reference="
                            + saved.getAssessmentReference()
                            + ", ticket reference="
                            + ticket.getTicketReference()
                            + ", reason="
                            + saved.getRejectionReason()
            );

            log.info("Ticket AI assessment rejected: assessmentId={} rejectedByUserId={}", saved.getId(), actorUserId);

            return SupportTicketAiAssessmentResponse.from(saved);
    }

    @Override
    @Transactional
    public SupportTicketAiAssessmentResponse escalateAssessment(Long assessmentId, EscalateTicketAiAssessmentRequest request) {
        Long actorUserId = currentUserService.getCurrentUserId();

        SupportTicketAiAssessment assessment = findAssessment(assessmentId);

        assertAssessmentIsGenerated(assessment);

        assessment.setAssessmentStatus(SupportTicketAiAssessment.AiAssessmentStatus.ESCALATED);

        assessment.setEscalationReason(request.getEscalationReason().trim());

        assessment.setApprovedResponse(null);
        assessment.setApprovedByUserId(null);
        assessment.setApprovedAt(null);

        assessment.setRejectedByUserId(null);
        assessment.setRejectedAt(null);
        assessment.setRejectionReason(null);

        SupportTicketAiAssessment saved = assessmentRepository.saveAndFlush(assessment);

        SupportTicket ticket = findTicket(saved.getTicketId());

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_AI_ASSESSMENT_ESCALATED,
                ticket.getTenderId(),
                ticket.getBidId(),
                actorUserId,
                "Support ticket AI assessment escalated",
                "Assessment reference="
                        + saved.getAssessmentReference()
                        + ", ticket reference="
                        + ticket.getTicketReference()
                        + ", reason="
                        + saved.getEscalationReason()
        );

        log.info("Ticket AI assessment escalated: assessmentId={} escalatedByUserId={}", saved.getId(),
                actorUserId);

        return SupportTicketAiAssessmentResponse.from(saved);

    }

    @Override
    public SupportTicketAiAssessmentResponse publishApprovedResponse(Long assessmentId) {

        Long actorUserId = currentUserService.getCurrentUserId();

        SupportTicketAiAssessment assessment = findAssessment(assessmentId);

        assertAssessmentCanBePublished(assessment);

        SupportTicket ticket = findTicket(assessment.getTicketId());

        assertTicketCanReceivePublishedResponse(ticket);

        RespondToTicketRequest responseRequest = new RespondToTicketRequest();

        responseRequest.setResponse(assessment.getApprovedResponse().trim());

        responseRequest.setPublicClarification(ticket.getTicketType() == SupportTicket.TicketType.CLARIFICATION_REQUEST);

        supportTicketService.respondToTicket(ticket.getId(), responseRequest);

        LocalDateTime now = LocalDateTime.now();

        assessment.setAssessmentStatus(
                SupportTicketAiAssessment
                        .AiAssessmentStatus
                        .RESPONSE_PUBLISHED
        );

        assessment.setPublishedByUserId(actorUserId);
        assessment.setPublishedAt(now);

        SupportTicketAiAssessment saved = assessmentRepository.saveAndFlush(assessment);

        auditService.recordSuccess(
                ProcurementAuditEvent.AuditEventType.SUPPORT_TICKET_AI_RESPONSE_PUBLISHED,
                ticket.getTenderId(),
                ticket.getBidId(),
                actorUserId,
                "Approved AI ticket response published",
                "Assessment reference="
                        + saved.getAssessmentReference()
                        + ", ticket reference="
                        + ticket.getTicketReference()
                        + ", ticket type="
                        + ticket.getTicketType()
                        + ", publicClarification="
                        + (
                ticket.getTicketType()
                        == SupportTicket.TicketType
                        .CLARIFICATION_REQUEST
        )
);

        log.info("Approved AI response published: assessmentId={} ticketId={} publishedByUserId={}", saved.getId(),
                ticket.getId(),
                actorUserId
        );

        return SupportTicketAiAssessmentResponse.from(saved);
    }

    private SupportTicket findTicket(Long ticketId) {

        return ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new SupportTicketException("Support ticket not found with ID: " + ticketId));
    }

    private SupportTicketAiAssessment findAssessment(Long assessmentId) {

        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new TicketAiAssessmentException("Ticket AI assessment not found with ID: " + assessmentId));

    }

    private void assertAssessmentIsGenerated(SupportTicketAiAssessment assessment) {

        if (assessment.getAssessmentStatus()
                != SupportTicketAiAssessment.AiAssessmentStatus.GENERATED) {

            throw new TicketAiAssessmentStateException("Only GENERATED AI assessments can be approved, "
                            + "rejected, or escalated.");
        }
    }

    private void validateTriageResult(TicketAiTriageResult result) {

        if (result == null) {
            throw new TicketAiAssessmentException("Ticket AI triage service returned no assessment result.");
        }
        if (result.getPriority() == null) {
            throw new TicketAiAssessmentException("Ticket AI assessment priority is missing.");
        }

        if (result.getResolutionMode() == null) {
            throw new TicketAiAssessmentException("Ticket AI assessment resolution mode is missing.");
        }

        if (!hasText(result.getSummary())) {
            throw new TicketAiAssessmentException("Ticket AI assessment summary is missing.");
        }

        if (result.getConfidenceScore() == null) {
            throw new TicketAiAssessmentException("Ticket AI assessment confidence score is missing.");
        }

        if (result.getConfidenceScore().signum() < 0 || result.getConfidenceScore() .compareTo(new java.math.BigDecimal("100.00")) > 0) {

            throw new TicketAiAssessmentException("Ticket AI assessment confidence score must be " + "between 0 and 100.");
        }

        if (!hasText(result.getModelVersion())) {
            throw new TicketAiAssessmentException("Ticket AI assessment model version is missing.");
        }

        if (result.getResolutionMode()
                != SupportTicketAiAssessment
                .AiResolutionMode
                .HUMAN_ONLY
                && !hasText(result.getSuggestedResponse())) {

            throw new TicketAiAssessmentException("Ticket AI assessment suggested response is missing.");
        }
    }

    private String resolveApprovedResponse(SupportTicketAiAssessment assessment, ApproveTicketAiAssessmentRequest request) {

            if (request != null

                    && hasText(request.getApprovedResponse())) {

                return request.getApprovedResponse().trim();
            }

            if (hasText(assessment.getSuggestedResponse())) {
                return assessment.getSuggestedResponse().trim();
            }

            throw new TicketAiAssessmentException("The assessment does not contain a suggested response. "
                            + "Provide an approved response explicitly.");
        }

        private boolean isEditedResponse(SupportTicketAiAssessment assessment, ApproveTicketAiAssessmentRequest request) {

            if (request == null || !hasText(request.getApprovedResponse())) {
                return false;
            }

            String requestedResponse = request.getApprovedResponse().trim();

            String suggestedResponse = trimToNull(assessment.getSuggestedResponse());

            return !requestedResponse.equals(suggestedResponse);
        }

        private String generateAssessmentReference() {
            return "KB-AI-TICKET-"
                    + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
        }

        private String trimToNull(String value)
        {

            if (value == null || value.isBlank()) {
                return null;
            }

            return value.trim();
        }

        private boolean hasText(String value)
        {
            return value != null && !value.isBlank();
        }

    private void assertAssessmentCanBePublished(SupportTicketAiAssessment assessment) {

        boolean approved = assessment.getAssessmentStatus() == SupportTicketAiAssessment.AiAssessmentStatus.APPROVED;

        boolean editedAndApproved = assessment.getAssessmentStatus() == SupportTicketAiAssessment.AiAssessmentStatus.EDITED_AND_APPROVED;

        if (!approved && !editedAndApproved) {

            throw new TicketAiAssessmentStateException("Only APPROVED or EDITED_AND_APPROVED assessments can be published.");
        }

        if (!hasText(assessment.getApprovedResponse())) {
            throw new TicketAiAssessmentException("Approved AI assessment does not contain a response to publish.");
        }

        if (assessment.getResolutionMode() == SupportTicketAiAssessment.AiResolutionMode.HUMAN_ONLY) {

            throw new TicketAiAssessmentStateException("HUMAN_ONLY assessments cannot be published as AI responses.");
        }
    }

    private void assertTicketCanReceivePublishedResponse(SupportTicket ticket) {

        if (ticket.getStatus() == SupportTicket.TicketStatus.CLOSED) {
            throw new TicketAiAssessmentStateException("Approved AI response cannot be published because the ticket is closed.");
        }

        if (ticket.getStatus() == SupportTicket.TicketStatus.REJECTED) {
            throw new TicketAiAssessmentStateException("Approved AI response cannot be published because the ticket is rejected.");
        }

        if (ticket.getStatus() == SupportTicket.TicketStatus.RESPONDED) {
            throw new TicketAiAssessmentStateException("Approved AI response cannot be published because the ticket already has a response.");
        }
    }
}
