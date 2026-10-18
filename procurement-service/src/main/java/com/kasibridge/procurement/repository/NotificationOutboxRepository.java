package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.NotificationOutbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository
        extends JpaRepository<NotificationOutbox, Long> {

    Page<NotificationOutbox> findByStatus(
            NotificationOutbox.NotificationStatus status,
            Pageable pageable
    );

    Page<NotificationOutbox> findByRecipientUserId(
            Long recipientUserId,
            Pageable pageable
    );

    Page<NotificationOutbox> findByRelatedTenderId(
            Long relatedTenderId,
            Pageable pageable
    );

    Page<NotificationOutbox> findByRelatedTicketId(
            Long relatedTicketId,
            Pageable pageable
    );

    Page<NotificationOutbox> findByChannel(
            NotificationOutbox.NotificationChannel channel,
            Pageable pageable
    );

    Page<NotificationOutbox> findByChannelAndReadAtIsNull(
            NotificationOutbox.NotificationChannel channel,
            Pageable pageable
    );

    Page<NotificationOutbox> findByChannelAndReadAtIsNotNull(
            NotificationOutbox.NotificationChannel channel,
            Pageable pageable
    );

    List<NotificationOutbox>
    findByStatusAndChannelOrderByCreatedAtAsc(
            NotificationOutbox.NotificationStatus status,
            NotificationOutbox.NotificationChannel channel,
            Pageable pageable
    );

    List<NotificationOutbox>
    findByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
            Collection<NotificationOutbox.NotificationStatus> statuses,
            int retryCount,
            Pageable pageable
    );

    List<NotificationOutbox>
    findByStatusInAndChannelAndRetryCountLessThanOrderByCreatedAtAsc(
            Collection<NotificationOutbox.NotificationStatus> statuses,
            NotificationOutbox.NotificationChannel channel,
            int retryCount,
            Pageable pageable
    );

    Page<NotificationOutbox> findByChannelAndRecipientUserId(
            NotificationOutbox.NotificationChannel channel,
            Long recipientUserId,
            Pageable pageable
    );

    Page<NotificationOutbox>
    findByChannelAndRecipientUserIdAndReadAtIsNull(
            NotificationOutbox.NotificationChannel channel,
            Long recipientUserId,
            Pageable pageable
    );

    Page<NotificationOutbox>
    findByChannelAndRecipientUserIdAndReadAtIsNotNull(
            NotificationOutbox.NotificationChannel channel,
            Long recipientUserId,
            Pageable pageable
    );

    boolean existsByTemplateTypeAndRelatedTicketIdAndRecipientUserId(
            NotificationOutbox.NotificationTemplateType templateType,
            Long relatedTicketId,
            Long recipientUserId
    );

    Optional<NotificationOutbox> findByProviderMessageId(
            String providerMessageId
    );

    /*
     * Legacy combined claim query.
     *
     * Keep this temporarily because the existing concurrency integration
     * tests still exercise claimBatch(), which currently calls this method.
     */
    @Query(
            value = """
                    SELECT notification.*
                    FROM public.notification_outbox notification
                    WHERE notification.channel = :channel
                      AND notification.retry_count < :maxRetries
                      AND (
                          notification.status = 'PENDING'
                          OR (
                              notification.status = 'FAILED'
                              AND (
                                  notification.next_attempt_at IS NULL
                                  OR notification.next_attempt_at
                                      <= CURRENT_TIMESTAMP
                              )
                          )
                          OR (
                              notification.status = 'PROCESSING'
                              AND notification.claim_expires_at IS NOT NULL
                              AND notification.claim_expires_at
                                  < CURRENT_TIMESTAMP
                          )
                      )
                    ORDER BY
                        notification.created_at,
                        notification.id
                    FOR UPDATE SKIP LOCKED
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    List<NotificationOutbox>
    findDispatchCandidatesForUpdate(
            @Param("channel")
            String channel,
            @Param("maxRetries")
            int maxRetries,
            @Param("batchSize")
            int batchSize
    );

    /*
     * Claims only new notifications that have not yet entered processing.
     */
    @Query(
            value = """
                    SELECT notification.*
                    FROM public.notification_outbox notification
                    WHERE notification.channel = :channel
                      AND notification.status = 'PENDING'
                      AND notification.retry_count < :maxRetries
                    ORDER BY
                        notification.created_at,
                        notification.id
                    FOR UPDATE SKIP LOCKED
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    List<NotificationOutbox>
    findPendingDispatchCandidatesForUpdate(
            @Param("channel")
            String channel,
            @Param("maxRetries")
            int maxRetries,
            @Param("batchSize")
            int batchSize
    );

    /*
     * Claims eligible failed notifications and abandoned processing claims.
     *
     * FAILED rows must have reached next_attempt_at.
     * PROCESSING rows must have an expired lease.
     */
    @Query(
            value = """
                    SELECT notification.*
                    FROM public.notification_outbox notification
                    WHERE notification.channel = :channel
                      AND notification.retry_count < :maxRetries
                      AND (
                          (
                              notification.status = 'FAILED'
                              AND (
                                  notification.next_attempt_at IS NULL
                                  OR notification.next_attempt_at
                                      <= CURRENT_TIMESTAMP
                              )
                          )
                          OR (
                              notification.status = 'PROCESSING'
                              AND notification.claim_expires_at IS NOT NULL
                              AND notification.claim_expires_at
                                  < CURRENT_TIMESTAMP
                          )
                      )
                    ORDER BY
                        notification.created_at,
                        notification.id
                    FOR UPDATE SKIP LOCKED
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    List<NotificationOutbox>
    findRetryDispatchCandidatesForUpdate(
            @Param("channel")
            String channel,
            @Param("maxRetries")
            int maxRetries,
            @Param("batchSize")
            int batchSize
    );

    /*
     * Locks one notification for an explicit single-item dispatch claim.
     */
    @Query(
            value = """
                    SELECT notification.*
                    FROM public.notification_outbox notification
                    WHERE notification.id = :notificationId
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    Optional<NotificationOutbox>
    findByIdForDispatchClaim(
            @Param("notificationId")
            Long notificationId
    );
}