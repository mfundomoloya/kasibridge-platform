package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.SupportTicket;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ticket
        FROM SupportTicket ticket
        WHERE ticket.id = :ticketId
        """)
    Optional<SupportTicket> findByIdForUpdate(
            @Param("ticketId") Long ticketId
    );

    Page<SupportTicket> findByTenderId(Long tenderId, Pageable pageable);

    Page<SupportTicket> findByCreatedByUserId(Long createdByUserId, Pageable pageable);

    Page<SupportTicket> findByTenderIdAndPublicClarificationTrue(Long tenderId, Pageable pageable);

    Page<SupportTicket> findByContactPhoneNumberContainingIgnoreCase(String phoneNumber, Pageable pageable);

    Page<SupportTicket> findByBusinessNameSnapshotContainingIgnoreCase(String businessName, Pageable pageable);

    Page<SupportTicket> findByContactNameContainingIgnoreCase(String contactName, Pageable pageable);

    Page<SupportTicket> findByAssignedToUserId(Long assignedToUserId, Pageable pageable);

    Page<SupportTicket> findByAssignedToUserIdAndStatus(Long assignedToUserId, SupportTicket.TicketStatus status, Pageable pageable);

    Page<SupportTicket> findByAssignedToUserIdIsNull(Pageable pageable);
}
