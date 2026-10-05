package com.kasibridge.procurement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.kasibridge.procurement.entity.Tender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenderRepository extends JpaRepository<Tender, Long> {

    Optional<Tender> findByTenderReference(String tenderReference);

    boolean existsByTenderReference(String tenderReference);

    Page<Tender> findByStatus(Tender.TenderStatus status, Pageable pageable);

    Optional<Tender> findByIdAndStatus(Long id, Tender.TenderStatus status);
}
