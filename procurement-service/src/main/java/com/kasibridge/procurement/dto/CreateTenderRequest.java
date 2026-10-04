package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTenderRequest {

    @NotBlank(message = "Tender title is required")
    private String title;

    @NotBlank(message = "Tender description is required")
    private String description;

    @NotBlank(message = "Evaluation criteria is required")
    private String evaluationCriteria;

    @NotNull(message = "Budget amount is required")
    @DecimalMin(value = "0.01", message = "Budget amount must be greater than zero")
    private BigDecimal budgetAmount;

    @NotBlank(message = "Buyer organisation ID is required")
    private String buyerOrgId;

    @NotNull(message = "Created by user ID is required")
    private Long createdByUserId;

}
