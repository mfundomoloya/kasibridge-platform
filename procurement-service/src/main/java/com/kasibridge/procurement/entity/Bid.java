package com.kasibridge.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bids",
        indexes = {
                @Index(name = "idx_bid_reference", columnList = "bid_reference"),
                @Index(name = "idx_bid_tender_id", columnList = "tender_id"),
                @Index(name = "idx_bid_trader_id", columnList = "trader_id"),
                @Index(name = "idx_bid_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_reference",  nullable = false, unique = true, length = 50)
    private String bidReference;

    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "trader_id", nullable = false)
    private Long traderId;


    @Column(name = "bidder_alias", nullable = false, length = 50)
    private String bidderAlias;

    @Column(name = "technical_proposal", nullable = false, length = 5000)
    private String technicalProposal;

    @Column(name = "price_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BidStatus status;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = BidStatus.SUBMITTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum BidStatus {
        SUBMITTED,
        COMPLIANCE_FAILED,
        COMPLIANT,
        UNDER_EVALUATION,
        RECOMMENDED,
        REJECTED,
        AWARDED
    }

}
