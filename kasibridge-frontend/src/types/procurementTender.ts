export type ProcurementTenderStatus =
    | 'DRAFT'
    | 'PUBLISHED'
    | 'BIDDING_CLOSED'
    | 'EVALUATION'
    | 'ADJUDICATION'
    | 'AWARDED'
    | 'CANCELLED'

export interface ProcurementTender {
    id: number
    tenderReference: string
    title: string
    description: string
    evaluationCriteria: string
    budgetAmount: number
    buyerOrgId: string
    requiredBusinessType: string
    serviceArea: string
    createdByUserId: number
    status: ProcurementTenderStatus
    specificationHash: string | null
    publishedAt: string | null
    closedAt: string | null
    createdAt: string
    updatedAt: string
}

export interface CreateProcurementTenderRequest {
    title: string
    description: string
    evaluationCriteria: string
    budgetAmount: number
    buyerOrgId: string
    requiredBusinessType: string
    serviceArea: string
    createdByUserId: number
}

export interface SpecificationIntegrity {
    tenderId: number
    tenderReference: string
    storedHash: string
    currentHash: string
    integrityValid: boolean
    message: string
}

export interface ProcurementTenderPageMetadata {
    size: number
    number: number
    totalElements: number
    totalPages: number
}

export interface ProcurementTenderPage {
    content: ProcurementTender[]
    page: ProcurementTenderPageMetadata
}

export interface ProcurementTenderQuery {
    page?: number
    size?: number
    sort?: ProcurementTenderSort
}

export type ProcurementTenderSort =
    | 'createdAt,desc'
    | 'createdAt,asc'
    | 'updatedAt,desc'
    | 'updatedAt,asc'
    | 'publishedAt,desc'
    | 'publishedAt,asc'
    | 'tenderReference,asc'
    | 'tenderReference,desc'
    | 'title,asc'
    | 'title,desc'
    | 'budgetAmount,desc'
    | 'budgetAmount,asc'
    | 'status,asc'
    | 'status,desc'

export interface ProcurementApiFieldError {
    field: string
    message: string
}

export interface ProcurementApiErrorResponse {
    status: number
    error?: string
    message: string
    timestamp: string
    fieldErrors?:
        ProcurementApiFieldError[] | null
}

export interface ProcurementTenderStatistics {
    total: number
    draft: number
    published: number
    biddingClosed: number
    evaluation: number
    adjudication: number
    awarded: number
    cancelled: number
}

export const PROCUREMENT_TENDER_STATUS_LABELS:
    Record<ProcurementTenderStatus, string> = {
    DRAFT: 'Draft',
    PUBLISHED: 'Published',
    BIDDING_CLOSED: 'Bidding closed',
    EVALUATION: 'Evaluation',
    ADJUDICATION: 'Adjudication',
    AWARDED: 'Awarded',
    CANCELLED: 'Cancelled',
}

export const PROCUREMENT_TENDER_SORT_OPTIONS:
    ReadonlyArray<{
        value: ProcurementTenderSort
        label: string
    }> = [
    {
        value: 'createdAt,desc',
        label: 'Newest created',
    },
    {
        value: 'createdAt,asc',
        label: 'Oldest created',
    },
    {
        value: 'updatedAt,desc',
        label: 'Recently updated',
    },
    {
        value: 'publishedAt,desc',
        label: 'Recently published',
    },
    {
        value: 'tenderReference,asc',
        label: 'Tender reference',
    },
    {
        value: 'title,asc',
        label: 'Title A to Z',
    },
    {
        value: 'budgetAmount,desc',
        label: 'Highest budget',
    },
    {
        value: 'budgetAmount,asc',
        label: 'Lowest budget',
    },
    {
        value: 'status,asc',
        label: 'Workflow status',
    },
]

export function canPublishTender(
    tender: ProcurementTender,
): boolean {
    return tender.status === 'DRAFT'
}

export function canVerifyTenderSpecification(
    tender: ProcurementTender,
): boolean {
    return (
        tender.status !== 'DRAFT'
        && tender.specificationHash !== null
        && tender.specificationHash
            .trim()
            .length > 0
    )
}

export function canCloseTenderBidding(
    tender: ProcurementTender,
): boolean {
    return tender.status === 'PUBLISHED'
}

export function canStartTenderEvaluation(
    tender: ProcurementTender,
): boolean {
    return tender.status
        === 'BIDDING_CLOSED'
}

/*
 * This helper is for displaying workflow state only.
 * The Specification Officer interface must not expose
 * the start-adjudication action.
 */
export function isTenderInEvaluation(
    tender: ProcurementTender,
): boolean {
    return tender.status === 'EVALUATION'
}

export function isTenderFinal(
    tender: ProcurementTender,
): boolean {
    return (
        tender.status === 'AWARDED'
        || tender.status === 'CANCELLED'
    )
}

export function calculateTenderStatistics(
    tenders: ProcurementTender[],
): ProcurementTenderStatistics {
    const statistics:
        ProcurementTenderStatistics = {
        total: tenders.length,
        draft: 0,
        published: 0,
        biddingClosed: 0,
        evaluation: 0,
        adjudication: 0,
        awarded: 0,
        cancelled: 0,
    }

    for (const tender of tenders) {
        switch (tender.status) {
            case 'DRAFT':
                statistics.draft += 1
                break

            case 'PUBLISHED':
                statistics.published += 1
                break

            case 'BIDDING_CLOSED':
                statistics.biddingClosed += 1
                break

            case 'EVALUATION':
                statistics.evaluation += 1
                break

            case 'ADJUDICATION':
                statistics.adjudication += 1
                break

            case 'AWARDED':
                statistics.awarded += 1
                break

            case 'CANCELLED':
                statistics.cancelled += 1
                break
        }
    }

    return statistics
}