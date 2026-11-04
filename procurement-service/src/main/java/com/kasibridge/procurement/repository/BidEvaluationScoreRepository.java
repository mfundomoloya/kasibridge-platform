package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.BidEvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidEvaluationScoreRepository
        extends JpaRepository<
        BidEvaluationScore,
        Long
        > {

    List<BidEvaluationScore>
    findByTenderId(
            Long tenderId
    );

    List<BidEvaluationScore>
    findByBidId(
            Long bidId
    );

    List<BidEvaluationScore>
    findByTenderIdAndEvaluatorUserIdOrderByBidIdAsc(
            Long tenderId,
            Long evaluatorUserId
    );

    boolean existsByTenderId(
            Long tenderId
    );

    boolean
    existsByTenderIdAndBidIdAndEvaluatorUserId(
            Long tenderId,
            Long bidId,
            Long evaluatorUserId
    );

    boolean
    existsByBidIdAndEvaluatorUserId(
            Long bidId,
            Long evaluatorUserId
    );

    boolean
    existsByTenderIdAndBidId(
            Long tenderId,
            Long bidId
    );

    /*
     * Counts all submitted evaluation scores for
     * one tender.
     *
     * The database unique constraint prevents one
     * evaluator from scoring the same bid twice.
     */
    long countByTenderId(
            Long tenderId
    );

    /*
     * Counts unique evaluator and bid combinations.
     *
     * This query remains defensive even though the
     * database applies a unique constraint to
     * bid_id and evaluator_user_id.
     */
    @Query(
            """
            SELECT COUNT(s.id)
            FROM BidEvaluationScore s
            WHERE s.tenderId = :tenderId
            """
    )
    long countSubmittedScoresByTenderId(
            @Param("tenderId")
            Long tenderId
    );

    /*
     * Counts the number of different evaluators who
     * submitted at least one score for the tender.
     */
    @Query(
            """
            SELECT COUNT(
                DISTINCT s.evaluatorUserId
            )
            FROM BidEvaluationScore s
            WHERE s.tenderId = :tenderId
            """
    )
    long countDistinctEvaluatorsByTenderId(
            @Param("tenderId")
            Long tenderId
    );

    /*
     * Counts the number of different bids that have
     * received at least one evaluation.
     */
    @Query(
            """
            SELECT COUNT(
                DISTINCT s.bidId
            )
            FROM BidEvaluationScore s
            WHERE s.tenderId = :tenderId
            """
    )
    long countDistinctScoredBidsByTenderId(
            @Param("tenderId")
            Long tenderId
    );

    /*
     * Returns average scores per bid for the later
     * adjudication ranking.
     *
     * Object array positions:
     * 0 = bidId
     * 1 = average technical score
     * 2 = average price score
     * 3 = average total score
     * 4 = submitted evaluator score count
     */
    @Query(
            """
            SELECT
                s.bidId,
                AVG(s.technicalScore),
                AVG(s.priceScore),
                AVG(s.totalScore),
                COUNT(s.id)
            FROM BidEvaluationScore s
            WHERE s.tenderId = :tenderId
            GROUP BY s.bidId
            """
    )
    List<Object[]>
    findAverageScoresByTenderId(
            @Param("tenderId")
            Long tenderId
    );
}