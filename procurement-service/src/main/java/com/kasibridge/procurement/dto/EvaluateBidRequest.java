package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EvaluateBidRequest {

    @NotNull(message = "Evaluator user ID is required")
    private Long evaluatorUserId;

    @NotNull(message = "Technical score is required")
    @DecimalMin(value = "0.00", message = "Technical score cannot be below 0")
    @DecimalMax(value = "100.0", message = "Technical score cannot exceed 100")
    private BigDecimal technicalScore;

    @NotNull(message = "Price score is required")
    @DecimalMin(value = "0.00", message = "Technical score cannot be below 0")
    @DecimalMax(value = "100.00", message = "Technical score cannot exceed 100")
    private BigDecimal priceScore;

    @NotBlank(message = "Comments are required")
    @Size(min = 10, max = 1000, message = "Comments cannot exceed 1000 characters")
    private String comments;
}
