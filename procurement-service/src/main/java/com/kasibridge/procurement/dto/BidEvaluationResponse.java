package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.BidEvaluationScore;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BidEvaluationResponse {

    private Long id;
    private Long tenderId;
    private Long bidId;
    private Long evaluatorUserId;
    private BigDecimal technicalScore;
    private BigDecimal priceScore;
    private BigDecimal totalScore;
    private String comments;
    private LocalDateTime submittedAt;

    public static BidEvaluationResponse from(BidEvaluationScore score) {
        return BidEvaluationResponse.builder()
                .id(score.getId())
                .tenderId(score.getTenderId())
                .bidId(score.getBidId())
                .evaluatorUserId(score.getEvaluatorUserId())
                .technicalScore(score.getTechnicalScore())
                .priceScore(score.getPriceScore())
                .totalScore(score.getTotalScore())
                .comments(score.getComments())
                .submittedAt(score.getSubmittedAt())
                .build();
    }
}
