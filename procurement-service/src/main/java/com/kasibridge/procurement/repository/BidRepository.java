package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByTenderId(Long tenderId);

    long countByTenderId(Long tenderId);

    boolean existsByTenderIdAndTraderProfileId(Long tenderId, Long traderProfileId);


    @Query("""
    SELECT MIN(b.priceAmount)
    FROM Bid b
    WHERE b.tenderId = :tenderId
      AND b.status IN ('COMPLIANT', 'UNDER_EVALUATION')
    """)
    BigDecimal findLowestBidPriceByTenderId(@Param("tenderId") Long tenderId);

    List<Bid> findByTenderIdAndStatusIn(Long tenderId, Collection<Bid.BidStatus> statuses);
}
