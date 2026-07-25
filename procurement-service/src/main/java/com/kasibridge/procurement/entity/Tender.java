package com.kasibridge.procurement.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenders",
        indexes = {
                @Index(name = "idx_tender_reference", columnList = "tender_reference"),
                @Index(name = "idx_tender_status", columnList = "status"),
                @Index(name = "idx_tender_buyer_org", columnList = "buyer_org_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_reference", nullable = false, unique = true, length = 40)
    private String tenderReference;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 3000)
    private String description;

    @Column(name = "evaluation_criteria", nullable = false, length = 3000)
    private String evaluationCriteria;

    @Column(name = "budget_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "buyer_org_id", nullable = false, length = 100)
    private String buyerOrgId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TenderStatus status;

    @Column(name = "specification_hash", length = 128)
    private String specificationHash;

    @Column(name = "awarded_bid_id")
    private Long awardedBidId;

    @Column(name = "awarded_by_user_id")
    private Long awardedByUserId;

    @Column(name = "awarded_at")
    private LocalDateTime awardedAt;

    @Column(name = "award_reason", length = 1000)
    private String awardReason;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = TenderStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isPublishedOrBeyond() {
        return status != TenderStatus.DRAFT;
    }

    public enum TenderStatus {
        DRAFT,
        PUBLISHED,
        BIDDING_CLOSED,
        EVALUATION,
        ADJUDICATION,
        AWARDED,
        CANCELLED
    }

}
