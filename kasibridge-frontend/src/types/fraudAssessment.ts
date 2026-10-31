export type FraudClassification =
    | 'LEGITIMATE'
    | 'HIGH_RISK'

export type FraudRiskLevel =
    | 'LOW'
    | 'MEDIUM'
    | 'HIGH'
    | 'CRITICAL'

export type FraudReviewStatus =
    | 'PENDING_REVIEW'
    | 'AUTO_CLEARED'
    | 'CONFIRMED_FRAUD'
    | 'FALSE_POSITIVE'
    | 'ESCALATED'

export type FraudRecommendedAction =
    | 'ALLOW_WITH_MONITORING'
    | 'REQUIRE_REVIEW'
    | 'HOLD_PAYMENT_AND_REVIEW'
    | 'BLOCK_AND_ESCALATE'

export type FraudDetectionSource =
    | 'SUPERVISED_MODEL'
    | 'DETERMINISTIC_RULE'
    | 'HYBRID'

export interface FraudAssessment {
    id: number
    assessmentReference: string
    invoiceReference: string
    supplierId: string | null
    departmentId: string | null
    invoiceAmount: number

    supplierCountry: string | null
    region: string | null

    classification: FraudClassification
    riskLevel: FraudRiskLevel
    riskProbability: number
    threshold: number
    recommendedAction: FraudRecommendedAction

    modelName: string
    modelVersion: string
    experiment: string
    detectionSource: FraudDetectionSource
    modelSha256: string
    featureSnapshotSha256: string

    reviewStatus: FraudReviewStatus
    reviewedBy: string | null
    reviewNotes: string | null
    reviewedAt: string | null

    assessedAt: string
    createdAt: string
    updatedAt: string | null
    version: number
}

export interface FraudAssessmentDashboard {
    totalAssessments: number
    pendingReview: number
    escalated: number
    confirmedFraud: number
    falsePositive: number
    autoCleared: number
    criticalRisk: number
    highRisk: number
    openHighOrCriticalRisk: number
    oldestPendingAssessedAt: string | null
    oldestPendingAgeHours: number | null
    latestAssessedAt: string | null
    generatedAt: string
}

export interface FraudAssessmentPageDetails {
    size: number
    number: number
    totalElements: number
    totalPages: number
}

export interface FraudAssessmentPage {
    content: FraudAssessment[]
    page: FraudAssessmentPageDetails
}

export interface FraudAssessmentQuery {
    reviewStatus?: FraudReviewStatus
    riskLevel?: FraudRiskLevel
    classification?: FraudClassification
    supplierId?: string
    departmentId?: string
    modelVersion?: string
    assessedFrom?: string
    assessedTo?: string
    page?: number
    size?: number
    sort?: FraudAssessmentSort
}

export type FraudAssessmentSort =
    | 'assessmentReference,asc'
    | 'assessmentReference,desc'
    | 'invoiceReference,asc'
    | 'invoiceReference,desc'
    | 'supplierId,asc'
    | 'supplierId,desc'
    | 'departmentId,asc'
    | 'departmentId,desc'
    | 'invoiceAmount,asc'
    | 'invoiceAmount,desc'
    | 'classification,asc'
    | 'classification,desc'
    | 'riskLevel,asc'
    | 'riskLevel,desc'
    | 'riskProbability,asc'
    | 'riskProbability,desc'
    | 'reviewStatus,asc'
    | 'reviewStatus,desc'
    | 'modelVersion,asc'
    | 'modelVersion,desc'
    | 'assessedAt,asc'
    | 'assessedAt,desc'
    | 'createdAt,asc'
    | 'createdAt,desc'
    | 'updatedAt,asc'
    | 'updatedAt,desc'

export type FraudReviewDecision =
    | 'CONFIRM_FRAUD'
    | 'FALSE_POSITIVE'
    | 'ESCALATE'

export interface FraudAssessmentReviewRequest {
    reviewNotes: string
    expectedVersion: number
}

export interface FraudApiFieldError {
    field: string
    message: string
}

export interface FraudApiErrorResponse {
    status: number
    error?: string
    message: string
    timestamp: string
    fieldErrors?: FraudApiFieldError[] | null
}

export interface FraudAssessmentFilterState {
    reviewStatus: FraudReviewStatus | ''
    riskLevel: FraudRiskLevel | ''
    classification: FraudClassification | ''
    supplierId: string
    departmentId: string
    modelVersion: string
    assessedFrom: string
    assessedTo: string
}

export const EMPTY_FRAUD_ASSESSMENT_FILTERS:
    FraudAssessmentFilterState = {
    reviewStatus: '',
    riskLevel: '',
    classification: '',
    supplierId: '',
    departmentId: '',
    modelVersion: '',
    assessedFrom: '',
    assessedTo: '',
}

export const FRAUD_REVIEW_STATUS_LABELS:
    Record<FraudReviewStatus, string> = {
    PENDING_REVIEW: 'Pending review',
    AUTO_CLEARED: 'Auto-cleared',
    CONFIRMED_FRAUD: 'Confirmed fraud',
    FALSE_POSITIVE: 'False positive',
    ESCALATED: 'Escalated',
}

export const FRAUD_RISK_LEVEL_LABELS:
    Record<FraudRiskLevel, string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High',
    CRITICAL: 'Critical',
}

export const FRAUD_CLASSIFICATION_LABELS:
    Record<FraudClassification, string> = {
    LEGITIMATE: 'Legitimate',
    HIGH_RISK: 'High risk',
}

export const FRAUD_RECOMMENDED_ACTION_LABELS:
    Record<FraudRecommendedAction, string> = {
    ALLOW_WITH_MONITORING:
        'Allow with monitoring',
    REQUIRE_REVIEW:
        'Require review',
    HOLD_PAYMENT_AND_REVIEW:
        'Hold payment and review',
    BLOCK_AND_ESCALATE:
        'Block and escalate',
}

export const FRAUD_DETECTION_SOURCE_LABELS:
    Record<FraudDetectionSource, string> = {
    SUPERVISED_MODEL:
        'Supervised model',
    DETERMINISTIC_RULE:
        'Deterministic rule',
    HYBRID:
        'Hybrid detection',
}

export const FRAUD_ASSESSMENT_SORT_OPTIONS:
    ReadonlyArray<{
        value: FraudAssessmentSort
        label: string
    }> = [
    {
        value: 'assessedAt,desc',
        label: 'Newest assessments',
    },
    {
        value: 'assessedAt,asc',
        label: 'Oldest assessments',
    },
    {
        value: 'riskProbability,desc',
        label: 'Highest probability',
    },
    {
        value: 'riskProbability,asc',
        label: 'Lowest probability',
    },
    {
        value: 'invoiceAmount,desc',
        label: 'Highest invoice amount',
    },
    {
        value: 'invoiceAmount,asc',
        label: 'Lowest invoice amount',
    },
    {
        value: 'assessmentReference,asc',
        label: 'Assessment reference',
    },
]

export function canConfirmFraudAssessment(
    assessment: FraudAssessment,
): boolean {
    return (
        assessment.reviewStatus
        === 'PENDING_REVIEW'
        || assessment.reviewStatus
        === 'ESCALATED'
    )
}

export function canMarkFraudAssessmentFalsePositive(
    assessment: FraudAssessment,
): boolean {
    return (
        assessment.reviewStatus
        === 'PENDING_REVIEW'
        || assessment.reviewStatus
        === 'ESCALATED'
    )
}

export function canEscalateFraudAssessment(
    assessment: FraudAssessment,
): boolean {
    return (
        assessment.reviewStatus
        === 'PENDING_REVIEW'
        || assessment.reviewStatus
        === 'AUTO_CLEARED'
    )
}

export function isFraudAssessmentFinal(
    assessment: FraudAssessment,
): boolean {
    return (
        assessment.reviewStatus
        === 'CONFIRMED_FRAUD'
        || assessment.reviewStatus
        === 'FALSE_POSITIVE'
    )
}

export function getRiskProbabilityPercentage(
    probability: number,
): number {
    return Math.round(
        probability * 10000
    ) / 100
}