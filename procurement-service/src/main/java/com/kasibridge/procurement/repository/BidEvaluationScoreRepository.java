package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.BidEvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidEvaluationScoreRepository extends JpaRepository<BidEvaluationScore, Long> {

    List<BidEvaluationScore> findByTenderId(Long tenderId);
    List<BidEvaluationScore> findByBidId(Long bidId);
    boolean existsByTenderIdAndBidIdAndEvaluatorUserId(Long tenderId, Long bidId, Long evaluatorUserId);

    @Query("""
       SELECT s.bidId,
                AVG(s.technicalScore),
                AVG(s.priceScore),
                AVG(s.totalScore),
                COUNT(s.id)
            FROM BidEvaluationScore s
            WHERE s.tenderId = :tenderId
            GROUP BY s.bidId
""")
    List<Object[]> findAverageScoresByTenderId(@Param("tenderId") Long tenderId);
}
