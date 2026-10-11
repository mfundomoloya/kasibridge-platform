export type SupportTicketType =
    | 'CLARIFICATION_REQUEST'
    | 'COMPLIANCE_APPEAL'
    | 'UPLOAD_ISSUE'
    | 'GENERAL_SUPPORT'

export type SupportTicketStatus =
    | 'OPEN'
    | 'IN_REVIEW'
    | 'RESPONDED'
    | 'CLOSED'
    | 'REJECTED'

export type TicketAssignmentSource =
    | 'MANUAL'
    | 'AI_ROUTING'
    | 'SYSTEM_FALLBACK'

export interface SupportTicket {
    id: number
    ticketReference: string
    tenderId: number
    bidId: number | null
    createdByUserId: number
    traderId: number | null
    contactName: string | null
    contactPhoneNumber: string | null
    contactEmail: string | null
    businessNameSnapshot: string | null
    ticketType: SupportTicketType
    status: SupportTicketStatus
    subject: string
    description: string
    response: string | null
    assignedToUserId: number | null
    assignedByUserId: number | null
    assignedAt: string | null
    assignmentReason: string | null
    assignmentSource:
        TicketAssignmentSource | null
    reviewedByUserId: number | null
    reviewStartedAt: string | null
    rejectedByUserId: number | null
    rejectedAt: string | null
    rejectionReason: string | null
    respondedByUserId: number | null
    respondedAt: string | null
    closedByUserId: number | null
    closedAt: string | null
    closureNotes: string | null
    publicClarification: boolean
    createdAt: string
    updatedAt: string
}

export interface CreateSupportTicketRequest {
    bidId?: number | null
    ticketType: SupportTicketType
    subject: string
    description: string
}

export interface PageSort {
    empty: boolean
    sorted: boolean
    unsorted: boolean
}

export interface Pageable {
    offset: number
    sort: PageSort
    pageNumber: number
    pageSize: number
    paged: boolean
    unpaged: boolean
}

export interface SupportTicketPage {
    content: SupportTicket[]
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

export interface SupportTicketApiError {
    status: number
    message: string
    fieldErrors: Record<string, string> | null
}

export const SUPPORT_TICKET_TYPE_LABELS:
    Record<SupportTicketType, string> = {
    CLARIFICATION_REQUEST:
        'Clarification request',
    COMPLIANCE_APPEAL:
        'Compliance appeal',
    UPLOAD_ISSUE:
        'Upload issue',
    GENERAL_SUPPORT:
        'General support',
}

export const SUPPORT_TICKET_STATUS_LABELS:
    Record<SupportTicketStatus, string> = {
    OPEN: 'Open',
    IN_REVIEW: 'In review',
    RESPONDED: 'Responded',
    CLOSED: 'Closed',
    REJECTED: 'Rejected',
}