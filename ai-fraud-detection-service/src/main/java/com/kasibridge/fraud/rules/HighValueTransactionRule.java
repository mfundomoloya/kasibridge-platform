package com.kasibridge.fraud.rules;

import com.kasibridge.fraud.dto.TransactionEvent;
import com.kasibridge.fraud.entity.FraudAlert;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class HighValueTransactionRule implements FraudRule {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("50000");

    @Override
    public Optional<FraudAlert> evaluate(TransactionEvent event) {
        //amount must exist
        if(event.amount() == null){
            return Optional.empty();
        }

        //check the rule condition
        if(event.amount().compareTo(HIGH_VALUE_THRESHOLD) > 0){
            //evidence payload
            String evidence = String.format(
                    "{\"amount\": %s, \"threshold\": %s}",
                    event.amount(),
                    HIGH_VALUE_THRESHOLD
            );

            //create FraudAlert using the factory method
            FraudAlert alert = FraudAlert.create(
                    event.traderId(),
                    event.transactionId(),
                    getPatternType(),
                    FraudAlert.Severity.HIGH,
                    "High value transaction detected",
                    evidence,
                    event.amount()
            );

            return Optional.of(alert);
        }

        return Optional.empty();
    }

    @Override
    public FraudAlert.FraudPatternType getPatternType() {
        return FraudAlert.FraudPatternType.HIGH_VALUE_TRANSACTION;
    }
}
