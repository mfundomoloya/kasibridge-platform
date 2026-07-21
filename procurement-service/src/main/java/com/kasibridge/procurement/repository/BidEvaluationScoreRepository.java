package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.BidEvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidEvaluationScoreRepository extends JpaRepository<BidEvaluationScore, Long> {

    List<BidEvaluationScore> findByTenderId(Long tenderId);
    List<BidEvaluationScore> findByBidId(Long bidId);
    boolean existsByTenderIdAndBidIdAndEvaluatorUserId(Long tenderId, Long bidId, Long evaluatorUserId);
}
