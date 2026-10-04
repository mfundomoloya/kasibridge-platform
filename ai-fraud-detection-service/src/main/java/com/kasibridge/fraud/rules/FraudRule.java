package com.kasibridge.fraud.rules;

import com.kasibridge.fraud.dto.TransactionEvent;
import com.kasibridge.fraud.entity.FraudAlert;

import java.util.Optional;

/**
 * Contract for a single fraud detection rule.
 *
 * Each rule inspects a TransactionEvent (data pulled from the
 * Transaction Recording Service) and decides whether it represents
 * a suspicious pattern.
 *
 * New rules can be added later (e.g. AI-powered anomaly detection)
 * by simply implementing this interface — no existing code changes.
 */

public interface FraudRule {
    Optional<FraudAlert> evaluate(TransactionEvent event);

    //this will identify which pattern this rule detects
    FraudAlert.FraudPatternType getPatternType();
}
