package com.kasibridge.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AwardTenderRequest {

    @NotNull(message = "Winning bid ID is required")
    private Long winningBidId;

    @NotNull(message = "Adjudicator user ID is required")
    private Long adjudicatorUserId;

    @NotBlank(message = "Award reason is required")
    @Size(max = 1000, message = "Award reason cannot exceed 1000 characters")
    private String awardReason;
}
