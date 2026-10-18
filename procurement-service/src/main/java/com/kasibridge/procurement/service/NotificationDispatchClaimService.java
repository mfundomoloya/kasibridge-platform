package com.kasibridge.procurement.service;

import com.kasibridge.procurement.entity.NotificationOutbox;

import java.util.List;
import java.util.Optional;

public interface NotificationDispatchClaimService {

    List<NotificationOutbox> claimBatch(
            NotificationOutbox.NotificationChannel channel,
            int maxRetries,
            int batchSize,
            String workerId,
            int leaseSeconds
    );

    Optional<NotificationOutbox> claimOne(
            Long notificationId,
            int maxRetries,
            String workerId,
            int leaseSeconds
    );
}