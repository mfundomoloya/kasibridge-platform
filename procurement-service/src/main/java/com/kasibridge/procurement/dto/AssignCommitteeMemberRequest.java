package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssignCommitteeMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Committee role is required")
    private TenderCommitteeAssignment.CommitteeRole committeeRole;

    @NotNull(message = "Assigned by user ID is required")
    private Long assignedByUserId;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
