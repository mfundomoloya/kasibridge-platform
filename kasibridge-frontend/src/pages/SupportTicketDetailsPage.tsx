import {
    useEffect,
    useState,
} from 'react'

import {
    Link,
    useParams,
} from 'react-router-dom'

import {
    getSupportTicketApiError,
    getSupportTicketById,
} from '../api/supportTicketApi'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import {
    SupportTicketStatusBadge,
} from '../components/support/SupportTicketStatusBadge'

import {
    SUPPORT_TICKET_TYPE_LABELS,
} from '../types/supportTicket'

import type {
    SupportTicket,
} from '../types/supportTicket'

import {
    formatDateTime,
} from '../utils/tenderFormatters'

function parseTicketId(
    value: string | undefined
): number | null {
    if (!value) {
        return null
    }

    const parsedValue = Number(value)

    if (
        !Number.isInteger(parsedValue)
        || parsedValue <= 0
    ) {
        return null
    }

    return parsedValue
}

export function SupportTicketDetailsPage() {
    const { ticketId } = useParams()

    const [ticket, setTicket] =
        useState<SupportTicket | null>(null)

    const [isLoading, setIsLoading] =
        useState(true)

    const [errorMessage, setErrorMessage] =
        useState('')

    useEffect(() => {
        let isActive = true

        async function loadTicket() {
            const parsedTicketId =
                parseTicketId(ticketId)

            if (parsedTicketId === null) {
                if (isActive) {
                    setTicket(null)
                    setErrorMessage(
                        'The support ticket identifier is invalid.'
                    )
                    setIsLoading(false)
                }

                return
            }

            setIsLoading(true)
            setErrorMessage('')

            try {
                const loadedTicket =
                    await getSupportTicketById(
                        parsedTicketId
                    )

                if (!isActive) {
                    return
                }

                setTicket(loadedTicket)
            } catch (error) {
                if (!isActive) {
                    return
                }

                const apiError =
                    getSupportTicketApiError(
                        error
                    )

                setTicket(null)

                if (apiError.status === 404) {
                    setErrorMessage(
                        'The support ticket was not found.'
                    )
                } else if (
                    apiError.status === 403
                ) {
                    setErrorMessage(
                        'You are not authorised to view '
                        + 'this support ticket.'
                    )
                } else {
                    setErrorMessage(
                        apiError.message
                    )
                }
            } finally {
                if (isActive) {
                    setIsLoading(false)
                }
            }
        }

        void loadTicket()

        return () => {
            isActive = false
        }
    }, [ticketId])

    return (
        <AppLayout>
            <div className="back-navigation">
                <Link to="/support-tickets">
                    Back to support tickets
                </Link>
            </div>

            {isLoading ? (
                <section className="content-card">
                    <p>
                        Loading support ticket...
                    </p>
                </section>
            ) : errorMessage ? (
                <section className="content-card">
                    <div
                        className="error-message"
                        role="alert"
                    >
                        {errorMessage}
                    </div>

                    <div className="form-actions">
                        <Link
                            className="secondary-link-button"
                            to="/support-tickets"
                        >
                            Return to support tickets
                        </Link>
                    </div>
                </section>
            ) : ticket ? (
                <>
                    <section className="ticket-detail-hero">
                        <div>
                            <span className="eyebrow">
                                {ticket.ticketReference}
                            </span>

                            <h1>{ticket.subject}</h1>

                            <p>
                                {
                                    SUPPORT_TICKET_TYPE_LABELS[
                                        ticket.ticketType
                                        ]
                                }
                            </p>
                        </div>

                        <SupportTicketStatusBadge
                            status={ticket.status}
                        />
                    </section>

                    <section className="ticket-detail-grid">
                        <article className="content-card">
                            <span className="eyebrow">
                                Ticket information
                            </span>

                            <h2>Support request</h2>

                            <dl className="profile-details">
                                <div>
                                    <dt>Reference</dt>
                                    <dd>
                                        {ticket.ticketReference}
                                    </dd>
                                </div>

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
                                    <dt>Status</dt>
                                    <dd>
                                        <SupportTicketStatusBadge
                                            status={ticket.status}
                                        />
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

                                <div>
                                    <dt>Last updated</dt>
                                    <dd>
                                        {formatDateTime(
                                            ticket.updatedAt
                                        )}
                                    </dd>
                                </div>
                            </dl>
                        </article>

                        <article className="content-card">
                            <span className="eyebrow">
                                Contact snapshot
                            </span>

                            <h2>Submitted by</h2>

                            <dl className="profile-details">
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
                                    <dt>Contact email</dt>
                                    <dd>
                                        {
                                            ticket.contactEmail
                                            || 'Not available'
                                        }
                                    </dd>
                                </div>

                                <div>
                                    <dt>Contact number</dt>
                                    <dd>
                                        {
                                            ticket.contactPhoneNumber
                                            || 'Not available'
                                        }
                                    </dd>
                                </div>
                            </dl>
                        </article>

                        <article
                            className={
                                'content-card '
                                + 'ticket-detail-wide'
                            }
                        >
                            <span className="eyebrow">
                                Description
                            </span>

                            <h2>Issue details</h2>

                            <p className="tender-detail-text">
                                {ticket.description}
                            </p>
                        </article>

                        {ticket.response && (
                            <article
                                className={
                                    'content-card '
                                    + 'ticket-detail-wide '
                                    + 'support-ticket-response'
                                }
                            >
                                <span className="eyebrow">
                                    Official response
                                </span>

                                <h2>Response</h2>

                                <p className="tender-detail-text">
                                    {ticket.response}
                                </p>

                                {ticket.respondedAt && (
                                    <p className="field-help">
                                        Responded on{' '}
                                        {formatDateTime(
                                            ticket.respondedAt
                                        )}
                                    </p>
                                )}
                            </article>
                        )}

                        {ticket.rejectionReason && (
                            <article
                                className={
                                    'content-card '
                                    + 'ticket-detail-wide'
                                }
                            >
                                <span className="eyebrow">
                                    Ticket decision
                                </span>

                                <h2>Request rejected</h2>

                                <div
                                    className="error-message"
                                    role="status"
                                >
                                    {ticket.rejectionReason}
                                </div>

                                {ticket.rejectedAt && (
                                    <p className="field-help">
                                        Rejected on{' '}
                                        {formatDateTime(
                                            ticket.rejectedAt
                                        )}
                                    </p>
                                )}
                            </article>
                        )}

                        {ticket.closureNotes && (
                            <article
                                className={
                                    'content-card '
                                    + 'ticket-detail-wide'
                                }
                            >
                                <span className="eyebrow">
                                    Ticket closure
                                </span>

                                <h2>Closure notes</h2>

                                <p className="tender-detail-text">
                                    {ticket.closureNotes}
                                </p>

                                {ticket.closedAt && (
                                    <p className="field-help">
                                        Closed on{' '}
                                        {formatDateTime(
                                            ticket.closedAt
                                        )}
                                    </p>
                                )}
                            </article>
                        )}

                        {!ticket.response
                            && !ticket.rejectionReason
                            && !ticket.closureNotes
                            && (
                                <article
                                    className={
                                        'content-card '
                                        + 'ticket-detail-wide'
                                    }
                                >
                                    <span className="eyebrow">
                                        Current progress
                                    </span>

                                    <h2>
                                        Awaiting an official response
                                    </h2>

                                    <p className="tender-detail-text">
                                        The support team has not
                                        published a response for this
                                        ticket yet.
                                    </p>
                                </article>
                            )}
                    </section>
                </>
            ) : null}
        </AppLayout>
    )
}