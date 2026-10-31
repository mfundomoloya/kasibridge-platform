package com.kasibridge.fraud.repository;

import com.kasibridge.fraud.dto.FraudAssessmentSearchCriteria;
import com.kasibridge.fraud.entity.FraudAssessment;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class FraudAssessmentSpecification {

    private FraudAssessmentSpecification() {
    }

    public static Specification<FraudAssessment>
    withCriteria(
            FraudAssessmentSearchCriteria criteria
    ) {
        return (
                root,
                query,
                builder
        ) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            if (criteria != null) {
                addReviewStatusPredicate(
                        criteria,
                        predicates,
                        root,
                        builder
                );

                addRiskLevelPredicate(
                        criteria,
                        predicates,
                        root,
                        builder
                );

                addClassificationPredicate(
                        criteria,
                        predicates,
                        root,
                        builder
                );

                addSupplierPredicate(
                        criteria,
                        predicates,
                        root,
                        builder
                );

                addDepartmentPredicate(
                        criteria,
                        predicates,
                        root,
                        builder
                );

                addModelVersionPredicate(
                        criteria,
                        predicates,
                        root,
                        builder
                );

                addAssessmentTimePredicates(
                        criteria,
                        predicates,
                        root,
                        builder
                );
            }

            /*
             * Count queries used by pagination do not
             * require ordering. Applying order clauses
             * to a count query can cause SQL errors on
             * some JPA providers and database engines.
             */
            if (
                    !isCountQuery(
                            query.getResultType()
                    )
            ) {
                applyDefaultQueueOrdering(
                        query,
                        root,
                        builder
                );
            }

            return builder.and(
                    predicates.toArray(
                            Predicate[]::new
                    )
            );
        };
    }

    private static void addReviewStatusPredicate(
            FraudAssessmentSearchCriteria criteria,
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<
                    FraudAssessment
                    > root,
            jakarta.persistence.criteria.CriteriaBuilder
                    builder
    ) {
        if (criteria.reviewStatus() != null) {
            predicates.add(
                    builder.equal(
                            root.get(
                                    "reviewStatus"
                            ),
                            criteria.reviewStatus()
                    )
            );
        }
    }

    private static void addRiskLevelPredicate(
            FraudAssessmentSearchCriteria criteria,
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<
                    FraudAssessment
                    > root,
            jakarta.persistence.criteria.CriteriaBuilder
                    builder
    ) {
        if (criteria.riskLevel() != null) {
            predicates.add(
                    builder.equal(
                            root.get(
                                    "riskLevel"
                            ),
                            criteria.riskLevel()
                    )
            );
        }
    }

    private static void addClassificationPredicate(
            FraudAssessmentSearchCriteria criteria,
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<
                    FraudAssessment
                    > root,
            jakarta.persistence.criteria.CriteriaBuilder
                    builder
    ) {
        if (criteria.classification() != null) {
            predicates.add(
                    builder.equal(
                            root.get(
                                    "classification"
                            ),
                            criteria.classification()
                    )
            );
        }
    }

    private static void addSupplierPredicate(
            FraudAssessmentSearchCriteria criteria,
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<
                    FraudAssessment
                    > root,
            jakarta.persistence.criteria.CriteriaBuilder
                    builder
    ) {
        if (criteria.supplierId() != null) {
            predicates.add(
                    builder.equal(
                            builder.lower(
                                    root.get(
                                            "supplierId"
                                    )
                            ),
                            criteria
                                    .supplierId()
                                    .toLowerCase(
                                            java.util.Locale.ROOT
                                    )
                    )
            );
        }
    }

    private static void addDepartmentPredicate(
            FraudAssessmentSearchCriteria criteria,
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<
                    FraudAssessment
                    > root,
            jakarta.persistence.criteria.CriteriaBuilder
                    builder
    ) {
        if (criteria.departmentId() != null) {
            predicates.add(
                    builder.equal(
                            builder.lower(
                                    root.get(
                                            "departmentId"
                                    )
                            ),
                            criteria
                                    .departmentId()
                                    .toLowerCase(
                                            java.util.Locale.ROOT
                                    )
                    )
            );
        }
    }

    private static void addModelVersionPredicate(
            FraudAssessmentSearchCriteria criteria,
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<
                    FraudAssessment
                    > root,
            jakarta.persistence.criteria.CriteriaBuilder
                    builder
    ) {
        if (criteria.modelVersion() != null) {
            predicates.add(
                    builder.equal(
                            builder.lower(
                                    root.get(
                                            "modelVersion"
                                    )
                            ),
                            criteria
                                    .modelVersion()
                                    .toLowerCase(
                                            java.util.Locale.ROOT
                                    )
                    )
            );
        }
    }

    private static void addAssessmentTimePredicates(
            FraudAssessmentSearchCriteria criteria,
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<
                    FraudAssessment
                    > root,
            jakarta.persistence.criteria.CriteriaBuilder
                    builder
    ) {
        if (criteria.assessedFrom() != null) {
            predicates.add(
                    builder.greaterThanOrEqualTo(
                            root.get(
                                    "assessedAt"
                            ),
                            criteria.assessedFrom()
                    )
            );
        }

        if (criteria.assessedTo() != null) {
            predicates.add(
                    builder.lessThanOrEqualTo(
                            root.get(
                                    "assessedAt"
                            ),
                            criteria.assessedTo()
                    )
            );
        }
    }

    private static void applyDefaultQueueOrdering(
            jakarta.persistence.criteria.CriteriaQuery<?>
                    query,
            jakarta.persistence.criteria.Root<
                    FraudAssessment
                    > root,
            jakarta.persistence.criteria.CriteriaBuilder
                    builder
    ) {
        Expression<Integer> reviewPriority =
                builder.<Integer>selectCase()
                        .when(
                                builder.equal(
                                        root.get(
                                                "reviewStatus"
                                        ),
                                        FraudAssessment
                                                .ReviewStatus
                                                .PENDING_REVIEW
                                ),
                                0
                        )
                        .when(
                                builder.equal(
                                        root.get(
                                                "reviewStatus"
                                        ),
                                        FraudAssessment
                                                .ReviewStatus
                                                .ESCALATED
                                ),
                                1
                        )
                        .when(
                                builder.equal(
                                        root.get(
                                                "reviewStatus"
                                        ),
                                        FraudAssessment
                                                .ReviewStatus
                                                .AUTO_CLEARED
                                ),
                                2
                        )
                        .when(
                                builder.equal(
                                        root.get(
                                                "reviewStatus"
                                        ),
                                        FraudAssessment
                                                .ReviewStatus
                                                .FALSE_POSITIVE
                                ),
                                3
                        )
                        .when(
                                builder.equal(
                                        root.get(
                                                "reviewStatus"
                                        ),
                                        FraudAssessment
                                                .ReviewStatus
                                                .CONFIRMED_FRAUD
                                ),
                                4
                        )
                        .otherwise(5);

        Expression<Integer> riskPriority =
                builder.<Integer>selectCase()
                        .when(
                                builder.equal(
                                        root.get(
                                                "riskLevel"
                                        ),
                                        FraudAssessment
                                                .RiskLevel
                                                .CRITICAL
                                ),
                                0
                        )
                        .when(
                                builder.equal(
                                        root.get(
                                                "riskLevel"
                                        ),
                                        FraudAssessment
                                                .RiskLevel
                                                .HIGH
                                ),
                                1
                        )
                        .when(
                                builder.equal(
                                        root.get(
                                                "riskLevel"
                                        ),
                                        FraudAssessment
                                                .RiskLevel
                                                .MEDIUM
                                ),
                                2
                        )
                        .when(
                                builder.equal(
                                        root.get(
                                                "riskLevel"
                                        ),
                                        FraudAssessment
                                                .RiskLevel
                                                .LOW
                                ),
                                3
                        )
                        .otherwise(4);

        List<Order> queueOrdering =
                List.of(
                        builder.asc(
                                reviewPriority
                        ),
                        builder.asc(
                                riskPriority
                        ),
                        builder.asc(
                                root.get(
                                        "assessedAt"
                                )
                        ),
                        builder.asc(
                                root.get(
                                        "id"
                                )
                        )
                );

        query.orderBy(
                queueOrdering
        );
    }

    private static boolean isCountQuery(
            Class<?> resultType
    ) {
        return resultType == Long.class
                || resultType == long.class;
    }
}