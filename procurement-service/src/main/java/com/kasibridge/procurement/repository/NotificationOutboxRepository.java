package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.NotificationOutbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    Page<NotificationOutbox> findByStatus(NotificationOutbox.NotificationStatus status, Pageable pageable);

    Page<NotificationOutbox> findByRecipientUserId(Long recipientUserId, Pageable pageable);

    Page<NotificationOutbox> findByRelatedTenderId(Long relatedTenderId, Pageable pageable);

    Page<NotificationOutbox> findByRelatedTicketId(Long relatedTicketId, Pageable pageable);
}
