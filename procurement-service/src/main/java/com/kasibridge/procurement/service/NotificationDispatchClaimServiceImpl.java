package com.kasibridge.procurement.service;

import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.exception.NotificationOutboxException;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchClaimServiceImpl
        implements NotificationDispatchClaimService {

    private final NotificationOutboxRepository repository;

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public List<NotificationOutbox> claimPendingBatch(
            NotificationOutbox.NotificationChannel channel,
            int maxRetries,
            int batchSize,
            String workerId,
            int leaseSeconds
    ) {
        validateClaimRequest(
                channel,
                maxRetries,
                batchSize,
                workerId,
                leaseSeconds
        );

        List<NotificationOutbox> candidates =
                repository
                        .findPendingDispatchCandidatesForUpdate(
                                channel.name(),
                                maxRetries,
                                batchSize
                        );

        return claimCandidates(
                candidates,
                channel,
                workerId,
                leaseSeconds,
                "pending"
        );
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public List<NotificationOutbox> claimRetryBatch(
            NotificationOutbox.NotificationChannel channel,
            int maxRetries,
            int batchSize,
            String workerId,
            int leaseSeconds
    ) {
        validateClaimRequest(
                channel,
                maxRetries,
                batchSize,
                workerId,
                leaseSeconds
        );

        List<NotificationOutbox> candidates =
                repository
                        .findRetryDispatchCandidatesForUpdate(
                                channel.name(),
                                maxRetries,
                                batchSize
                        );

        return claimCandidates(
                candidates,
                channel,
                workerId,
                leaseSeconds,
                "retry"
        );
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public List<NotificationOutbox> claimBatch(
            NotificationOutbox.NotificationChannel channel,
            int maxRetries,
            int batchSize,
            String workerId,
            int leaseSeconds
    ) {
        validateClaimRequest(
                channel,
                maxRetries,
                batchSize,
                workerId,
                leaseSeconds
        );

        List<NotificationOutbox> candidates =
                repository
                        .findDispatchCandidatesForUpdate(
                                channel.name(),
                                maxRetries,
                                batchSize
                        );

        return claimCandidates(
                candidates,
                channel,
                workerId,
                leaseSeconds,
                "combined"
        );
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public Optional<NotificationOutbox> claimOne(
            Long notificationId,
            int maxRetries,
            String workerId,
            int leaseSeconds
    ) {
        if (notificationId == null) {
            throw new NotificationOutboxException(
                    "Notification ID is required."
            );
        }

        validateWorkerConfiguration(
                maxRetries,
                workerId,
                leaseSeconds
        );

        Optional<NotificationOutbox> optionalNotification =
                repository
                        .findByIdForDispatchClaim(
                                notificationId
                        );

        if (optionalNotification.isEmpty()) {
            return Optional.empty();
        }

        NotificationOutbox notification =
                optionalNotification.get();

        LocalDateTime now =
                LocalDateTime.now();

        assertClaimable(
                notification,
                maxRetries,
                now
        );

        claimNotification(
                notification,
                workerId,
                now,
                now.plusSeconds(
                        leaseSeconds
                )
        );

        return Optional.of(
                repository.saveAndFlush(
                        notification
                )
        );
    }

    private List<NotificationOutbox> claimCandidates(
            List<NotificationOutbox> candidates,
            NotificationOutbox.NotificationChannel channel,
            String workerId,
            int leaseSeconds,
            String claimType
    ) {
        LocalDateTime claimedAt =
                LocalDateTime.now();

        LocalDateTime expiresAt =
                claimedAt.plusSeconds(
                        leaseSeconds
                );

        candidates.forEach(notification ->
                claimNotification(
                        notification,
                        workerId,
                        claimedAt,
                        expiresAt
                )
        );

        if (!candidates.isEmpty()) {
            repository.saveAllAndFlush(
                    candidates
            );
        }

        log.info(
                "Notification dispatch batch claimed: "
                        + "type={} workerId={} channel={} count={}",
                claimType,
                workerId,
                channel,
                candidates.size()
        );

        return List.copyOf(
                candidates
        );
    }

    private void claimNotification(
            NotificationOutbox notification,
            String workerId,
            LocalDateTime claimedAt,
            LocalDateTime claimExpiresAt
    ) {
        notification.setStatus(
                NotificationOutbox
                        .NotificationStatus
                        .PROCESSING
        );

        notification.setClaimedAt(
                claimedAt
        );

        notification.setClaimedBy(
                workerId
        );

        notification.setClaimExpiresAt(
                claimExpiresAt
        );

        notification.setNextAttemptAt(
                null
        );

        notification.setRetryCount(
                notification.getRetryCount() + 1
        );

        notification.setUpdatedAt(
                claimedAt
        );
    }

    private void assertClaimable(
            NotificationOutbox notification,
            int maxRetries,
            LocalDateTime now
    ) {
        if (
                notification.getChannel()
                        != NotificationOutbox
                        .NotificationChannel
                        .WHATSAPP
        ) {
            throw new NotificationOutboxException(
                    "Only WhatsApp notifications can "
                            + "currently be dispatched."
            );
        }

        if (
                notification.getStatus()
                        == NotificationOutbox
                        .NotificationStatus
                        .SENT
        ) {
            throw new NotificationOutboxException(
                    "Notification has already been sent."
            );
        }

        if (
                notification.getStatus()
                        == NotificationOutbox
                        .NotificationStatus
                        .CANCELLED
        ) {
            throw new NotificationOutboxException(
                    "Cancelled notification cannot "
                            + "be dispatched."
            );
        }

        if (
                notification.getRetryCount()
                        >= maxRetries
        ) {
            throw new NotificationOutboxException(
                    "Notification has reached the "
                            + "maximum retry limit."
            );
        }

        if (
                notification.getStatus()
                        == NotificationOutbox
                        .NotificationStatus
                        .PROCESSING
                        && notification.getClaimExpiresAt()
                        != null
                        && !notification
                        .getClaimExpiresAt()
                        .isBefore(now)
        ) {
            throw new NotificationOutboxException(
                    "Notification is already claimed "
                            + "by another dispatcher."
            );
        }

        if (
                notification.getStatus()
                        == NotificationOutbox
                        .NotificationStatus
                        .FAILED
                        && notification.getNextAttemptAt()
                        != null
                        && notification
                        .getNextAttemptAt()
                        .isAfter(now)
        ) {
            throw new NotificationOutboxException(
                    "Notification is not yet eligible "
                            + "for another dispatch attempt."
            );
        }
    }

    private void validateClaimRequest(
            NotificationOutbox.NotificationChannel channel,
            int maxRetries,
            int batchSize,
            String workerId,
            int leaseSeconds
    ) {
        if (channel == null) {
            throw new NotificationOutboxException(
                    "Notification channel is required."
            );
        }

        if (batchSize < 1) {
            throw new NotificationOutboxException(
                    "Dispatch batch size must be positive."
            );
        }

        validateWorkerConfiguration(
                maxRetries,
                workerId,
                leaseSeconds
        );
    }

    private void validateWorkerConfiguration(
            int maxRetries,
            String workerId,
            int leaseSeconds
    ) {
        if (maxRetries < 1) {
            throw new NotificationOutboxException(
                    "Maximum retries must be positive."
            );
        }

        if (
                workerId == null
                        || workerId.isBlank()
        ) {
            throw new NotificationOutboxException(
                    "Dispatcher worker ID is required."
            );
        }

        if (workerId.length() > 100) {
            throw new NotificationOutboxException(
                    "Dispatcher worker ID may not "
                            + "exceed 100 characters."
            );
        }

        if (leaseSeconds < 1) {
            throw new NotificationOutboxException(
                    "Dispatch lease duration must "
                            + "be positive."
            );
        }
    }
}