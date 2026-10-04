package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupportTicketAiAssessmentRepository extends JpaRepository<SupportTicketAiAssessment, Long> {
    Optional<SupportTicketAiAssessment> findByTicketId(Long ticketId);

    boolean existsByTicketId(Long ticketId);

    Optional<SupportTicketAiAssessment> findByAssessmentReference(String assessmentReference);

    Page<SupportTicketAiAssessment> findByAssessmentStatus(SupportTicketAiAssessment.AiAssessmentStatus assessmentStatus,
                                                           Pageable pageable);

    Page<SupportTicketAiAssessment> findByResolutionMode(SupportTicketAiAssessment.AiResolutionMode resolutionMode,
                                                         Pageable pageable);

    Page<SupportTicketAiAssessment> findByPriority(SupportTicketAiAssessment.TicketPriority priority,
                                                   Pageable pageable);
}
