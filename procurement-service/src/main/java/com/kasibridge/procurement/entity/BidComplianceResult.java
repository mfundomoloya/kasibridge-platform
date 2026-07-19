package com.kasibridge.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "bid_compliance_results",
        indexes = {
                @Index(name = "idx_compliance_bid_id", columnList = "bid_id"),
                @Index(name = "idx_compliance_passed", columnList = "passed")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidComplianceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_id", nullable = false, unique = true)
    private Long bidId;

    @Column(name = "csd_valid", nullable = false)
    private boolean csdValid;

    @Column(name = "tax_clearance_valid", nullable = false)
    private boolean taxClearanceValid;

    @Column(name = "bbbee_valid", nullable = false)
    private boolean bbbeeValid;

    @Column(name = "required_documents_uploaded", nullable = false)
    private boolean requiredDocumentsUploaded;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    protected void onCreate() {
        checkedAt = LocalDateTime.now();
    }
}
