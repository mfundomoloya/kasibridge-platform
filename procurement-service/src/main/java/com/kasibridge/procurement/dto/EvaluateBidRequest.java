package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EvaluateBidRequest {

    @NotNull(message = "Evaluator user ID is required")
    private Long evaluatorUserId;

    @NotNull(message = "Technical score is required")
    @DecimalMin(value = "0.00", message = "Technical score cannot be below 0")
    @DecimalMax(value = "100.00", message = "Technical score cannot exceed 100")
    private BigDecimal technicalScore;

    @NotNull(message = "Price score is required")
    @DecimalMin(value = "0.00", message = "Technical score cannot be below 0")
    @DecimalMax(value = "100.00", message = "Technical score cannot exceed 100")
    private BigDecimal priceScore;

    @NotBlank(message = "Comments are required")
    private String comments;
}
