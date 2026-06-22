package com.kasibridge.fraud.rules;

import com.kasibridge.fraud.dto.TransactionEvent;
import com.kasibridge.fraud.entity.FraudAlert;
import com.kasibridge.fraud.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DuplicateTransactionRule implements FraudRule {

    private final FraudAlertRepository fraudAlertRepository;

    private static final int WINDOW_MINUTES = 5;

    @Override
    public Optional<FraudAlert> evaluate(TransactionEvent event) {

        if (event.transactionId() == null || event.traderId() == null) {
            return Optional.empty();
        }

        LocalDateTime since = event.occurredAt().minusMinutes(WINDOW_MINUTES);

        //check if similar alert already exists
        List<FraudAlert> recentAlerts =
                fraudAlertRepository.findRecentAlertsForTransaction(
                        event.traderId(),
                        getPatternType(),
                        event.transactionId(),
                        since
                );

        if(!recentAlerts.isEmpty()) {
            //already flagged - avoid duplicate alert
            return Optional.empty();
        }


        // for v1: we simulate duplicate detection
        // when microservices are connected, this would check actual transactions (via API/event store)

        // for now: trigger if same transaction comes again within window

        String evidence = String.format(
                "{\"windowMinutes\": %d, \"note\": \"Potential duplicate transaction pattern\"}",
                WINDOW_MINUTES
        );

        FraudAlert alert = FraudAlert.create(
                event.traderId(),
                event.transactionId(),
                getPatternType(),
                FraudAlert.Severity.MEDIUM,
                "Possible duplicate transaction detected within short time window",
                evidence,
                event.amount()
        );

        return Optional.of(alert);
    }

    @Override
    public FraudAlert.FraudPatternType getPatternType() {
        return FraudAlert.FraudPatternType.DUPLICATE_TRANSACTION;
    };

}
