package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ProcurementAuditEventRepository extends JpaRepository<ProcurementAuditEvent, Long> {

    /*
     * Serializes procurement audit-chain appends across
     * application instances and database sessions.
     *
     * The lock is transaction-scoped and is released
     * automatically when the current transaction commits
     * or rolls back.
     */
    @Query(
            value = """
                    SELECT pg_advisory_xact_lock(
                        CAST(:lockKey AS BIGINT)
                    )
                    """,
            nativeQuery = true
    )
    void acquireAuditChainLock(
            @Param("lockKey")
            long lockKey
    );

    Page<ProcurementAuditEvent> findByTenderId(Long tenderId, Pageable pageable);

    Page<ProcurementAuditEvent> findByBidId(Long bidId, Pageable pageable);

    Page<ProcurementAuditEvent> findByActorUserId(Long actorUserId, Pageable pageable);

    Optional<ProcurementAuditEvent> findTopByOrderByIdDesc();

    List<ProcurementAuditEvent> findAllByOrderByIdAsc();
}
