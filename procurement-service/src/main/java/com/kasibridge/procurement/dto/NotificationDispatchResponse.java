package com.kasibridge.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationDispatchResponse {

    private int selected;
    private int processed;
    private int sent;
    private int failed;
    private int skipped;

    private List<Long> sentNotificationIds;
    private List<Long> failedNotificationIds;
    private List<Long> skippedNotificationIds;
}
