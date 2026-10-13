import type {
    Pageable,
    PageSort,
} from './supportTicket'

export type AiDetectedCategory =
    | 'DOCUMENT_REQUIREMENT_QUERY'
    | 'DEADLINE_QUERY'
    | 'SCORING_DISPUTE'
    | 'AWARD_DISPUTE'
    | 'COMPLIANCE_OVERRIDE_REQUEST'
    | 'CORRUPTION_ALLEGATION'
    | 'GENERAL_SUPPORT'
    | 'UNKNOWN'
    | string

export type TicketAiAssessmentPriority =
    | 'LOW'
    | 'MEDIUM'
    | 'HIGH'
    | 'CRITICAL'

export type AiResolutionMode =
    | 'AUTO_RESOLVE'
    | 'DRAFT_FOR_APPROVAL'
    | 'HUMAN_ONLY'

export type TicketAiAssessmentStatus =
    | 'GENERATED'
    | 'APPROVED'
    | 'EDITED_AND_APPROVED'
    | 'RESPONSE_PUBLISHED'
    | 'REJECTED'
    | 'ESCALATED'

export interface SupportTicketAiAssessment {
    id: number
    assessmentReference: string
    ticketId: number

    detectedCategory: AiDetectedCategory
    priority: TicketAiAssessmentPriority
    resolutionMode: AiResolutionMode
    assessmentStatus: TicketAiAssessmentStatus

    summary: string
    suggestedResponse: string | null
    escalationReason: string | null

    confidenceScore: number | null
    modelVersion: string | null

    approvedResponse: string | null
    approvedByUserId: number | null
    approvedAt: string | null

    responseRelevanceConfirmed: boolean
    responseRelevanceConfirmedByUserId:
        number | null
    responseRelevanceConfirmedAt:
        string | null
    responseRelevanceConfirmationNotes:
        string | null

    publishedByUserId: number | null
    publishedAt: string | null

    rejectedByUserId: number | null
    rejectedAt: string | null
    rejectionReason: string | null

    createdAt: string
    updatedAt: string
    version: number
}

export interface SupportTicketAiAssessmentPage {
    content: SupportTicketAiAssessment[]
    pageable: Pageable
    totalPages: number
    totalElements: number
    last: boolean
    size: number
    number: number
    sort: PageSort
    numberOfElements: number
    first: boolean
    empty: boolean
}

export interface ApproveTicketAiAssessmentRequest {
    approvedResponse?: string | null
    responseRelevanceConfirmed: boolean
    responseRelevanceConfirmationNotes: string
}

export interface RejectTicketAiAssessmentRequest {
    rejectionReason: string
}

export interface EscalateTicketAiAssessmentRequest {
    escalationReason: string
}

export interface TicketAiAssessmentQuery {
    page?: number
    size?: number
    sort?: string
}

export const AI_ASSESSMENT_STATUS_LABELS:
    Record<TicketAiAssessmentStatus, string> = {
    GENERATED: 'Generated',
    APPROVED: 'Approved',
    EDITED_AND_APPROVED:
        'Edited and approved',
    RESPONSE_PUBLISHED:
        'Response published',
    REJECTED: 'Rejected',
    ESCALATED: 'Escalated',
}

export const AI_RESOLUTION_MODE_LABELS:
    Record<AiResolutionMode, string> = {
    AUTO_RESOLVE: 'Auto resolve',
    DRAFT_FOR_APPROVAL:
        'Draft for approval',
    HUMAN_ONLY: 'Human only',
}

export const AI_ASSESSMENT_PRIORITY_LABELS:
    Record<TicketAiAssessmentPriority, string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High',
    CRITICAL: 'Critical',
}

export function formatAiDetectedCategory(
    category: AiDetectedCategory,
): string {
    return category
        .toLowerCase()
        .split('_')
        .map((word) =>
            word.length > 0
                ? word.charAt(0).toUpperCase()
                + word.slice(1)
                : word
        )
        .join(' ')
}

export function canApproveAiAssessment(
    assessment: SupportTicketAiAssessment,
): boolean {
    return (
        assessment.assessmentStatus
        === 'GENERATED'
        && assessment.resolutionMode
        !== 'HUMAN_ONLY'
    )
}

export function canRejectAiAssessment(
    assessment: SupportTicketAiAssessment,
): boolean {
    return assessment.assessmentStatus
        === 'GENERATED'
}

export function canEscalateAiAssessment(
    assessment: SupportTicketAiAssessment,
): boolean {
    return assessment.assessmentStatus
        === 'GENERATED'
}

export function canPublishAiAssessment(
    assessment: SupportTicketAiAssessment,
    currentUserId: number | null,
): boolean {
    const hasPublishableStatus =
        assessment.assessmentStatus
        === 'APPROVED'
        || assessment.assessmentStatus
        === 'EDITED_AND_APPROVED'

    const hasApprovedResponse =
        assessment.approvedResponse !== null
        && assessment.approvedResponse.trim()
            .length > 0

    const hasCompleteRelevanceConfirmation =
        assessment.responseRelevanceConfirmed
        && assessment
            .responseRelevanceConfirmedByUserId
        !== null
        && assessment
            .responseRelevanceConfirmedAt
        !== null
        && assessment
            .responseRelevanceConfirmationNotes
        !== null
        && assessment
            .responseRelevanceConfirmationNotes
            .trim()
            .length > 0

    const isDifferentPublisher =
        currentUserId !== null
        && assessment.approvedByUserId !== null
        && currentUserId
        !== assessment.approvedByUserId

    return (
        hasPublishableStatus
        && assessment.resolutionMode
        !== 'HUMAN_ONLY'
        && hasApprovedResponse
        && hasCompleteRelevanceConfirmation
        && isDifferentPublisher
    )
}