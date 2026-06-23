package com.kasibridge.fraud.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FraudDashboardSummary {

    private long totalAlerts;

    private long openAlerts;
    private long reviewedAlerts;
    private long dismissedAlerts;
    private long escalatedAlerts;

    private long lowSeverityAlerts;
    private long mediumSeverityAlerts;
    private long highSeverityAlerts;

    private long backdatedTransactionAlerts;
    private long highValueTransactionAlerts;
    private long duplicateTransactionAlerts;

}
