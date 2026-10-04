package com.kasibridge.fraud.dto;

import com.kasibridge.fraud.entity.FraudAlert;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FraudAlertSearchCriteria {
    private Long traderId;
    private FraudAlert.AlertStatus status;
    private FraudAlert.FraudPatternType patternType;
    private FraudAlert.Severity severity;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private BigDecimal amountMin;
    private BigDecimal amountMax;
    private String alertReference;
}
