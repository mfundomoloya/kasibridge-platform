package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Page<SupportTicket> findByTenderId(Long tenderId, Pageable pageable);

    Page<SupportTicket> findByCreatedByUserId(Long createdByUserId, Pageable pageable);

    Page<SupportTicket> findByTenderIdAndPublicClarificationTrue(Long tenderId, Pageable pageable);

    Page<SupportTicket> findByContactPhoneNumberContainingIgnoreCase(String phoneNumber, Pageable pageable);

    Page<SupportTicket> findByBusinessNameSnapshotContainingIgnoreCase(String businessName, Pageable pageable);

    Page<SupportTicket> findByContactNameContainingIgnoreCase(String contactName, Pageable pageable);
}
