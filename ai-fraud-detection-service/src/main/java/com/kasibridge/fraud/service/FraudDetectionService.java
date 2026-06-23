package com.kasibridge.fraud.service;

import com.kasibridge.fraud.dto.FraudDashboardSummary;
import com.kasibridge.fraud.dto.TransactionEvent;
import com.kasibridge.fraud.entity.FraudAlert;
import com.kasibridge.fraud.exception.AlertNotFoundException;
import com.kasibridge.fraud.repository.FraudAlertRepository;
import com.kasibridge.fraud.rules.FraudRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    //query methods
    public FraudAlert getAlertById(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Fraud alert not found with ID: " + id));
    }

    public FraudAlert getAlertByReference(String alertReference) {
        return alertRepository.findByAlertReference(alertReference)
                .orElseThrow(() ->
                        new AlertNotFoundException("Fraud alert not found with reference: " + alertReference)
                );
    }

    public Page<FraudAlert> getAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }

    public Page<FraudAlert> getAlertsByTrader(Long traderId, Pageable pageable) {
        return alertRepository.findByTraderId(traderId, pageable);
    }

    public Page<FraudAlert> getAlertsByStatus(FraudAlert.AlertStatus status, Pageable pageable) {
        return alertRepository.findByStatus(status, pageable);
    }

    public Page<FraudAlert> getAlertsByPatternType(
            FraudAlert.FraudPatternType patternType,
            Pageable pageable
    ) {
        return alertRepository.findByPatternType(patternType, pageable);
    }

    public Page<FraudAlert> getAlertsByTraderAndStatus(
            Long traderId,
            FraudAlert.AlertStatus status,
            Pageable pageable
    ) {
        return alertRepository.findByTraderIdAndStatus(traderId, status, pageable);
    }

    //action methods

    @Transactional
    public FraudAlert reviewAlert(Long id, String reviewedBy, String reviewNotes) {
        FraudAlert alert = getAlertById(id);
        alert.review(reviewedBy, reviewNotes);
        return alertRepository.save(alert);
    }

    @Transactional
    public FraudAlert dismissAlert(Long id, String reviewedBy, String reviewNotes) {
        FraudAlert alert = getAlertById(id);
        alert.dismiss(reviewedBy, reviewNotes);
        return alertRepository.save(alert);
    }

    @Transactional
    public FraudAlert escalateAlert(Long id) {
        FraudAlert alert = getAlertById(id);
        alert.escalate();
        return alertRepository.save(alert);
    }

    public FraudDashboardSummary getDashboardSummary() {

        return FraudDashboardSummary.builder()
                .totalAlerts(alertRepository.count())

                .openAlerts(alertRepository.countByStatus(FraudAlert.AlertStatus.OPEN))
                .reviewedAlerts(alertRepository.countByStatus(FraudAlert.AlertStatus.REVIEWED))
                .dismissedAlerts(alertRepository.countByStatus(FraudAlert.AlertStatus.DISMISSED))
                .escalatedAlerts(alertRepository.countByStatus(FraudAlert.AlertStatus.ESCALATED))

                .lowSeverityAlerts(alertRepository.countBySeverity(FraudAlert.Severity.LOW))
                .mediumSeverityAlerts(alertRepository.countBySeverity(FraudAlert.Severity.MEDIUM))
                .highSeverityAlerts(alertRepository.countBySeverity(FraudAlert.Severity.HIGH))

                .backdatedTransactionAlerts(
                        alertRepository.countByPatternType(FraudAlert.FraudPatternType.BACKDATED_TRANSACTION)
                )
                .highValueTransactionAlerts(
                        alertRepository.countByPatternType(FraudAlert.FraudPatternType.HIGH_VALUE_TRANSACTION)
                )
                .duplicateTransactionAlerts(
                        alertRepository.countByPatternType(FraudAlert.FraudPatternType.DUPLICATE_TRANSACTION)
                )
                .build();
    }
}