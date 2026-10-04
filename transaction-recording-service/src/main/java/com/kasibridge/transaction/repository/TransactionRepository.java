package com.kasibridge.transaction.repository;

import com.kasibridge.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    //all transactions for a trader
    List<Transaction> findByTraderId(Long traderId);
    Page<Transaction> findByTraderId(Long traderId, Pageable pageable);

    //all transactions for a trader ordered by occurredAt descending
    Page<Transaction> findByTraderIdOrderByOccurredAtDesc(Long traderId, Pageable pageable);

    //find by reference number
    Optional<Transaction> findByReferenceNumber(String referenceNumber);


    //filter by type
    Page<Transaction> findByTraderIdAndType(Long traderId, Transaction.TransactionType type, Pageable pageable);

    //filter by status
    Page<Transaction> findByTraderIdAndStatus(Long traderId, Transaction.TransactionStatus status, Pageable pageable);

    //filter by channel
    Page<Transaction> findByTraderIdAndChannel(Long traderId, Transaction.TransactionChannel channel, Pageable pageable);

    //filter by payment method
    List<Transaction> findByTraderIdAndPaymentMethod(Long traderId, Transaction.PaymentMethod paymentMethod);

    //transactions for a trader between two dates
     Page<Transaction> findByTraderIdAndOccurredAtBetween(
            Long traderId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    // total sales for a trader

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.traderId = :traderId
          AND t.type = 'SALE'
          AND t.status = 'COMPLETED'
    """)

    BigDecimal getTotalSalesForTrader(@Param("traderId") Long traderId);

    //total expenses for a trader
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.traderId = :traderId
          AND t.type = 'EXPENSE'
          AND t.status = 'COMPLETED'
    """)

    BigDecimal getTotalExpensesForTrader(@Param("traderId") Long traderId);

    //count by status
    Long countByTraderIdAndStatus(Long traderId, Transaction.TransactionStatus status);

    //count by channel
    Long countByTraderIdAndChannel(Long traderId, Transaction.TransactionChannel channel);

    //count by payment method
    Long countByTraderIdAndPaymentMethod(Long traderId, Transaction.PaymentMethod paymentMethod);

    // fraud AI detection
    // All flagged transactions across all traders
    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    // Backdated transactions: occurredAt is significantly earlier than recordedAt
    // Used by AI fraud module to detect suspicious late entries


    /**
     * Backdated transactions:
     * recordedAt - occurredAt > thresholdMinutes
     * Portable JPQL using timestamp arithmetic (PostgreSQL-safe).
     */
    @Query("""
   SELECT t FROM Transaction t
       WHERE t.traderId = :traderId
       AND t.recordedAt IS NOT NULL
       AND t.occurredAt IS NOT NULL
       AND t.recordedAt > t.occurredAt
""")
    List<Transaction> findBackdatedTransactions(
            @Param("traderId") Long traderId
    );


     //Average capture delay (minutes) for credibility scoring.

    @Query("""
    SELECT t FROM Transaction t
    WHERE t.traderId = :traderId
    AND t.recordedAt IS NOT NULL
    AND t.occurredAt IS NOT NULL
""")

    Double getAverageCaptureDelayMinutes(@Param("traderId") Long traderId);
}
