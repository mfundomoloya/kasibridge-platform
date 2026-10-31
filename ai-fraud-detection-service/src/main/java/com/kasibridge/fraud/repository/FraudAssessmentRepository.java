package com.kasibridge.fraud.repository;

import com.kasibridge.fraud.entity.FraudAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, Long>, JpaSpecificationExecutor<FraudAssessment> {

    Optional<FraudAssessment> findByAssessmentReference(String assessmentReference);

    Optional<FraudAssessment> findByInvoiceReferenceAndModelVersion(String invoiceReference, String modelVersion);

    boolean existsByInvoiceReferenceAndModelVersion(String invoiceReference, String modelVersion);

    Page<FraudAssessment> findByInvoiceReference(String invoiceReference, Pageable pageable);

    Page<FraudAssessment> findBySupplierId(String supplierId, Pageable pageable);

    Page<FraudAssessment> findByDepartmentId(String departmentId, Pageable pageable);

    Page<FraudAssessment> findByClassification(FraudAssessment.Classification classification, Pageable pageable);

    Page<FraudAssessment> findByRiskLevel(FraudAssessment.RiskLevel riskLevel, Pageable pageable);

    Page<FraudAssessment> findByReviewStatus(FraudAssessment.ReviewStatus reviewStatus, Pageable pageable);

    Page<FraudAssessment> findByRecommendedAction(FraudAssessment.RecommendedAction recommendedAction, Pageable pageable);

    Page<FraudAssessment> findBySupplierIdAndReviewStatus(String supplierId, FraudAssessment.ReviewStatus reviewStatus,
                                                          Pageable pageable);

    Page<FraudAssessment> findByReviewStatusAndRiskLevel(FraudAssessment.ReviewStatus reviewStatus,
                                                         FraudAssessment.RiskLevel riskLevel, Pageable pageable);

    long countByReviewStatus(FraudAssessment.ReviewStatus reviewStatus);

    long countByRiskLevel(FraudAssessment.RiskLevel riskLevel);

    long countByClassification(FraudAssessment.Classification classification);

    @Query(value =
                    """
                    SELECT
                        COUNT(*) AS "totalAssessments",
    
                        COUNT(*) FILTER (
                            WHERE review_status =
                                'PENDING_REVIEW'
                        ) AS "pendingReview",
    
                        COUNT(*) FILTER (
                            WHERE review_status =
                                'ESCALATED'
                        ) AS "escalated",
    
                        COUNT(*) FILTER (
                            WHERE review_status =
                                'CONFIRMED_FRAUD'
                        ) AS "confirmedFraud",
    
                        COUNT(*) FILTER (
                            WHERE review_status =
                                'FALSE_POSITIVE'
                        ) AS "falsePositive",
    
                        COUNT(*) FILTER (
                            WHERE review_status =
                                'AUTO_CLEARED'
                        ) AS "autoCleared",
    
                        COUNT(*) FILTER (
                            WHERE risk_level =
                                'CRITICAL'
                        ) AS "criticalRisk",
    
                        COUNT(*) FILTER (
                            WHERE risk_level =
                                'HIGH'
                        ) AS "highRisk",
    
                        COUNT(*) FILTER (
                            WHERE risk_level IN (
                                'HIGH',
                                'CRITICAL'
                            )
                            AND review_status IN (
                                'PENDING_REVIEW',
                                'ESCALATED'
                            )
                        ) AS "openHighOrCriticalRisk",
    
                        MIN(assessed_at) FILTER (
                            WHERE review_status =
                                'PENDING_REVIEW'
                        ) AS "oldestPendingAssessedAt",
    
                        MAX(assessed_at)
                            AS "latestAssessedAt"
    
                    FROM fraud.fraud_assessments
                    """,
            nativeQuery = true
    )
    FraudAssessmentDashboardProjection
    getDashboardSummary();
}