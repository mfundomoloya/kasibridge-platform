export type ProcurementAnomalyType =
    | 'HIGHEST_SCORE_BYPASS'
    | 'RAPID_AWARD'
    | 'LARGE_SCORE_GAP'
    | 'SPECIFICATION_TAMPER_DETECTED'
    | 'MANUAL_PRICE_SCORE_ATTEMPT'

export type ProcurementAnomalySeverity =
    | 'LOW'
    | 'MEDIUM'
    | 'HIGH'
    | 'CRITICAL'

export type ProcurementAnomalyStatus =
    | 'OPEN'
    | 'REVIEWED'
    | 'DISMISSED'

export interface ProcurementAnomalyRecord {
    id: number
    anomalyReference: string
    tenderId: number
    bidId: number | null
    type: ProcurementAnomalyType
    severity: ProcurementAnomalySeverity
    status: ProcurementAnomalyStatus
    message: string
    evidence: string
    detectedAt: string
    reviewedByUserId: number | null
    reviewNotes: string | null
    reviewedAt: string | null
}

export interface ProcurementAnomalyPageMetadata {
    size: number
    number: number
    totalElements: number
    totalPages: number
}

export interface ProcurementAnomalyPage {
    content: ProcurementAnomalyRecord[]
    page: ProcurementAnomalyPageMetadata
}

export function isBlockingAwardAnomaly(
    anomaly: ProcurementAnomalyRecord,
): boolean {
    return (
        anomaly.status === 'OPEN'
        && (
            anomaly.severity === 'HIGH'
            || anomaly.severity === 'CRITICAL'
        )
    )
}