package com.kasibridge.fraud;

import com.kasibridge.fraud.dto.TransactionEvent;
import com.kasibridge.fraud.entity.FraudAlert;
import com.kasibridge.fraud.repository.FraudAlertRepository;
import com.kasibridge.fraud.rules.DuplicateTransactionRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DuplicateTransactionRuleTest {

    private final FraudAlertRepository repository = mock(FraudAlertRepository.class);
    private final DuplicateTransactionRule rule = new DuplicateTransactionRule(repository);

    @Test
    public void shouldDetectDuplicateWhenRecentAlertsExists(){

        TransactionEvent event = new TransactionEvent(
                1L,
                1L,
                new BigDecimal("1000"),
                TransactionEvent.TransactionType.SALE,
                "CASH",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // ✅ Simulate NO recent alerts
        when(repository.findRecentAlertsForTransaction(
                anyLong(),
                any(),
                anyLong(),
                any()
        )).thenReturn(List.of());

        Optional<FraudAlert> result = rule.evaluate(event);

        assertTrue(result.isPresent());

        FraudAlert alert = result.get();

        assertEquals(FraudAlert.FraudPatternType.DUPLICATE_TRANSACTION, alert.getPatternType());
        assertEquals(FraudAlert.Severity.MEDIUM, alert.getSeverity());
    }

    @Test
    void shouldNotCreateAlertWhenDuplicateAlreadyExists() {

        TransactionEvent event = new TransactionEvent(
                2L,
                1L,
                new BigDecimal("1000"),
                TransactionEvent.TransactionType.SALE,
                "CASH",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // ✅ Simulate existing duplicate alert
        when(repository.findRecentAlertsForTransaction(
                anyLong(),
                any(),
                anyLong(),
                any()
        )).thenReturn(List.of(mock(FraudAlert.class)));

        Optional<FraudAlert> result = rule.evaluate(event);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotFailWhenEventHasNullIds() {

        TransactionEvent event = new TransactionEvent(
                null,
                null,
                new BigDecimal("1000"),
                TransactionEvent.TransactionType.SALE,
                "CASH",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Optional<FraudAlert> result = rule.evaluate(event);

        assertTrue(result.isEmpty());
    }

}
