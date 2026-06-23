package com.kasibridge.fraud.dto;

import com.kasibridge.fraud.entity.FraudAlert;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FraudAlertResponse {

    private Long id;
    private String alertReference;
    private Long traderId;
    private Long transactionId;
    private FraudAlert.FraudPatternType patternType;
    private FraudAlert.Severity severity;
    private String description;
    private String evidence;
    private BigDecimal transactionAmount;
    private FraudAlert.AlertStatus status;
    private LocalDateTime detectedAt;

}
