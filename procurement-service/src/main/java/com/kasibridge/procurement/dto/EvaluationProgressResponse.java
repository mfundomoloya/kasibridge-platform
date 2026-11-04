package com.kasibridge.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EvaluationProgressResponse {

    private Long tenderId;

    private String tenderReference;

    private String tenderTitle;

    private long activeEvaluatorCount;

    private long eligibleBidCount;

    private long expectedScoreCount;

    private long submittedScoreCount;

    private long remainingScoreCount;

    private BigDecimal completionPercentage;

    private boolean evaluationComplete;

    private boolean readyForAdjudication;

    private String blockingReason;
}