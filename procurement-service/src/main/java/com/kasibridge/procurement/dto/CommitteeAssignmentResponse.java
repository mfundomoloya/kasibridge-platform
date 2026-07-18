package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommitteeAssignmentResponse {

    private Long id;
    private Long tenderId;
    private Long userId;
    private TenderCommitteeAssignment.CommitteeRole committeeRole;
    private Long assignedByUserId;
    private boolean active;
    private LocalDateTime assignedAt;
    private String reason;

    public static CommitteeAssignmentResponse from(TenderCommitteeAssignment assignment){
        return CommitteeAssignmentResponse.builder()
                .id(assignment.getId())
                .tenderId(assignment.getTenderId())
                .userId(assignment.getUserId())
                .committeeRole(assignment.getCommitteeRole())
                .assignedByUserId(assignment.getAssignedByUserId())
                .active(assignment.isActive())
                .assignedAt(assignment.getAssignedAt())
                .reason(assignment.getReason())
                .build();
    }
}
