import {
    type FormEvent,
    useEffect,
    useState,
} from 'react'

import {
    assignSupportTicket,
    getUnassignedSupportTickets,
} from '../api/procurementSupportApi'

import {
    getSupportTicketApiError,
} from '../api/supportTicketApi'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import {
    ProcurementTicketCard,
} from '../components/support/ProcurementTicketCard'

import type {
    SupportTicket,
} from '../types/supportTicket'

interface AssignmentFormState {
    assignedToUserId: string
    assignmentReason: string
}

const initialAssignmentForm:
    AssignmentFormState = {
    assignedToUserId: '',
    assignmentReason: '',
}

export function UnassignedSupportTicketsPage() {
    const [tickets, setTickets] =
        useState<SupportTicket[]>([])

    const [selectedTicket, setSelectedTicket] =
        useState<SupportTicket | null>(null)

    const [assignmentForm, setAssignmentForm] =
        useState<AssignmentFormState>(
            initialAssignmentForm
        )

    const [currentPage, setCurrentPage] =
        useState(0)

    const [totalPages, setTotalPages] =
        useState(0)

    const [totalElements, setTotalElements] =
        useState(0)

    const [isLoading, setIsLoading] =
        useState(true)

    const [isAssigning, setIsAssigning] =
        useState(false)

    const [errorMessage, setErrorMessage] =
        useState('')

    const [assignmentError, setAssignmentError] =
        useState('')

    useEffect(() => {
        let isActive = true

        async function loadTickets() {
            setIsLoading(true)
            setErrorMessage('')

            try {
                const ticketPage =
                    await getUnassignedSupportTickets({
                        page: currentPage,
                        size: 10,
                        sort: 'createdAt,asc',
                    })

                if (!isActive) {
                    return
                }

                setTickets(ticketPage.content)
                setTotalPages(ticketPage.totalPages)
                setTotalElements(
                    ticketPage.totalElements
                )
            } catch (error) {
                if (!isActive) {
                    return
                }

                const apiError =
                    getSupportTicketApiError(
                        error
                    )

                setTickets([])
                setTotalPages(0)
                setTotalElements(0)

                if (apiError.status === 403) {
                    setErrorMessage(
                        'Only authorised platform '
                        + 'administrators may access '
                        + 'the unassigned ticket queue.'
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

        void loadTickets()

        return () => {
            isActive = false
        }
    }, [currentPage])

    function openAssignmentForm(
        ticket: SupportTicket
    ) {
        setSelectedTicket(ticket)

        setAssignmentForm(
            initialAssignmentForm
        )

        setAssignmentError('')
    }

    function closeAssignmentForm() {
        if (isAssigning) {
            return
        }

        setSelectedTicket(null)

        setAssignmentForm(
            initialAssignmentForm
        )

        setAssignmentError('')
    }

    async function handleAssignment(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault()

        if (!selectedTicket || isAssigning) {
            return
        }

        const assignedToUserId =
            Number(
                assignmentForm
                    .assignedToUserId
            )

        const assignmentReason =
            assignmentForm
                .assignmentReason
                .trim()

        if (
            !Number.isInteger(
                assignedToUserId
            )
            || assignedToUserId <= 0
        ) {
            setAssignmentError(
                'Enter a valid staff user ID.'
            )
            return
        }

        if (!assignmentReason) {
            setAssignmentError(
                'An assignment reason is required.'
            )
            return
        }

        if (
            assignmentReason.length > 1000
        ) {
            setAssignmentError(
                'The assignment reason cannot '
                + 'exceed 1000 characters.'
            )
            return
        }

        setIsAssigning(true)
        setAssignmentError('')

        try {
            const assignedTicket =
                await assignSupportTicket(
                    selectedTicket.id,
                    {
                        assignedToUserId,
                        assignmentReason,
                    }
                )

            setTickets(
                (currentTickets) =>
                    currentTickets.filter(
                        (ticket) =>
                            ticket.id
                            !== assignedTicket.id
                    )
            )

            setTotalElements(
                (currentTotal) =>
                    Math.max(
                        0,
                        currentTotal - 1
                    )
            )

            setSelectedTicket(null)

            setAssignmentForm(
                initialAssignmentForm
            )
        } catch (error) {
            const apiError =
                getSupportTicketApiError(
                    error
                )

            setAssignmentError(
                apiError.message
            )
        } finally {
            setIsAssigning(false)
        }
    }

    function goToPreviousPage() {
        setCurrentPage((page) =>
            Math.max(0, page - 1)
        )
    }

    function goToNextPage() {
        setCurrentPage((page) =>
            Math.min(
                totalPages - 1,
                page + 1
            )
        )
    }

    return (
        <ProcurementLayout>
            <section className="page-header">
                <span className="eyebrow">
                    Platform administration
                </span>

                <h1>Unassigned support tickets</h1>

                <p>
                    Assign queued support tickets to
                    authorised staff members and record
                    the reason for each assignment.
                </p>
            </section>

            <section
                className="procurement-queue-toolbar"
                aria-label={
                    'Unassigned ticket summary'
                }
            >
                <div>
                    <strong>
                        Assignment queue
                    </strong>

                    <p className="field-help">
                        Oldest tickets are shown first.
                    </p>
                </div>

                <span className="notification-count">
                    {totalElements}{' '}
                    {totalElements === 1
                        ? 'unassigned ticket'
                        : 'unassigned tickets'}
                </span>
            </section>

            {errorMessage && (
                <div
                    className="error-message"
                    role="alert"
                >
                    {errorMessage}
                </div>
            )}

            {isLoading ? (
                <section className="content-card">
                    <p>
                        Loading unassigned support
                        tickets...
                    </p>
                </section>
            ) : tickets.length === 0 ? (
                <section className="content-card">
                    <span className="eyebrow">
                        Assignment queue
                    </span>

                    <h2>No unassigned tickets</h2>

                    <p>
                        All current support tickets
                        have been assigned.
                    </p>
                </section>
            ) : (
                <>
                    <section
                        className={
                            'procurement-ticket-list'
                        }
                        aria-label={
                            'Unassigned support tickets'
                        }
                    >
                        {tickets.map((ticket) => (
                            <div
                                className={
                                    'unassigned-ticket-item'
                                }
                                key={ticket.id}
                            >
                                <ProcurementTicketCard
                                    ticket={ticket}
                                />

                                <div
                                    className={
                                        'unassigned-ticket-actions'
                                    }
                                >
                                    <button
                                        className={
                                            'primary-button'
                                        }
                                        type="button"
                                        disabled={
                                            isAssigning
                                        }
                                        onClick={() => {
                                            openAssignmentForm(
                                                ticket
                                            )
                                        }}
                                    >
                                        Assign ticket
                                    </button>
                                </div>
                            </div>
                        ))}
                    </section>

                    {totalPages > 1 && (
                        <nav
                            className={
                                'pagination-controls'
                            }
                            aria-label={
                                'Unassigned support '
                                + 'ticket pagination'
                            }
                        >
                            <button
                                className={
                                    'secondary-button'
                                }
                                type="button"
                                disabled={
                                    currentPage === 0
                                    || isLoading
                                }
                                onClick={
                                    goToPreviousPage
                                }
                            >
                                Previous
                            </button>

                            <span>
                                Page {currentPage + 1}
                                {' '}of {totalPages}
                            </span>

                            <button
                                className={
                                    'secondary-button'
                                }
                                type="button"
                                disabled={
                                    currentPage
                                    >= totalPages - 1
                                    || isLoading
                                }
                                onClick={
                                    goToNextPage
                                }
                            >
                                Next
                            </button>
                        </nav>
                    )}
                </>
            )}

            {selectedTicket && (
                <section
                    className={
                        'content-card '
                        + 'ticket-assignment-panel'
                    }
                    aria-labelledby={
                        'ticket-assignment-title'
                    }
                >
                    <span className="eyebrow">
                        Manual assignment
                    </span>

                    <h2 id="ticket-assignment-title">
                        Assign{' '}
                        {selectedTicket.ticketReference}
                    </h2>

                    <p>
                        Enter the authorised staff
                        user ID and a clear reason for
                        the assignment.
                    </p>

                    {assignmentError && (
                        <div
                            className="error-message"
                            role="alert"
                        >
                            {assignmentError}
                        </div>
                    )}

                    <form
                        onSubmit={handleAssignment}
                        noValidate
                    >
                        <div className="form-group">
                            <label
                                htmlFor={
                                    'assignedToUserId'
                                }
                            >
                                Staff user ID
                            </label>

                            <input
                                id="assignedToUserId"
                                name="assignedToUserId"
                                type="number"
                                min="1"
                                step="1"
                                inputMode="numeric"
                                value={
                                    assignmentForm
                                        .assignedToUserId
                                }
                                disabled={
                                    isAssigning
                                }
                                onChange={(event) => {
                                    setAssignmentForm(
                                        (current) => ({
                                            ...current,
                                            assignedToUserId:
                                            event
                                                .target
                                                .value,
                                        })
                                    )

                                    setAssignmentError('')
                                }}
                            />
                        </div>

                        <div className="form-group">
                            <label
                                htmlFor={
                                    'assignmentReason'
                                }
                            >
                                Assignment reason
                            </label>

                            <textarea
                                id="assignmentReason"
                                name="assignmentReason"
                                rows={6}
                                maxLength={1000}
                                value={
                                    assignmentForm
                                        .assignmentReason
                                }
                                disabled={
                                    isAssigning
                                }
                                onChange={(event) => {
                                    setAssignmentForm(
                                        (current) => ({
                                            ...current,
                                            assignmentReason:
                                            event
                                                .target
                                                .value,
                                        })
                                    )

                                    setAssignmentError('')
                                }}
                            />

                            <p className="field-help">
                                {
                                    assignmentForm
                                        .assignmentReason
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
                                    isAssigning
                                }
                            >
                                {isAssigning
                                    ? 'Assigning ticket...'
                                    : 'Confirm assignment'}
                            </button>

                            <button
                                className={
                                    'secondary-button'
                                }
                                type="button"
                                disabled={
                                    isAssigning
                                }
                                onClick={
                                    closeAssignmentForm
                                }
                            >
                                Cancel
                            </button>
                        </div>
                    </form>
                </section>
            )}
        </ProcurementLayout>
    )
}