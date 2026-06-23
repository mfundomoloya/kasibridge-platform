package com.kasibridge.fraud.dto;

import com.kasibridge.fraud.entity.FraudAlert;

public class FraudAlertMapper {

    private FraudAlertMapper() {
    }

    public static FraudAlertResponse toResponse(FraudAlert alert) {
        return new FraudAlertResponse(
                alert.getId(),
                alert.getAlertReference(),
                alert.getTraderId(),
                alert.getTransactionId(),
                alert.getPatternType(),
                alert.getSeverity(),
                alert.getDescription(),
                alert.getEvidence(),
                alert.getTransactionAmount(),
                alert.getStatus(),
                alert.getDetectedAt()
        );
    }

}
