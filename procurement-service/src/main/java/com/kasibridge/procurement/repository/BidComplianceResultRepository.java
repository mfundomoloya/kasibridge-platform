package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.BidComplianceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BidComplianceResultRepository extends JpaRepository<BidComplianceResult, Long> {

    Optional<BidComplianceResult> findByBidId(Long bidId);
}
