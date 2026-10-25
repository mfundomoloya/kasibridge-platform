package com.kasibridge.fraud.repository;

import com.kasibridge.fraud.entity.FraudAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FraudAssessmentRepository
        extends JpaRepository<
        FraudAssessment,
        Long
        > {

    Optional<FraudAssessment>
    findByAssessmentReference(
            String assessmentReference
    );

    Optional<FraudAssessment>
    findByInvoiceReferenceAndModelVersion(
            String invoiceReference,
            String modelVersion
    );

    boolean
    existsByInvoiceReferenceAndModelVersion(
            String invoiceReference,
            String modelVersion
    );

    Page<FraudAssessment>
    findByInvoiceReference(
            String invoiceReference,
            Pageable pageable
    );

    Page<FraudAssessment>
    findBySupplierId(
            String supplierId,
            Pageable pageable
    );

    Page<FraudAssessment>
    findByDepartmentId(
            String departmentId,
            Pageable pageable
    );

    Page<FraudAssessment>
    findByClassification(
            FraudAssessment.Classification
                    classification,
            Pageable pageable
    );

    Page<FraudAssessment>
    findByRiskLevel(
            FraudAssessment.RiskLevel
                    riskLevel,
            Pageable pageable
    );

    Page<FraudAssessment>
    findByReviewStatus(
            FraudAssessment.ReviewStatus
                    reviewStatus,
            Pageable pageable
    );

    Page<FraudAssessment>
    findByRecommendedAction(
            FraudAssessment.RecommendedAction
                    recommendedAction,
            Pageable pageable
    );

    Page<FraudAssessment>
    findBySupplierIdAndReviewStatus(
            String supplierId,
            FraudAssessment.ReviewStatus
                    reviewStatus,
            Pageable pageable
    );

    Page<FraudAssessment>
    findByReviewStatusAndRiskLevel(
            FraudAssessment.ReviewStatus
                    reviewStatus,
            FraudAssessment.RiskLevel
                    riskLevel,
            Pageable pageable
    );

    long countByReviewStatus(
            FraudAssessment.ReviewStatus
                    reviewStatus
    );

    long countByRiskLevel(
            FraudAssessment.RiskLevel
                    riskLevel
    );

    long countByClassification(
            FraudAssessment.Classification
                    classification
    );
}