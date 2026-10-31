import type {
    FraudReviewStatus,
    FraudRiskLevel,
} from '../../types/fraudAssessment'

import {
    FRAUD_REVIEW_STATUS_LABELS,
    FRAUD_RISK_LEVEL_LABELS,
} from '../../types/fraudAssessment'

interface FraudReviewStatusBadgeProps {
    status: FraudReviewStatus
}

interface FraudRiskLevelBadgeProps {
    riskLevel: FraudRiskLevel
}

export function FraudReviewStatusBadge({
                                           status,
                                       }: FraudReviewStatusBadgeProps) {
    return (
        <span
            className={
                'fraud-badge '
                + getReviewStatusClassName(
                    status
                )
            }
        >
            {
                FRAUD_REVIEW_STATUS_LABELS[
                    status
                    ]
            }
        </span>
    )
}

export function FraudRiskLevelBadge({
                                        riskLevel,
                                    }: FraudRiskLevelBadgeProps) {
    return (
        <span
            className={
                'fraud-badge '
                + getRiskLevelClassName(
                    riskLevel
                )
            }
        >
            {
                FRAUD_RISK_LEVEL_LABELS[
                    riskLevel
                    ]
            }
        </span>
    )
}

function getReviewStatusClassName(
    status: FraudReviewStatus,
): string {
    switch (status) {
        case 'PENDING_REVIEW':
            return 'fraud-status-pending'

        case 'AUTO_CLEARED':
            return 'fraud-status-cleared'

        case 'CONFIRMED_FRAUD':
            return 'fraud-status-confirmed'

        case 'FALSE_POSITIVE':
            return 'fraud-status-false-positive'

        case 'ESCALATED':
            return 'fraud-status-escalated'
    }
}

function getRiskLevelClassName(
    riskLevel: FraudRiskLevel,
): string {
    switch (riskLevel) {
        case 'LOW':
            return 'fraud-risk-low'

        case 'MEDIUM':
            return 'fraud-risk-medium'

        case 'HIGH':
            return 'fraud-risk-high'

        case 'CRITICAL':
            return 'fraud-risk-critical'
    }
}