package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.NotificationDispatchResponse;
import com.kasibridge.procurement.dto.NotificationOutboxResponse;

public interface NotificationDispatcherService {
    NotificationDispatchResponse dispatchPending();
    NotificationDispatchResponse retryFailed();
    NotificationOutboxResponse dispatchOne(Long notificationId);
}
