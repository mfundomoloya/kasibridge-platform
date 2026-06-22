package com.kasibridge.fraud.rules;

import com.kasibridge.fraud.dto.TransactionEvent;
import com.kasibridge.fraud.entity.FraudAlert;
import com.kasibridge.fraud.dto.TransactionEvent.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HighValueTransactionRuleTest {

    private final HighValueTransactionRule rule = new HighValueTransactionRule();

    @Test
    void shouldDetectHighValueTransaction() {

        // ✅ amount > 50,000 → should trigger
        TransactionEvent event = new TransactionEvent(
                1L,
                1L,
                new BigDecimal("60000"),
                TransactionType.SALE,
                "CASH",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Optional<FraudAlert> result = rule.evaluate(event);

        assertTrue(result.isPresent());

        FraudAlert alert = result.get();

        assertEquals(FraudAlert.FraudPatternType.HIGH_VALUE_TRANSACTION, alert.getPatternType());
        assertEquals(FraudAlert.Severity.HIGH, alert.getSeverity());
        assertEquals(1L, alert.getTraderId());
        assertEquals(60000, alert.getTransactionAmount().intValue());
    }

    @Test
    void shouldNotDetectWhenBelowThreshold() {

        // ✅ amount < 50,000 → should NOT trigger
        TransactionEvent event = new TransactionEvent(
                2L,
                1L,
                new BigDecimal("49999"),
                TransactionType.SALE,
                "CASH",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Optional<FraudAlert> result = rule.evaluate(event);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotDetectWhenAmountEqualsThreshold() {

        // ✅ amount == 50,000 → should NOT trigger (only > threshold triggers)
        TransactionEvent event = new TransactionEvent(
                3L,
                1L,
                new BigDecimal("50000"),
                TransactionType.SALE,
                "CASH",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Optional<FraudAlert> result = rule.evaluate(event);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotFailWhenAmountIsNull() {

        TransactionEvent event = new TransactionEvent(
                4L,
                1L,
                null,
                TransactionType.SALE,
                "CASH",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Optional<FraudAlert> result = rule.evaluate(event);

        assertTrue(result.isEmpty());
    }
}