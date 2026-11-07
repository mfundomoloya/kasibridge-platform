package com.kasibridge.fraud.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(
        ignoreUnknown = true
)
public record AwardIntegritySignalResponse(

        String code,

        String severity,

        String message,

        @JsonProperty("observed_value")
        Object observedValue,

        Object threshold,

        @JsonProperty("risk_points")
        Integer riskPoints
) {
}