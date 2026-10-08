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

    List<Bid> findByTraderProfileIdOrderBySubmittedAtDesc(Long traderProfileId);

    long countByTenderId(Long tenderId);

    @Query("""
        SELECT CASE
            WHEN COUNT(b) > 0 THEN true
            ELSE false
        END
        FROM Bid b
        WHERE b.tenderId = :tenderId
          AND b.traderProfileId = :traderProfileId
          AND b.status <> :excludedStatus
        """)
    boolean existsActiveBidForTrader(
            @Param("tenderId") Long tenderId,
            @Param("traderProfileId") Long traderProfileId,
            @Param("excludedStatus") Bid.BidStatus excludedStatus
    );

    @Query("""
    SELECT MIN(b.priceAmount)
    FROM Bid b
    WHERE b.tenderId = :tenderId
      AND b.status IN ('COMPLIANT', 'UNDER_EVALUATION')
    """)
    BigDecimal findLowestBidPriceByTenderId(@Param("tenderId") Long tenderId);

    List<Bid> findByTenderIdAndStatusIn(Long tenderId, Collection<Bid.BidStatus> statuses);
}
