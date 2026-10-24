package com.kasibridge.fraud.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlInferenceResponse(

        String classification,

        @JsonProperty("risk_level")
        String riskLevel,

        @JsonProperty("risk_probability")
        BigDecimal riskProbability,

        @JsonProperty("model_name")
        String modelName,

        @JsonProperty("model_version")
        String modelVersion,

        String experiment,

        @JsonProperty("detection_source")
        String detectionSource,

        @JsonProperty("recommended_action")
        String recommendedAction,

        BigDecimal threshold,

        @JsonProperty("model_sha256")
        String modelSha256
) {
}
