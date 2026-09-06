package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.NotificationOutbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    Page<NotificationOutbox> findByStatus(NotificationOutbox.NotificationStatus status, Pageable pageable);

    Page<NotificationOutbox> findByRecipientUserId(Long recipientUserId, Pageable pageable);

    Page<NotificationOutbox> findByRelatedTenderId(Long relatedTenderId, Pageable pageable);

    Page<NotificationOutbox> findByRelatedTicketId(Long relatedTicketId, Pageable pageable);

    List<NotificationOutbox> findByStatusAndChannelOrderByCreatedAtAsc(
            NotificationOutbox.NotificationStatus status,
            NotificationOutbox.NotificationChannel channel,
            Pageable pageable);

    List<NotificationOutbox> findByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
            Collection<NotificationOutbox.NotificationStatus> statuses,
            int retryCount,
            Pageable pageable
    );

    List<NotificationOutbox> findByStatusInAndChannelAndRetryCountLessThanOrderByCreatedAtAsc(
            Collection<NotificationOutbox.NotificationStatus> statuses,
            NotificationOutbox.NotificationChannel channel,
            int retryCount,
            Pageable pageable);
}
