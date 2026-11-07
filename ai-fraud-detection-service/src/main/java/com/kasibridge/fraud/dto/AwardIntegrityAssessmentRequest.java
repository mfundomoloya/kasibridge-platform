package com.kasibridge.fraud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AwardIntegrityAssessmentRequest(

        @JsonProperty("tender_id")
        @NotNull
        @Positive
        Long tenderId,

        @JsonProperty("budget_amount")
        @NotNull
        @DecimalMin(
                value = "0.01"
        )
        BigDecimal budgetAmount,

        @JsonProperty("awarded_amount")
        @NotNull
        @DecimalMin(
                value = "0.00"
        )
        BigDecimal awardedAmount,

        @JsonProperty("submitted_bid_count")
        @PositiveOrZero
        long submittedBidCount,

        @JsonProperty("eligible_bid_count")
        @PositiveOrZero
        long eligibleBidCount,

        @JsonProperty("withdrawn_bid_count")
        @PositiveOrZero
        long withdrawnBidCount,

        @JsonProperty("active_evaluator_count")
        @PositiveOrZero
        long activeEvaluatorCount,

        @JsonProperty("expected_score_count")
        @PositiveOrZero
        long expectedScoreCount,

        @JsonProperty("submitted_score_count")
        @PositiveOrZero
        long submittedScoreCount,

        @JsonProperty("average_technical_score")
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal averageTechnicalScore,

        @JsonProperty("average_price_score")
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("20.00")
        BigDecimal averagePriceScore,

        @JsonProperty("average_total_score")
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal averageTotalScore,

        @JsonProperty("minimum_technical_threshold")
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal minimumTechnicalThreshold,

        @JsonProperty("award_rank")
        @Positive
        int awardRank,

        @JsonProperty(
                "specification_officer_user_id"
        )
        @NotNull
        @Positive
        Long specificationOfficerUserId,

        @JsonProperty("evaluator_user_ids")
        @NotEmpty
        List<
                @NotNull
                @Positive
                        Long
                > evaluatorUserIds,

        @JsonProperty("adjudicator_user_id")
        @NotNull
        @Positive
        Long adjudicatorUserId,

        @JsonProperty("published_at")
        @NotNull
        LocalDateTime publishedAt,

        @JsonProperty("bidding_closed_at")
        @NotNull
        LocalDateTime biddingClosedAt,

        @JsonProperty("awarded_at")
        @NotNull
        LocalDateTime awardedAt
) {

    public AwardIntegrityAssessmentRequest {
        evaluatorUserIds =
                evaluatorUserIds == null
                        ? null
                        : List.copyOf(
                        evaluatorUserIds
                );
    }
}