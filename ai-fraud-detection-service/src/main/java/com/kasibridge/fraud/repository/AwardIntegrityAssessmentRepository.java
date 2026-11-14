package com.kasibridge.fraud.repository;

import com.kasibridge.fraud.entity
        .AwardIntegrityAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository
        .JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AwardIntegrityAssessmentRepository extends JpaRepository<AwardIntegrityAssessment, Long> {

    Optional<AwardIntegrityAssessment> findByAssessmentReference(String assessmentReference);

    boolean existsByAssessmentReference(String assessmentReference);

    /*
     * Idempotency lookup.
     *
     * Repeating an assessment with the same
     * tender, rule version and evidence snapshot
     * returns the existing persisted record.
     */
    Optional<AwardIntegrityAssessment> findByTenderIdAndRuleVersionAndRequestSnapshotSha256(
            Long tenderId,
            String ruleVersion,
            String requestSnapshotSha256
    );

    /*
     * Complete assessment history for one tender,
     * including results produced by later rule
     * versions or changed evidence snapshots.
     */
    Page<AwardIntegrityAssessment> findByTenderId(Long tenderId, Pageable pageable);

    /*
     * Governance queue by review state.
     */
    Page<AwardIntegrityAssessment> findByReviewStatus(AwardIntegrityAssessment.ReviewStatus reviewStatus,
                                                      Pageable pageable);

    /*
     * Governance queue by risk level.
     */
    Page<AwardIntegrityAssessment> findByRiskLevel(AwardIntegrityAssessment.RiskLevel riskLevel, Pageable pageable);

    /*
     * Combined operational queue filter.
     */
    Page<AwardIntegrityAssessment> findByReviewStatusAndRiskLevel(AwardIntegrityAssessment.ReviewStatus reviewStatus,
                                                                  AwardIntegrityAssessment.RiskLevel riskLevel,
                                                                  Pageable pageable
    );

    /*
     * Retrieves the most recent assessment for
     * a tender without assuming that the tender
     * has only one assessment.
     */
    Optional<AwardIntegrityAssessment> findFirstByTenderIdOrderByAssessedAtDescIdDesc(Long tenderId);

    /*
     * Supports queue and dashboard counts.
     */
    long countByReviewStatus(AwardIntegrityAssessment.ReviewStatus reviewStatus);

    long countByRiskLevel(AwardIntegrityAssessment.RiskLevel riskLevel);

    long countByReviewStatusAndRiskLevel(AwardIntegrityAssessment.ReviewStatus reviewStatus,
                                         AwardIntegrityAssessment.RiskLevel riskLevel
    );
}