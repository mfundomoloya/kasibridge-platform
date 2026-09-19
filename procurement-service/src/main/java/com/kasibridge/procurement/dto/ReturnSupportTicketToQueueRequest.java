package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReturnSupportTicketToQueueRequest {

    @NotBlank(message = "Return-to-queue reason is required")
    @Size(max = 1000,message = "Return-to-queue reason cannot exceed 1000 characters")
    private String reason;
}