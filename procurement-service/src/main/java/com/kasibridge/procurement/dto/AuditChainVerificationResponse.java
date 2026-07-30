package com.kasibridge.procurement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditChainVerificationResponse {

    private boolean valid;
    private long checkedEvents;
    private Long failedEventId;
    private String message;
}
