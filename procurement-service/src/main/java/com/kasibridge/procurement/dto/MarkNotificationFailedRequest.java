package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MarkNotificationFailedRequest {

    @NotBlank(message = "Failure reason is required")
    @Size(max = 1000, message = "Failure reason cannot exceed 1000 characters")
    private String failureReason;
}
