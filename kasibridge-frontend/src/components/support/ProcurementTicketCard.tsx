import {
    Link,
} from 'react-router-dom'

import {
    SUPPORT_TICKET_TYPE_LABELS,
} from '../../types/supportTicket'

import type {
    SupportTicket,
} from '../../types/supportTicket'

import {
    formatDateTime,
} from '../../utils/tenderFormatters'

import {
    SupportTicketStatusBadge,
} from './SupportTicketStatusBadge'

interface ProcurementTicketCardProps {
    ticket: SupportTicket
}

export function ProcurementTicketCard({
                                          ticket,
                                      }: ProcurementTicketCardProps) {
    return (
        <article
            className={
                'content-card '
                + 'procurement-ticket-card'
            }
        >
            <div className="support-ticket-card-header">
                <div>
                    <span className="eyebrow">
                        {ticket.ticketReference}
                    </span>

                    <h2>{ticket.subject}</h2>
                </div>

                <SupportTicketStatusBadge
                    status={ticket.status}
                />
            </div>

            <dl className="profile-details">
                <div>
                    <dt>Ticket type</dt>

                    <dd>
                        {
                            SUPPORT_TICKET_TYPE_LABELS[
                                ticket.ticketType
                                ]
                        }
                    </dd>
                </div>

                <div>
                    <dt>Tender ID</dt>

                    <dd>{ticket.tenderId}</dd>
                </div>

                {ticket.bidId !== null && (
                    <div>
                        <dt>Bid ID</dt>

                        <dd>{ticket.bidId}</dd>
                    </div>
                )}

                <div>
                    <dt>Business</dt>

                    <dd>
                        {
                            ticket.businessNameSnapshot
                            || 'Not available'
                        }
                    </dd>
                </div>

                <div>
                    <dt>Contact name</dt>

                    <dd>
                        {
                            ticket.contactName
                            || 'Not available'
                        }
                    </dd>
                </div>

                <div>
                    <dt>Created</dt>

                    <dd>
                        {formatDateTime(
                            ticket.createdAt
                        )}
                    </dd>
                </div>

                {ticket.assignedAt && (
                    <div>
                        <dt>Assigned</dt>

                        <dd>
                            {formatDateTime(
                                ticket.assignedAt
                            )}
                        </dd>
                    </div>
                )}

                {ticket.assignmentSource && (
                    <div>
                        <dt>Assignment source</dt>

                        <dd>
                            {ticket.assignmentSource}
                        </dd>
                    </div>
                )}
            </dl>

            <div className="support-ticket-summary">
                <h3>Description</h3>

                <p className="tender-detail-text">
                    {ticket.description}
                </p>
            </div>

            {ticket.assignmentReason && (
                <div className="ticket-assignment-summary">
                    <h3>Assignment reason</h3>

                    <p className="tender-detail-text">
                        {ticket.assignmentReason}
                    </p>
                </div>
            )}

            {ticket.response && (
                <div className="support-ticket-response">
                    <h3>Published response</h3>

                    <p className="tender-detail-text">
                        {ticket.response}
                    </p>
                </div>
            )}

            {ticket.rejectionReason && (
                <div
                    className="error-message"
                    role="status"
                >
                    <strong>
                        Rejection reason:
                    </strong>{' '}
                    {ticket.rejectionReason}
                </div>
            )}

            {ticket.closureNotes && (
                <div className="support-ticket-closure">
                    <h3>Closure notes</h3>

                    <p className="tender-detail-text">
                        {ticket.closureNotes}
                    </p>
                </div>
            )}

            <div className="form-actions">
                <Link
                    className="primary-link-button"
                    to={
                        '/procurement/support-tickets/'
                        + ticket.id
                    }
                >
                    Review ticket
                </Link>
            </div>
        </article>
    )
}