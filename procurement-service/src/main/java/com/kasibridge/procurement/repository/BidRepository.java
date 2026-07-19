package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByTenderId(Long tenderId);

    long countByTenderId(Long tenderId);

    boolean existsByTenderIdAndTraderId(Long tenderId, Long traderId);
}
