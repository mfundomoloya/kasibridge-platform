package com.kasibridge.fraud.rules;

import com.kasibridge.fraud.dto.TransactionEvent;
import com.kasibridge.fraud.entity.FraudAlert;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class BackdatedTransactionRule implements FraudRule{
    private static final long THRESHOLD_MINUTES = 60;

    @Override
    public Optional<FraudAlert> evaluate(TransactionEvent event){

        //this is for timestamps - guard clause
        if(event.occurredAt() == null || event.recordedAt() == null){
            return Optional.empty();
        }

        //delay between occurred and recorded times
        long delayMinutes = Duration.between(event.occurredAt(), event.recordedAt()).toMinutes();

        //check rule condition
        if(delayMinutes >  THRESHOLD_MINUTES){

            //build evidence
            String evidence = String.format(
                    "{\"delayMinutes\": %d, \"thresholdMinutes\": %d}",
                    delayMinutes,
                    THRESHOLD_MINUTES
            );

            //create FraudAlert using the factory method
            FraudAlert alert = FraudAlert.create(
                    event.traderId(),
                    event.transactionId(),
                    getPatternType(),
                    FraudAlert.Severity.MEDIUM,
                    "Transaction recorded significantly later than it occurred",
                    evidence,
                    event.amount()
            );
            return Optional.of(alert);
        }
        return Optional.empty();
    }

    @Override
    public FraudAlert.FraudPatternType getPatternType() {
        return FraudAlert.FraudPatternType.BACKDATED_TRANSACTION;
    }
}
