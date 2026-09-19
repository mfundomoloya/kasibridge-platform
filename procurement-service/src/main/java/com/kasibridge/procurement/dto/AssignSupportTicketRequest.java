package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssignSupportTicketRequest {

    @NotNull(message = "Assigned user ID is required")
    private Long assignedToUserId;

    @NotBlank(message = "Assignment reason is required")
    @Size(max = 1000, message = "Assignment reason cannot exceed 1000 characters")
    private String assignmentReason;
}