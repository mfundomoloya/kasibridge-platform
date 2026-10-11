import {
    useEffect,
    useState,
} from 'react'

import {
    Link,
} from 'react-router-dom'

import {
    getMySupportTickets,
    getSupportTicketApiError,
} from '../api/supportTicketApi'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import {
    SupportTicketCard,
} from '../components/support/SupportTicketCard'

import type {
    SupportTicket,
} from '../types/supportTicket'

export function SupportTicketsPage() {
    const [tickets, setTickets] =
        useState<SupportTicket[]>([])

    const [currentPage, setCurrentPage] =
        useState(0)

    const [totalPages, setTotalPages] =
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
                const ticketPage =
                    await getMySupportTickets({
                        page: currentPage,
                        size: 10,
                        sort: 'createdAt,desc',
                    })

                if (!isActive) {
                    return
                }

                setTickets(ticketPage.content)
                setTotalPages(ticketPage.totalPages)
            } catch (error) {
                if (!isActive) {
                    return
                }

                const apiError =
                    getSupportTicketApiError(error)

                setTickets([])
                setTotalPages(0)
                setErrorMessage(apiError.message)
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
        <AppLayout>
            <section className="page-header">
                <div className="page-header-actions">
                    <div>
                        <span className="eyebrow">
                            Trader portal
                        </span>

                        <h1>Support tickets</h1>

                        <p>
                            Track your support requests,
                            tender clarifications, compliance
                            appeals, and technical issues.
                        </p>
                    </div>

                    <Link
                        className="primary-link-button"
                        to="/support-tickets/new"
                    >
                        Create support ticket
                    </Link>
                </div>
            </section>

            {isLoading ? (
                <section className="content-card">
                    <p>
                        Loading your support tickets...
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
                </section>
            ) : tickets.length === 0 ? (
                <section className="content-card">
                    <span className="eyebrow">
                        Support activity
                    </span>

                    <h2>No support tickets</h2>

                    <p>
                        You have not created any support
                        tickets yet.
                    </p>

                    <div className="form-actions">
                        <Link
                            className="primary-link-button"
                            to="/support-tickets/new"
                        >
                            Create your first ticket
                        </Link>
                    </div>
                </section>
            ) : (
                <>
                    <section
                        className="support-ticket-list"
                        aria-label="My support tickets"
                    >
                        {tickets.map((ticket) => (
                            <SupportTicketCard
                                key={ticket.id}
                                ticket={ticket}
                            />
                        ))}
                    </section>

                    {totalPages > 1 && (
                        <nav
                            className="pagination-controls"
                            aria-label={
                                'Support ticket pagination'
                            }
                        >
                            <button
                                className="secondary-button"
                                type="button"
                                disabled={currentPage === 0}
                                onClick={goToPreviousPage}
                            >
                                Previous
                            </button>

                            <span>
                                Page {currentPage + 1}
                                {' '}of {totalPages}
                            </span>

                            <button
                                className="secondary-button"
                                type="button"
                                disabled={
                                    currentPage
                                    >= totalPages - 1
                                }
                                onClick={goToNextPage}
                            >
                                Next
                            </button>
                        </nav>
                    )}
                </>
            )}
        </AppLayout>
    )
}