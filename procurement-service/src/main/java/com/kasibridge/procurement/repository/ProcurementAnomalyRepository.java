package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.ProcurementAnomaly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementAnomalyRepository extends JpaRepository<ProcurementAnomaly, Long> {

    Page<ProcurementAnomaly> findByTenderId(Long tenderId, Pageable pageable);

    Page<ProcurementAnomaly> findByStatus(
            ProcurementAnomaly.AnomalyStatus status,
            Pageable pageable
    );

    boolean existsByTenderIdAndTypeAndEvidence(
            Long tenderId,
            ProcurementAnomaly.AnomalyType type,
            String evidence
    );
}