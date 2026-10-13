import {
    useEffect,
    useState,
} from 'react'

import {
    Link,
    useSearchParams,
} from 'react-router-dom'

import {
    getPublicClarifications,
    getSupportTicketApiError,
} from '../api/supportTicketApi'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import {
    SUPPORT_TICKET_TYPE_LABELS,
} from '../types/supportTicket'

import type {
    SupportTicket,
} from '../types/supportTicket'

import {
    formatDateTime,
} from '../utils/tenderFormatters'

function parsePositiveInteger(
    value: string | null
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

export function OfficialClarificationsPage() {
    const [searchParams] =
        useSearchParams()

    const tenderId =
        parsePositiveInteger(
            searchParams.get('tenderId')
        )

    const [clarifications, setClarifications] =
        useState<SupportTicket[]>([])

    const [currentPage, setCurrentPage] =
        useState(0)

    const [totalPages, setTotalPages] =
        useState(0)

    const [isLoading, setIsLoading] =
        useState(false)

    const [errorMessage, setErrorMessage] =
        useState('')

    useEffect(() => {
        let isActive = true

        async function loadClarifications() {
            if (tenderId === null) {
                setClarifications([])
                setTotalPages(0)
                setErrorMessage('')
                setIsLoading(false)
                return
            }

            setIsLoading(true)
            setErrorMessage('')

            try {
                const clarificationPage =
                    await getPublicClarifications(
                        tenderId,
                        {
                            page: currentPage,
                            size: 10,
                            sort: 'respondedAt,desc',
                        }
                    )

                if (!isActive) {
                    return
                }

                setClarifications(
                    clarificationPage.content
                )

                setTotalPages(
                    clarificationPage.totalPages
                )
            } catch (error) {
                if (!isActive) {
                    return
                }

                const apiError =
                    getSupportTicketApiError(
                        error
                    )

                setClarifications([])
                setTotalPages(0)

                if (apiError.status === 404) {
                    setErrorMessage(
                        'The selected tender was not found.'
                    )
                } else if (
                    apiError.status === 403
                ) {
                    setErrorMessage(
                        'You are not authorised to view '
                        + 'clarifications for this tender.'
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

        void loadClarifications()

        return () => {
            isActive = false
        }
    }, [tenderId, currentPage])


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
                <span className="eyebrow">
                    Tender information
                </span>

                <h1>Official clarifications</h1>

                <p>
                    Review clarification responses
                    published for a selected tender.
                </p>
            </section>

            {tenderId === null ? (
                <section className="content-card">
                    <span className="eyebrow">
                        Select a tender
                    </span>

                    <h2>
                        No tender has been selected
                    </h2>

                    <p>
                        Open a tender to view its
                        published official clarifications.
                    </p>

                    <div className="form-actions">
                        <Link
                            className="primary-link-button"
                            to="/tenders"
                        >
                            View open tenders
                        </Link>
                    </div>
                </section>
            ) : isLoading ? (
                <section className="content-card">
                    <p>
                        Loading official
                        clarifications...
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
                            to={`/tenders/${tenderId}`}
                        >
                            Return to tender
                        </Link>
                    </div>
                </section>
            ) : clarifications.length === 0 ? (
                <section className="content-card">
                    <span className="eyebrow">
                        Tender {tenderId}
                    </span>

                    <h2>
                        No official clarifications
                    </h2>

                    <p>
                        No clarification responses
                        have been published for this
                        tender.
                    </p>

                    <div className="form-actions">
                        <Link
                            className="primary-link-button"
                            to={
                                '/support-tickets/new'
                                + `?tenderId=${tenderId}`
                            }
                        >
                            Request clarification
                        </Link>

                        <Link
                            className="secondary-link-button"
                            to={`/tenders/${tenderId}`}
                        >
                            Return to tender
                        </Link>
                    </div>
                </section>
            ) : (
                <>
                    <section
                        className="clarification-list"
                        aria-label={
                            'Official tender clarifications'
                        }
                    >
                        {clarifications.map(
                            (clarification) => (
                                <article
                                    key={clarification.id}
                                    className={
                                        'content-card '
                                        + 'clarification-card'
                                    }
                                >
                                    <div
                                        className={
                                            'clarification-card-header'
                                        }
                                    >
                                        <div>
                                            <span className="eyebrow">
                                                {
                                                    clarification
                                                        .ticketReference
                                                }
                                            </span>

                                            <h2>
                                                {
                                                    clarification
                                                        .subject
                                                }
                                            </h2>
                                        </div>

                                        <span
                                            className={
                                                'official-clarification-badge'
                                            }
                                        >
                                            Official response
                                        </span>
                                    </div>

                                    <dl className="profile-details">
                                        <div>
                                            <dt>Request type</dt>

                                            <dd>
                                                {
                                                    SUPPORT_TICKET_TYPE_LABELS[
                                                        clarification
                                                            .ticketType
                                                        ]
                                                }
                                            </dd>
                                        </div>

                                        <div>
                                            <dt>Published</dt>

                                            <dd>
                                                {clarification
                                                    .respondedAt
                                                    ? formatDateTime(
                                                        clarification
                                                            .respondedAt
                                                    )
                                                    : 'Not available'}
                                            </dd>
                                        </div>
                                    </dl>

                                    <div
                                        className={
                                            'clarification-question'
                                        }
                                    >
                                        <h3>Question</h3>

                                        <p
                                            className={
                                                'tender-detail-text'
                                            }
                                        >
                                            {
                                                clarification
                                                    .description
                                            }
                                        </p>
                                    </div>

                                    <div
                                        className={
                                            'clarification-response'
                                        }
                                    >
                                        <h3>
                                            Official response
                                        </h3>

                                        <p
                                            className={
                                                'tender-detail-text'
                                            }
                                        >
                                            {
                                                clarification.response
                                                || 'No response is available.'
                                            }
                                        </p>
                                    </div>
                                </article>
                            )
                        )}
                    </section>

                    {totalPages > 1 && (
                        <nav
                            className="pagination-controls"
                            aria-label={
                                'Clarification pagination'
                            }
                        >
                            <button
                                className="secondary-button"
                                type="button"
                                disabled={
                                    currentPage === 0
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
                                className="secondary-button"
                                type="button"
                                disabled={
                                    currentPage
                                    >= totalPages - 1
                                }
                                onClick={
                                    goToNextPage
                                }
                            >
                                Next
                            </button>
                        </nav>
                    )}

                    <div className="form-actions">
                        <Link
                            className="primary-link-button"
                            to={
                                '/support-tickets/new'
                                + `?tenderId=${tenderId}`
                            }
                        >
                            Request clarification
                        </Link>

                        <Link
                            className="secondary-link-button"
                            to={`/tenders/${tenderId}`}
                        >
                            Return to tender
                        </Link>
                    </div>
                </>
            )}
        </AppLayout>
    )
}