package com.kasibridge.transaction.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTransactionNotesRequest {
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
