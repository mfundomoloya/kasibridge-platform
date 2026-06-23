package com.kasibridge.fraud.service;

import com.kasibridge.fraud.dto.TransactionEvent;
import com.kasibridge.fraud.entity.FraudAlert;
import com.kasibridge.fraud.repository.FraudAlertRepository;
import com.kasibridge.fraud.rules.FraudRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
@Slf4j
public class FraudDetectionService {

    private final List<FraudRule> rules;
    private final FraudAlertRepository alertRepository;

    private static final long DEDUP_LOOPBACK_DAYS = 1;

    public FraudDetectionService(List<FraudRule> rules, FraudAlertRepository alertRepository) {
        this.rules = rules;
        this.alertRepository = alertRepository;

        log.info("Fraud detection engine initialised with {} rules: {}",
                rules.size(),
                rules.stream().map(r -> r.getClass().getSimpleName()).toList());
    }

    @Transactional
    public List<FraudAlert> detect(TransactionEvent event) {

        log.info("Running fraud detection for transaction ID: {}", event.transactionId());

        List<FraudAlert> alerts = rules.stream()
                .map(rule -> evaluateRule(rule, event))
                .flatMap(Optional::stream)
                .filter(alert -> !alreadyAlerted(event, alert.getPatternType()))
                .map(this::saveSafely)
                .filter(Objects::nonNull)
                .toList();

        if (alerts.isEmpty()) {
            log.info("No new fraud alerts for transaction ID: {}", event.transactionId());
        } else {
            alerts.forEach(a -> log.info(
                    "Fraud alert created: {} [{}] for transaction ID: {}",
                    a.getAlertReference(),
                    a.getPatternType(),
                    event.transactionId()
            ));
        }

        return alerts;
    }


    private Optional<FraudAlert> evaluateRule(FraudRule rule, TransactionEvent event) {
        try {
            return rule.evaluate(event);
        } catch (Exception ex) {
            log.error("Rule {} failed for transaction ID {}: {}",
                    rule.getClass().getSimpleName(),
                    event.transactionId(),
                    ex.getMessage(),
                    ex);
            return Optional.empty();
        }
    }

    private boolean alreadyAlerted(TransactionEvent event, FraudAlert.FraudPatternType patternType) {

        // ✅ Null safety
        if (event.transactionId() == null || event.traderId() == null) {
            return false;
        }

        LocalDateTime since = LocalDateTime.now().minusDays(DEDUP_LOOPBACK_DAYS);

        List<FraudAlert> existing = alertRepository.findRecentAlertsForTransaction(
                event.traderId(),
                patternType,
                event.transactionId(),
                since
        );

        if (!existing.isEmpty()) {
            log.debug("Skipping duplicate alert — transaction ID: {} already has a {} alert",
                    event.transactionId(), patternType);
            return true;
        }

        return false;
    }

    private FraudAlert saveSafely(FraudAlert alert) {
        try {
            return alertRepository.save(alert);
        } catch (Exception ex) {
            log.error("Failed to persist alert for transaction ID {}: {}",
                    alert.getTransactionId(),
                    ex.getMessage(),
                    ex);
            return null;
        }
    }


}