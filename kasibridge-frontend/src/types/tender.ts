export type TraderTenderStatus =
    'PUBLISHED'

export interface TraderTender {
    id: number
    tenderReference: string
    title: string
    description: string
    evaluationCriteria: string
    budgetAmount: number
    buyerOrgId: string
    status: TraderTenderStatus
    publishedAt: string
}

export interface PageMetadata {
    size: number
    number: number
    totalElements: number
    totalPages: number
}

export interface TenderPageResponse {
    content: TraderTender[]
    page: PageMetadata
}

export interface TenderQuery {
    page: number
    size: number
}