package com.kasibridge.fraud;


import com.kasibridge.fraud.dto.TransactionEvent;
import com.kasibridge.fraud.entity.FraudAlert;
import com.kasibridge.fraud.rules.BackdatedTransactionRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BackdatedTransactionRuleTest {

    private final BackdatedTransactionRule rule = new BackdatedTransactionRule();

    @Test
    void shouldDetectBackdatedTransaction(){

        //create event with large delay (> 60 mins)
        TransactionEvent event = new TransactionEvent(
                1L,
                1L,
                new BigDecimal("1000.00"),
                TransactionEvent.TransactionType.SALE,
                "CASH",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now()
        );

        Optional<FraudAlert> result = rule.evaluate(event);

        //assertions
        assertTrue(result.isPresent());

        FraudAlert alert = result.get();

        assertEquals(FraudAlert.FraudPatternType.BACKDATED_TRANSACTION, alert.getPatternType());
        assertEquals(FraudAlert.Severity.MEDIUM, alert.getSeverity());
        assertEquals(1L, alert.getTraderId());
        assertEquals(1L, alert.getTransactionId());
    }

    @Test
    void shouldNotDetectWhenWithinThreshold(){
        //delay < 60 mins
        TransactionEvent event = new TransactionEvent(
                2L,
                1L,
                new BigDecimal("500.00"),
                TransactionEvent.TransactionType.SALE,
                "CASH",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now()
        );

        Optional<FraudAlert> result = rule.evaluate(event);

        //assertions
        assertTrue(result.isEmpty());
    }

    @Test

    void shouldNotFailWhenTimestampsAreNull() {

        TransactionEvent event = new TransactionEvent(
                3L,
                1L,
                new BigDecimal("200.00"),
                TransactionEvent.TransactionType.SALE,
                "CASH",
                null,
                null
        );

        Optional<FraudAlert> result = rule.evaluate(event);

        assertTrue(result.isEmpty());
    }

}
