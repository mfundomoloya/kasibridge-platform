package com.kasibridge.procurement.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tender_committee_assignments",
        indexes = {
                @Index(name = "idx_committee_tender_id", columnList = "tender_id"),
                @Index(name = "idx_committee_user_id", columnList = "user_id"),
                @Index(name = "idx_committee_tender_user", columnList = "tender_id, user_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenderCommitteeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "committee_role", nullable = false, length = 40)
    private CommitteeRole committeeRole;

    @Column(name = "assigned_by_user_id", nullable = false)
    private Long assignedByUserId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by_user_id")
    private Long revokedByUserId;

    @Column(name = "reason", length = 500)
    private String reason;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
        active = true;
    }

    public enum CommitteeRole {
        SPECIFICATION_OFFICER,
        EVALUATOR,
        ADJUDICATOR
    }

}
