package com.kasibridge.fraud.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record AwardIntegrityAssessmentResponse(

        @JsonProperty("tender_id")
        Long tenderId,

        @JsonProperty("assessment_type")
        String assessmentType,

        @JsonProperty("risk_score")
        BigDecimal riskScore,

        @JsonProperty("risk_level")
        String riskLevel,

        String recommendation,

        @JsonProperty("rule_version")
        String ruleVersion,

        @JsonProperty("model_used")
        Boolean modelUsed,

        @JsonProperty(
                "award_consistent_with_rules"
        )
        Boolean awardConsistentWithRules,

        List<AwardIntegritySignalResponse>
        signals,

        @JsonProperty("assessed_at")
        OffsetDateTime assessedAt
) {

    public AwardIntegrityAssessmentResponse {
        signals =
                signals == null
                        ? List.of()
                        : List.copyOf(
                        signals
                );
    }
}