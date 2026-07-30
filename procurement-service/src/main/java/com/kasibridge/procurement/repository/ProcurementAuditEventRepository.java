package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface ProcurementAuditEventRepository extends JpaRepository<ProcurementAuditEvent, Long> {
    Page<ProcurementAuditEvent> findByTenderId(Long tenderId, Pageable pageable);

    Page<ProcurementAuditEvent> findByBidId(Long bidId, Pageable pageable);

    Page<ProcurementAuditEvent> findByActorUserId(Long actorUserId, Pageable pageable);

    Optional<ProcurementAuditEvent> findTopByOrderByIdDesc();

    List<ProcurementAuditEvent> findAllByOrderByIdAsc();
}
