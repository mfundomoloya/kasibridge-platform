import {
    SUPPORT_TICKET_STATUS_LABELS,
} from '../../types/supportTicket'

import type {
    SupportTicketStatus,
} from '../../types/supportTicket'

interface SupportTicketStatusBadgeProps {
    status: SupportTicketStatus
}

const statusClassNames:
    Record<SupportTicketStatus, string> = {
    OPEN: 'ticket-status-open',
    IN_REVIEW: 'ticket-status-review',
    RESPONDED: 'ticket-status-responded',
    CLOSED: 'ticket-status-closed',
    REJECTED: 'ticket-status-rejected',
}

export function SupportTicketStatusBadge({
                                             status,
                                         }: SupportTicketStatusBadgeProps) {
    const label =
        SUPPORT_TICKET_STATUS_LABELS[status]

    const statusClassName =
        statusClassNames[status]

    return (
        <span
            className={
                'support-ticket-status-badge '
                + statusClassName
            }
            aria-label={
                `Support ticket status: ${label}`
            }
        >
            {label}
        </span>
    )
}