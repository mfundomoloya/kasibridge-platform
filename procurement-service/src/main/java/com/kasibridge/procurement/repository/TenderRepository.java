package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.Tender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenderRepository extends JpaRepository<Tender, Long> {

    Optional<Tender> findByTenderReference(String tenderReference);

    boolean existsByTenderReference(String tenderReference);

}
