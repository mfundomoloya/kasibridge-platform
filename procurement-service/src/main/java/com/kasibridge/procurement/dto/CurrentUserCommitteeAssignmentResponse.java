package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.entity.TenderCommitteeAssignment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CurrentUserCommitteeAssignmentResponse {

    private Long assignmentId;

    private Long tenderId;

    private String tenderReference;

    private String tenderTitle;

    private String buyerOrgId;

    private BigDecimal budgetAmount;

    private Tender.TenderStatus tenderStatus;

    private TenderCommitteeAssignment.CommitteeRole
            committeeRole;

    private Long userId;

    private Long assignedByUserId;

    private LocalDateTime assignedAt;

    private String assignmentReason;

    private boolean active;

    private LocalDateTime tenderPublishedAt;

    private LocalDateTime tenderClosedAt;

    private LocalDateTime tenderUpdatedAt;

    public static CurrentUserCommitteeAssignmentResponse
    from(
            TenderCommitteeAssignment assignment,
            Tender tender
    ) {
        if (assignment == null) {
            throw new IllegalArgumentException(
                    "Committee assignment is required."
            );
        }

        if (tender == null) {
            throw new IllegalArgumentException(
                    "Tender is required."
            );
        }

        if (
                assignment.getTenderId() == null
                        || !assignment.getTenderId()
                        .equals(tender.getId())
        ) {
            throw new IllegalArgumentException(
                    "Committee assignment tender ID "
                            + "does not match the tender."
            );
        }

        return CurrentUserCommitteeAssignmentResponse
                .builder()
                .assignmentId(
                        assignment.getId()
                )
                .tenderId(
                        tender.getId()
                )
                .tenderReference(
                        tender.getTenderReference()
                )
                .tenderTitle(
                        tender.getTitle()
                )
                .buyerOrgId(
                        tender.getBuyerOrgId()
                )
                .budgetAmount(
                        tender.getBudgetAmount()
                )
                .tenderStatus(
                        tender.getStatus()
                )
                .committeeRole(
                        assignment.getCommitteeRole()
                )
                .userId(
                        assignment.getUserId()
                )
                .assignedByUserId(
                        assignment.getAssignedByUserId()
                )
                .assignedAt(
                        assignment.getAssignedAt()
                )
                .assignmentReason(
                        assignment.getReason()
                )
                .active(
                        assignment.isActive()
                )
                .tenderPublishedAt(
                        tender.getPublishedAt()
                )
                .tenderClosedAt(
                        tender.getClosedAt()
                )
                .tenderUpdatedAt(
                        tender.getUpdatedAt()
                )
                .build();
    }
}