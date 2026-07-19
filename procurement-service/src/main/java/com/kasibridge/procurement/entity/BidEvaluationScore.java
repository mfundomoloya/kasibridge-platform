package com.kasibridge.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bid_evaluation_scores",
        indexes = {
                @Index(name = "idx_eval_tender_id", columnList = "tender_id"),
                @Index(name = "idx_eval_bid_id", columnList = "bid_id"),
                @Index(name = "idx_eval_evaluator_user_id", columnList = "evaluator_user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_eval_bid_evaluator",
                        columnNames = {"bid_id", "evaluator_user_id"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidEvaluationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_id", nullable = false)
    private Long tenderId;

    @Column(name = "bid_id", nullable = false)
    private Long bidId;

    @Column(name = "evaluator_user_id", nullable = false)
    private Long evaluatorUserId;

    @Column(name = "technical_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal technicalScore;

    @Column(name = "price_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal priceScore;

    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "comments", nullable = false, length = 1000)
    private String comments;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
