package com.kasibridge.fraud.specification;

import com.kasibridge.fraud.entity.FraudAlert;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FraudAlertSpecifications {
    private FraudAlertSpecifications() {
    }

    public static Specification<FraudAlert> hasTraderId(Long traderId) {
        return (root, query, cb) ->
                traderId == null ? null : cb.equal(root.get("traderId"), traderId);
    }

    public static Specification<FraudAlert> hasStatus(FraudAlert.AlertStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<FraudAlert> hasPatternType(FraudAlert.FraudPatternType patternType) {
        return (root, query, cb) ->
                patternType == null ? null : cb.equal(root.get("patternType"), patternType);
    }

    public static Specification<FraudAlert> hasSeverity(FraudAlert.Severity severity) {
        return (root, query, cb) ->
                severity == null ? null : cb.equal(root.get("severity"), severity);
    }

    public static Specification<FraudAlert> detectedAfter(LocalDateTime fromDate) {
        return (root, query, cb) ->
                fromDate == null ? null : cb.greaterThanOrEqualTo(root.get("detectedAt"), fromDate);
    }

    public static Specification<FraudAlert> detectedBefore(LocalDateTime toDate) {
        return (root, query, cb) ->
                toDate == null ? null : cb.lessThanOrEqualTo(root.get("detectedAt"), toDate);
    }

    public static Specification<FraudAlert> amountAtLeast(BigDecimal amountMin) {
        return (root, query, cb) ->
                amountMin == null ? null : cb.greaterThanOrEqualTo(root.get("transactionAmount"), amountMin);
    }

    public static Specification<FraudAlert> amountAtMost(BigDecimal amountMax) {
        return (root, query, cb) ->
                amountMax == null ? null : cb.lessThanOrEqualTo(root.get("transactionAmount"), amountMax);
    }

    public static Specification<FraudAlert> hasAlertReferenceLike(String  alertReference) {
        return (root, query, cb) ->{
            if(alertReference == null || alertReference.trim().isEmpty()){
                return null;
            }
            return cb.like(cb.lower(root.get("alertReference")), "%" + alertReference.toLowerCase() + "%");
        };
    }
}
