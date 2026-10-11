import {
    type FormEvent,
    useEffect,
    useState,
} from 'react'

import {
    Link,
    useParams,
} from 'react-router-dom'

import {
    closeSupportTicket,
    rejectSupportTicket,
    respondToSupportTicket,
    returnSupportTicketToQueue,
    startSupportTicketReview,
} from '../api/procurementSupportApi'

import {
    getSupportTicketApiError,
    getSupportTicketById,
} from '../api/supportTicketApi'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

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

type TicketAction =
    | 'respond'
    | 'reject'
    | 'close'
    | 'return-to-queue'

interface ActionFormState {
    response: string
    rejectionReason: string
    closureNotes: string
    returnReason: string
}

const initialActionForm: ActionFormState = {
    response: '',
    rejectionReason: '',
    closureNotes: '',
    returnReason: '',
}

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

export function ProcurementSupportTicketDetailsPage() {
    const { ticketId } = useParams()

    const [ticket, setTicket] =
        useState<SupportTicket | null>(null)

    const [selectedAction, setSelectedAction] =
        useState<TicketAction | null>(null)

    const [actionForm, setActionForm] =
        useState<ActionFormState>(
            initialActionForm
        )

    const [isLoading, setIsLoading] =
        useState(true)

    const [isSubmitting, setIsSubmitting] =
        useState(false)

    const [errorMessage, setErrorMessage] =
        useState('')

    const [actionError, setActionError] =
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

    function selectAction(
        action: TicketAction
    ) {
        setSelectedAction(action)
        setActionForm(initialActionForm)
        setActionError('')
    }

    function cancelAction() {
        if (isSubmitting) {
            return
        }

        setSelectedAction(null)
        setActionForm(initialActionForm)
        setActionError('')
    }

    function applyUpdatedTicket(
        updatedTicket: SupportTicket
    ) {
        setTicket(updatedTicket)
        setSelectedAction(null)
        setActionForm(initialActionForm)
        setActionError('')
    }

    async function handleStartReview() {
        if (!ticket || isSubmitting) {
            return
        }

        setIsSubmitting(true)
        setActionError('')

        try {
            const updatedTicket =
                await startSupportTicketReview(
                    ticket.id
                )

            applyUpdatedTicket(updatedTicket)
        } catch (error) {
            const apiError =
                getSupportTicketApiError(
                    error
                )

            setActionError(apiError.message)
        } finally {
            setIsSubmitting(false)
        }
    }

    async function handleRespond(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault()

        if (!ticket || isSubmitting) {
            return
        }

        const response =
            actionForm.response.trim()

        if (!response) {
            setActionError(
                'A response is required.'
            )
            return
        }

        if (response.length > 3000) {
            setActionError(
                'The response cannot exceed '
                + '3000 characters.'
            )
            return
        }

        setIsSubmitting(true)
        setActionError('')

        try {
            const updatedTicket =
                await respondToSupportTicket(
                    ticket.id,
                    {
                        response,
                        publicClarification: false,
                    }
                )

            applyUpdatedTicket(updatedTicket)
        } catch (error) {
            const apiError =
                getSupportTicketApiError(
                    error
                )

            setActionError(apiError.message)
        } finally {
            setIsSubmitting(false)
        }
    }

    async function handleReject(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault()

        if (!ticket || isSubmitting) {
            return
        }

        const rejectionReason =
            actionForm.rejectionReason.trim()

        if (!rejectionReason) {
            setActionError(
                'A rejection reason is required.'
            )
            return
        }

        if (rejectionReason.length > 1000) {
            setActionError(
                'The rejection reason cannot '
                + 'exceed 1000 characters.'
            )
            return
        }

        setIsSubmitting(true)
        setActionError('')

        try {
            const updatedTicket =
                await rejectSupportTicket(
                    ticket.id,
                    {
                        rejectionReason,
                    }
                )

            applyUpdatedTicket(updatedTicket)
        } catch (error) {
            const apiError =
                getSupportTicketApiError(
                    error
                )

            setActionError(apiError.message)
        } finally {
            setIsSubmitting(false)
        }
    }

    async function handleClose(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault()

        if (!ticket || isSubmitting) {
            return
        }

        const closureNotes =
            actionForm.closureNotes.trim()

        if (!closureNotes) {
            setActionError(
                'Closure notes are required.'
            )
            return
        }

        if (closureNotes.length > 1000) {
            setActionError(
                'Closure notes cannot exceed '
                + '1000 characters.'
            )
            return
        }

        setIsSubmitting(true)
        setActionError('')

        try {
            const updatedTicket =
                await closeSupportTicket(
                    ticket.id,
                    {
                        closureNotes,
                    }
                )

            applyUpdatedTicket(updatedTicket)
        } catch (error) {
            const apiError =
                getSupportTicketApiError(
                    error
                )

            setActionError(apiError.message)
        } finally {
            setIsSubmitting(false)
        }
    }

    async function handleReturnToQueue(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault()

        if (!ticket || isSubmitting) {
            return
        }

        const reason =
            actionForm.returnReason.trim()

        if (!reason) {
            setActionError(
                'A return-to-queue reason is required.'
            )
            return
        }

        if (reason.length > 1000) {
            setActionError(
                'The return-to-queue reason cannot '
                + 'exceed 1000 characters.'
            )
            return
        }

        setIsSubmitting(true)
        setActionError('')

        try {
            const updatedTicket =
                await returnSupportTicketToQueue(
                    ticket.id,
                    {
                        reason,
                    }
                )

            applyUpdatedTicket(updatedTicket)
        } catch (error) {
            const apiError =
                getSupportTicketApiError(
                    error
                )

            setActionError(apiError.message)
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <ProcurementLayout>
            <div className="back-navigation">
                <Link
                    to="/procurement/support-tickets"
                >
                    Back to assigned support queue
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
                            to={
                                '/procurement/'
                                + 'support-tickets'
                            }
                        >
                            Return to support queue
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
                                        {
                                            ticket
                                                .ticketReference
                                        }
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
                                Assignment
                            </span>

                            <h2>Workflow ownership</h2>

                            <dl className="profile-details">
                                <div>
                                    <dt>
                                        Assigned user ID
                                    </dt>

                                    <dd>
                                        {
                                            ticket
                                                .assignedToUserId
                                            ?? 'Unassigned'
                                        }
                                    </dd>
                                </div>

                                <div>
                                    <dt>
                                        Assigned by user ID
                                    </dt>

                                    <dd>
                                        {
                                            ticket
                                                .assignedByUserId
                                            ?? 'Not available'
                                        }
                                    </dd>
                                </div>

                                <div>
                                    <dt>
                                        Assignment source
                                    </dt>

                                    <dd>
                                        {
                                            ticket
                                                .assignmentSource
                                            ?? 'Not available'
                                        }
                                    </dd>
                                </div>

                                <div>
                                    <dt>Assigned at</dt>

                                    <dd>
                                        {ticket.assignedAt
                                            ? formatDateTime(
                                                ticket
                                                    .assignedAt
                                            )
                                            : 'Not available'}
                                    </dd>
                                </div>
                            </dl>

                            {ticket.assignmentReason && (
                                <div
                                    className={
                                        'ticket-assignment-summary'
                                    }
                                >
                                    <h3>
                                        Assignment reason
                                    </h3>

                                    <p
                                        className={
                                            'tender-detail-text'
                                        }
                                    >
                                        {
                                            ticket
                                                .assignmentReason
                                        }
                                    </p>
                                </div>
                            )}
                        </article>

                        <article
                            className={
                                'content-card '
                                + 'ticket-detail-wide'
                            }
                        >
                            <span className="eyebrow">
                                Trader request
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
                                    Published response
                                </span>

                                <h2>Response</h2>

                                <p
                                    className={
                                        'tender-detail-text'
                                    }
                                >
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

                                <h2>Rejected</h2>

                                <div
                                    className="error-message"
                                    role="status"
                                >
                                    {ticket.rejectionReason}
                                </div>
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

                                <p
                                    className={
                                        'tender-detail-text'
                                    }
                                >
                                    {ticket.closureNotes}
                                </p>
                            </article>
                        )}

                        <article
                            className={
                                'content-card '
                                + 'ticket-detail-wide '
                                + 'procurement-ticket-actions'
                            }
                        >
                            <span className="eyebrow">
                                Authorised actions
                            </span>

                            <h2>Ticket workflow</h2>

                            {actionError && (
                                <div
                                    className="error-message"
                                    role="alert"
                                >
                                    {actionError}
                                </div>
                            )}

                            {!selectedAction && (
                                <div className="form-actions">
                                    {ticket.status
                                        === 'OPEN' && (
                                            <button
                                                className={
                                                    'primary-button'
                                                }
                                                type="button"
                                                disabled={
                                                    isSubmitting
                                                }
                                                onClick={
                                                    handleStartReview
                                                }
                                            >
                                                {isSubmitting
                                                    ? (
                                                        'Starting '
                                                        + 'review...'
                                                    )
                                                    : 'Start review'}
                                            </button>
                                        )}

                                    {ticket.status
                                        === 'IN_REVIEW'
                                        && ticket.ticketType
                                        !== (
                                            'CLARIFICATION_REQUEST'
                                        ) && (
                                            <button
                                                className={
                                                    'primary-button'
                                                }
                                                type="button"
                                                onClick={() => {
                                                    selectAction(
                                                        'respond'
                                                    )
                                                }}
                                            >
                                                Respond
                                            </button>
                                        )}

                                    {ticket.status
                                        === 'IN_REVIEW' && (
                                            <button
                                                className={
                                                    'danger-button'
                                                }
                                                type="button"
                                                onClick={() => {
                                                    selectAction(
                                                        'reject'
                                                    )
                                                }}
                                            >
                                                Reject
                                            </button>
                                        )}

                                    {ticket.status
                                        === 'IN_REVIEW' && (
                                            <button
                                                className={
                                                    'secondary-button'
                                                }
                                                type="button"
                                                onClick={() => {
                                                    selectAction(
                                                        'return-to-queue'
                                                    )
                                                }}
                                            >
                                                Return to queue
                                            </button>
                                        )}

                                    {ticket.status
                                        === 'RESPONDED' && (
                                            <button
                                                className={
                                                    'primary-button'
                                                }
                                                type="button"
                                                onClick={() => {
                                                    selectAction(
                                                        'close'
                                                    )
                                                }}
                                            >
                                                Close ticket
                                            </button>
                                        )}
                                </div>
                            )}

                            {ticket.ticketType
                                === 'CLARIFICATION_REQUEST'
                                && ticket.status
                                === 'IN_REVIEW'
                                && !selectedAction && (
                                    <p className="field-help">
                                        Clarification requests
                                        must use the official
                                        clarification approval and
                                        publication workflow.
                                    </p>
                                )}

                            {selectedAction
                                === 'respond' && (
                                    <form
                                        onSubmit={
                                            handleRespond
                                        }
                                    >
                                        <div className="form-group">
                                            <label
                                                htmlFor={
                                                    'ticketResponse'
                                                }
                                            >
                                                Response
                                            </label>

                                            <textarea
                                                id={
                                                    'ticketResponse'
                                                }
                                                rows={10}
                                                maxLength={3000}
                                                value={
                                                    actionForm
                                                        .response
                                                }
                                                disabled={
                                                    isSubmitting
                                                }
                                                onChange={(
                                                    event
                                                ) => {
                                                    setActionForm(
                                                        (
                                                            current
                                                        ) => ({
                                                            ...current,
                                                            response:
                                                            event
                                                                .target
                                                                .value,
                                                        })
                                                    )

                                                    setActionError('')
                                                }}
                                            />

                                            <p
                                                className={
                                                    'field-help'
                                                }
                                            >
                                                {
                                                    actionForm
                                                        .response
                                                        .length
                                                }
                                                /3000 characters
                                            </p>
                                        </div>

                                        <div className="form-actions">
                                            <button
                                                className={
                                                    'primary-button'
                                                }
                                                type="submit"
                                                disabled={
                                                    isSubmitting
                                                }
                                            >
                                                {isSubmitting
                                                    ? (
                                                        'Publishing '
                                                        + 'response...'
                                                    )
                                                    : (
                                                        'Publish '
                                                        + 'response'
                                                    )}
                                            </button>

                                            <button
                                                className={
                                                    'secondary-button'
                                                }
                                                type="button"
                                                disabled={
                                                    isSubmitting
                                                }
                                                onClick={
                                                    cancelAction
                                                }
                                            >
                                                Cancel
                                            </button>
                                        </div>
                                    </form>
                                )}

                            {selectedAction
                                === 'reject' && (
                                    <form
                                        onSubmit={
                                            handleReject
                                        }
                                    >
                                        <div className="form-group">
                                            <label
                                                htmlFor={
                                                    'rejectionReason'
                                                }
                                            >
                                                Rejection reason
                                            </label>

                                            <textarea
                                                id={
                                                    'rejectionReason'
                                                }
                                                rows={6}
                                                maxLength={1000}
                                                value={
                                                    actionForm
                                                        .rejectionReason
                                                }
                                                disabled={
                                                    isSubmitting
                                                }
                                                onChange={(
                                                    event
                                                ) => {
                                                    setActionForm(
                                                        (
                                                            current
                                                        ) => ({
                                                            ...current,
                                                            rejectionReason:
                                                            event
                                                                .target
                                                                .value,
                                                        })
                                                    )

                                                    setActionError('')
                                                }}
                                            />

                                            <p className="field-help">
                                                {
                                                    actionForm
                                                        .rejectionReason
                                                        .length
                                                }
                                                /1000 characters
                                            </p>
                                        </div>

                                        <div className="form-actions">
                                            <button
                                                className={
                                                    'danger-button'
                                                }
                                                type="submit"
                                                disabled={
                                                    isSubmitting
                                                }
                                            >
                                                {isSubmitting
                                                    ? (
                                                        'Rejecting '
                                                        + 'ticket...'
                                                    )
                                                    : 'Reject ticket'}
                                            </button>

                                            <button
                                                className={
                                                    'secondary-button'
                                                }
                                                type="button"
                                                disabled={
                                                    isSubmitting
                                                }
                                                onClick={
                                                    cancelAction
                                                }
                                            >
                                                Cancel
                                            </button>
                                        </div>
                                    </form>
                                )}

                            {selectedAction
                                === 'close' && (
                                    <form
                                        onSubmit={
                                            handleClose
                                        }
                                    >
                                        <div className="form-group">
                                            <label
                                                htmlFor={
                                                    'closureNotes'
                                                }
                                            >
                                                Closure notes
                                            </label>

                                            <textarea
                                                id={
                                                    'closureNotes'
                                                }
                                                rows={6}
                                                maxLength={1000}
                                                value={
                                                    actionForm
                                                        .closureNotes
                                                }
                                                disabled={
                                                    isSubmitting
                                                }
                                                onChange={(
                                                    event
                                                ) => {
                                                    setActionForm(
                                                        (
                                                            current
                                                        ) => ({
                                                            ...current,
                                                            closureNotes:
                                                            event
                                                                .target
                                                                .value,
                                                        })
                                                    )

                                                    setActionError('')
                                                }}
                                            />

                                            <p className="field-help">
                                                {
                                                    actionForm
                                                        .closureNotes
                                                        .length
                                                }
                                                /1000 characters
                                            </p>
                                        </div>

                                        <div className="form-actions">
                                            <button
                                                className={
                                                    'primary-button'
                                                }
                                                type="submit"
                                                disabled={
                                                    isSubmitting
                                                }
                                            >
                                                {isSubmitting
                                                    ? (
                                                        'Closing '
                                                        + 'ticket...'
                                                    )
                                                    : 'Close ticket'}
                                            </button>

                                            <button
                                                className={
                                                    'secondary-button'
                                                }
                                                type="button"
                                                disabled={
                                                    isSubmitting
                                                }
                                                onClick={
                                                    cancelAction
                                                }
                                            >
                                                Cancel
                                            </button>
                                        </div>
                                    </form>
                                )}

                            {selectedAction
                                === 'return-to-queue'
                                && (
                                    <form
                                        onSubmit={
                                            handleReturnToQueue
                                        }
                                    >
                                        <div className="form-group">
                                            <label
                                                htmlFor={
                                                    'returnReason'
                                                }
                                            >
                                                Return-to-queue reason
                                            </label>

                                            <textarea
                                                id={
                                                    'returnReason'
                                                }
                                                rows={6}
                                                maxLength={1000}
                                                value={
                                                    actionForm
                                                        .returnReason
                                                }
                                                disabled={
                                                    isSubmitting
                                                }
                                                onChange={(
                                                    event
                                                ) => {
                                                    setActionForm(
                                                        (
                                                            current
                                                        ) => ({
                                                            ...current,
                                                            returnReason:
                                                            event
                                                                .target
                                                                .value,
                                                        })
                                                    )

                                                    setActionError('')
                                                }}
                                            />

                                            <p className="field-help">
                                                {
                                                    actionForm
                                                        .returnReason
                                                        .length
                                                }
                                                /1000 characters
                                            </p>
                                        </div>

                                        <div className="form-actions">
                                            <button
                                                className={
                                                    'primary-button'
                                                }
                                                type="submit"
                                                disabled={
                                                    isSubmitting
                                                }
                                            >
                                                {isSubmitting
                                                    ? (
                                                        'Returning '
                                                        + 'ticket...'
                                                    )
                                                    : (
                                                        'Return '
                                                        + 'to queue'
                                                    )}
                                            </button>

                                            <button
                                                className={
                                                    'secondary-button'
                                                }
                                                type="button"
                                                disabled={
                                                    isSubmitting
                                                }
                                                onClick={
                                                    cancelAction
                                                }
                                            >
                                                Cancel
                                            </button>
                                        </div>
                                    </form>
                                )}
                        </article>
                    </section>
                </>
            ) : null}
        </ProcurementLayout>
    )
}