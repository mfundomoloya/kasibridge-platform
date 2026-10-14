package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupportTicketAiAssessmentRepository extends JpaRepository<SupportTicketAiAssessment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT assessment
            FROM SupportTicketAiAssessment assessment
            WHERE assessment.id = :assessmentId
            """)
    Optional<SupportTicketAiAssessment>
    findByIdForUpdate(
            @Param("assessmentId")
            Long assessmentId
    );

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
