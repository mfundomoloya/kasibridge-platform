package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.ApproveTicketAiAssessmentRequest;
import com.kasibridge.procurement.dto.EscalateTicketAiAssessmentRequest;
import com.kasibridge.procurement.dto.RejectTicketAiAssessmentRequest;
import com.kasibridge.procurement.dto.SupportTicketAiAssessmentResponse;
import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupportTicketAiAssessmentService {

    SupportTicketAiAssessmentResponse generateAssessment(Long ticketId);

    SupportTicketAiAssessmentResponse getAssessmentByTicketId(Long ticketId);

    SupportTicketAiAssessmentResponse getAssessmentById(Long assessmentId);

    Page<SupportTicketAiAssessmentResponse> getAssessments(Pageable pageable);

    Page<SupportTicketAiAssessmentResponse> getAssessmentsByStatus(SupportTicketAiAssessment.AiAssessmentStatus status, Pageable pageable);

    Page<SupportTicketAiAssessmentResponse> getAssessmentsByPriority(SupportTicketAiAssessment.TicketPriority priority, Pageable pageable);

    Page<SupportTicketAiAssessmentResponse> getAssessmentsByResolutionMode(SupportTicketAiAssessment.AiResolutionMode resolutionMode, Pageable pageable);

    SupportTicketAiAssessmentResponse approveAssessment(Long assessmentId, ApproveTicketAiAssessmentRequest request);

    SupportTicketAiAssessmentResponse rejectAssessment(Long assessmentId, RejectTicketAiAssessmentRequest request);

    SupportTicketAiAssessmentResponse escalateAssessment(Long assessmentId, EscalateTicketAiAssessmentRequest request);
}
