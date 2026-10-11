import {
    useEffect,
    useState,
} from 'react'

import {
    getAssignedSupportTickets,
    getAssignedSupportTicketsByStatus,
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

import {
    SUPPORT_TICKET_STATUS_LABELS,
} from '../types/supportTicket'

import type {
    SupportTicket,
    SupportTicketStatus,
} from '../types/supportTicket'

type QueueStatusFilter =
    | 'ALL'
    | SupportTicketStatus

interface StatusFilterOption {
    value: QueueStatusFilter
    label: string
}

const statusFilters: StatusFilterOption[] = [
    {
        value: 'ALL',
        label: 'All assigned',
    },
    {
        value: 'OPEN',
        label:
        SUPPORT_TICKET_STATUS_LABELS.OPEN,
    },
    {
        value: 'IN_REVIEW',
        label:
        SUPPORT_TICKET_STATUS_LABELS.IN_REVIEW,
    },
    {
        value: 'RESPONDED',
        label:
        SUPPORT_TICKET_STATUS_LABELS.RESPONDED,
    },
    {
        value: 'CLOSED',
        label:
        SUPPORT_TICKET_STATUS_LABELS.CLOSED,
    },
    {
        value: 'REJECTED',
        label:
        SUPPORT_TICKET_STATUS_LABELS.REJECTED,
    },
]

export function ProcurementSupportQueuePage() {
    const [tickets, setTickets] =
        useState<SupportTicket[]>([])

    const [selectedStatus, setSelectedStatus] =
        useState<QueueStatusFilter>('ALL')

    const [currentPage, setCurrentPage] =
        useState(0)

    const [totalPages, setTotalPages] =
        useState(0)

    const [totalElements, setTotalElements] =
        useState(0)

    const [isLoading, setIsLoading] =
        useState(true)

    const [errorMessage, setErrorMessage] =
        useState('')

    useEffect(() => {
        let isActive = true

        async function loadTickets() {
            setIsLoading(true)
            setErrorMessage('')

            try {
                const query = {
                    page: currentPage,
                    size: 10,
                    sort: 'assignedAt,desc',
                }

                const ticketPage =
                    selectedStatus === 'ALL'
                        ? await getAssignedSupportTickets(
                            query
                        )
                        : await getAssignedSupportTicketsByStatus(
                            selectedStatus,
                            query
                        )

                if (!isActive) {
                    return
                }

                setTickets(ticketPage.content)
                setTotalPages(
                    ticketPage.totalPages
                )
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
                        'You are not authorised to '
                        + 'access the assigned support '
                        + 'ticket queue.'
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
    }, [
        currentPage,
        selectedStatus,
    ])

    function handleStatusChange(
        status: QueueStatusFilter
    ) {
        setSelectedStatus(status)
        setCurrentPage(0)
        setErrorMessage('')
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
                    Procurement support
                </span>

                <h1>Assigned support queue</h1>

                <p>
                    Review support tickets assigned to
                    your account and perform only the
                    actions permitted by each ticket's
                    current workflow state.
                </p>
            </section>

            <section
                className="procurement-queue-toolbar"
                aria-label="Support queue filters"
            >
                <div
                    className="notification-filter-group"
                    role="group"
                    aria-label={
                        'Filter assigned tickets by status'
                    }
                >
                    {statusFilters.map((filter) => (
                        <button
                            key={filter.value}
                            className={
                                'notification-filter-button '
                                + (
                                    selectedStatus
                                    === filter.value
                                        ? 'is-active'
                                        : ''
                                )
                            }
                            type="button"
                            aria-pressed={
                                selectedStatus
                                === filter.value
                            }
                            disabled={isLoading}
                            onClick={() => {
                                handleStatusChange(
                                    filter.value
                                )
                            }}
                        >
                            {filter.label}
                        </button>
                    ))}
                </div>

                <span className="notification-count">
                    {totalElements}{' '}
                    {totalElements === 1
                        ? 'assigned ticket'
                        : 'assigned tickets'}
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
                        Loading assigned support
                        tickets...
                    </p>
                </section>
            ) : tickets.length === 0 ? (
                <section className="content-card">
                    <span className="eyebrow">
                        Assigned queue
                    </span>

                    <h2>
                        {selectedStatus === 'ALL'
                            ? 'No assigned tickets'
                            : (
                                'No '
                                + SUPPORT_TICKET_STATUS_LABELS[
                                    selectedStatus
                                    ].toLowerCase()
                                + ' tickets'
                            )}
                    </h2>

                    <p>
                        {selectedStatus === 'ALL'
                            ? (
                                'No support tickets are '
                                + 'currently assigned to '
                                + 'your account.'
                            )
                            : (
                                'No assigned support '
                                + 'tickets match the '
                                + 'selected status.'
                            )}
                    </p>
                </section>
            ) : (
                <>
                    <section
                        className={
                            'procurement-ticket-list'
                        }
                        aria-label={
                            'Assigned support tickets'
                        }
                    >
                        {tickets.map((ticket) => (
                            <ProcurementTicketCard
                                key={ticket.id}
                                ticket={ticket}
                            />
                        ))}
                    </section>

                    {totalPages > 1 && (
                        <nav
                            className={
                                'pagination-controls'
                            }
                            aria-label={
                                'Assigned support '
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
        </ProcurementLayout>
    )
}