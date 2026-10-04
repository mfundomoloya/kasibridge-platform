package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmitBidRequest {

    @NotBlank(message = "Technical proposal is required")
    @Size(max = 10000, message = "Technical proposal cannot exceed 10000 characters")
    private String technicalProposal;


    @NotNull(message = "Price amount is required")
    @DecimalMin(value = "0.01", message = "Price amount must be greater than 0")
    private BigDecimal priceAmount;

    /*
    * Mocked compliance values.
    * In production, these would come from trusted compliance sources.
 */

    private boolean csdValid;
    private boolean taxClearanceValid;
    private boolean bbbeeValid;
    private boolean requiredDocumentsUploaded;
}
