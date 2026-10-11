import {
    Link,
} from 'react-router-dom'

import type {
    SupportTicket,
} from '../../types/supportTicket'

import {
    SUPPORT_TICKET_TYPE_LABELS,
} from '../../types/supportTicket'

import {
    formatDateTime,
} from '../../utils/tenderFormatters'

import {
    SupportTicketStatusBadge,
} from './SupportTicketStatusBadge'

interface SupportTicketCardProps {
    ticket: SupportTicket
}

export function SupportTicketCard({
                                      ticket,
                                  }: SupportTicketCardProps) {
    return (
        <article className="content-card support-ticket-card">
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
                    <dt>Created</dt>

                    <dd>
                        {formatDateTime(
                            ticket.createdAt
                        )}
                    </dd>
                </div>

                <div>
                    <dt>Last updated</dt>

                    <dd>
                        {formatDateTime(
                            ticket.updatedAt
                        )}
                    </dd>
                </div>
            </dl>

            <div className="support-ticket-summary">
                <h3>Description</h3>

                <p className="tender-detail-text">
                    {ticket.description}
                </p>
            </div>

            {ticket.response && (
                <div className="support-ticket-response">
                    <h3>Response</h3>

                    <p className="tender-detail-text">
                        {ticket.response}
                    </p>

                    {ticket.respondedAt && (
                        <p className="support-ticket-timestamp">
                            Responded{' '}
                            {formatDateTime(
                                ticket.respondedAt
                            )}
                        </p>
                    )}
                </div>
            )}

            {ticket.rejectionReason && (
                <div className="error-message">
                    <strong>Rejection reason:</strong>{' '}
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
                    className="secondary-link-button"
                    to={`/support-tickets/${ticket.id}`}
                >
                    View ticket
                </Link>
            </div>
        </article>
    )
}