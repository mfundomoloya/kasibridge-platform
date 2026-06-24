package com.kasibridge.fraud.repository;

import com.kasibridge.fraud.entity.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long>, JpaSpecificationExecutor<FraudAlert> {

    //core
    Optional<FraudAlert> findByAlertReference(String alertReference);

    //by trader
    Page<FraudAlert> findByTraderId(Long traderId, Pageable pageable);

    List<FraudAlert> findByTraderId(Long traderId);

    Page<FraudAlert> findByTraderIdAndStatus(Long traderId, FraudAlert.AlertStatus status, Pageable pageable);

    //by status
    Page<FraudAlert> findByStatus(FraudAlert.AlertStatus status, Pageable pageable);

    //by pattern
    Page<FraudAlert> findByPatternType(FraudAlert.FraudPatternType patternType, Pageable pageable);

    //by severity
    Page<FraudAlert> findBySeverity(FraudAlert.Severity severity, Pageable pageable);

    //by transaction
    List<FraudAlert> findByTransactionId(Long transactionId);

    //date range
    Page<FraudAlert> findByDetectedAtBetween(
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    //Counts (for dashboard)
    long countByStatus(FraudAlert.AlertStatus status);

    long countByTraderIdAndStatus(
            Long traderId,
            FraudAlert.AlertStatus status
    );

    long countByPatternType(FraudAlert.FraudPatternType patternType);

    long countBySeverity(FraudAlert.Severity severity);

    Page<FraudAlert> findByTraderIdAndSeverity(
            Long traderId,
            FraudAlert.Severity severity,
            Pageable pageable
    );


    Page<FraudAlert> findByTraderIdAndStatusNot(
            Long traderId,
            FraudAlert.AlertStatus status,
            Pageable pageable
    );

    //High Risk Traders
    @Query("""
        SELECT f.traderId FROM FraudAlert f
        WHERE f.status = 'OPEN'
        GROUP BY f.traderId
        HAVING COUNT(f.id) >= :threshold
    """)
    List<Long> findHighRiskTraderIds(
            @Param("status") FraudAlert.AlertStatus status,
            @Param("threshold") long threshold);

    //Duplicate Alert Prevention
    // Prevents creating duplicate alerts for same transaction + pattern
    @Query("""
        SELECT f FROM FraudAlert f
        WHERE f.traderId = :traderId
        AND f.patternType = :patternType
        AND f.transactionId = :transactionId
        AND f.detectedAt >= :since
    """)

    List<FraudAlert> findRecentAlertsForTransaction(
            @Param("traderId") Long traderId,
            @Param("patternType") FraudAlert.FraudPatternType patternType,
            @Param("transactionId") Long transactionId,
            @Param("since") LocalDateTime since
    );

    boolean existsByTransactionIdAndPatternType(Long transactionId, FraudAlert.FraudPatternType patternType);
}
