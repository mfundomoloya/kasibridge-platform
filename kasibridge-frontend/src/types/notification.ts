export type NotificationChannel =
    | 'WHATSAPP'
    | 'EMAIL'
    | 'SMS'
    | 'IN_APP'

export type NotificationStatus =
    | 'PENDING'
    | 'PROCESSING'
    | 'SENT'
    | 'FAILED'
    | 'CANCELLED'

export type NotificationTemplateType =
    | 'SUPPORT_TICKET_CREATED'
    | 'SUPPORT_TICKET_RESPONDED'
    | 'SUPPORT_TICKET_CLOSED'
    | 'OFFICIAL_CLARIFICATION_PUBLISHED'
    | 'BID_RECEIVED'
    | 'BID_COMPLIANCE_PASSED'
    | 'BID_COMPLIANCE_FAILED'
    | 'TENDER_AWARDED'
    | 'PROCUREMENT_ANOMALY_DETECTED'
    | 'BID_WITHDRAWN'

export interface Notification {
    id: number
    notificationReference: string
    channel: NotificationChannel
    templateType: NotificationTemplateType
    status: NotificationStatus
    recipientUserId: number | null
    recipientPhone: string | null
    recipientEmail: string | null
    message: string
    relatedTenderId: number | null
    relatedBidId: number | null
    relatedTicketId: number | null
    retryCount: number
    failureReason: string | null
    providerMessageId: string | null
    readByUserId: number | null
    readAt: string | null
    read: boolean
    createdAt: string
    sentAt: string | null
    updatedAt: string | null
}

export interface NotificationPageMetadata {
    size: number
    number: number
    totalElements: number
    totalPages: number
}

export interface NotificationPage {
    content: Notification[]
    page: NotificationPageMetadata
}

export interface NotificationApiError {
    status: number
    message: string
    fieldErrors: Record<string, string> | null
}

export const NOTIFICATION_TEMPLATE_LABELS:
    Record<NotificationTemplateType, string> = {
    SUPPORT_TICKET_CREATED:
        'Support ticket created',
    SUPPORT_TICKET_RESPONDED:
        'Support ticket response',
    SUPPORT_TICKET_CLOSED:
        'Support ticket closed',
    OFFICIAL_CLARIFICATION_PUBLISHED:
        'Official clarification published',
    BID_RECEIVED:
        'Bid received',
    BID_COMPLIANCE_PASSED:
        'Bid compliance passed',
    BID_COMPLIANCE_FAILED:
        'Bid compliance failed',
    TENDER_AWARDED:
        'Tender awarded',
    PROCUREMENT_ANOMALY_DETECTED:
        'Procurement alert',
    BID_WITHDRAWN:
        'Bid withdrawn',
}