import {
    useCallback,
    useEffect,
    useState,
} from 'react'

import {
    Link,
} from 'react-router-dom'

import {
    getTicketAiAssessments,
    getTicketAiAssessmentsByStatus,
} from '../api/supportTicketAiAssessmentApi'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import {
    TicketAiAssessmentStatusBadge,
} from '../components/support/TicketAiAssessmentStatusBadge'

import {
    AI_ASSESSMENT_PRIORITY_LABELS,
    AI_ASSESSMENT_STATUS_LABELS,
    AI_RESOLUTION_MODE_LABELS,
    formatAiDetectedCategory,
} from '../types/supportTicketAiAssessment'

import type {
    SupportTicketAiAssessmentPage,
    TicketAiAssessmentStatus,
} from '../types/supportTicketAiAssessment'

const PAGE_SIZE = 20

const STATUS_OPTIONS:
    Array<{
        value: TicketAiAssessmentStatus | ''
        label: string
    }> = [
    {
        value: '',
        label: 'All statuses',
    },
    {
        value: 'GENERATED',
        label:
        AI_ASSESSMENT_STATUS_LABELS.GENERATED,
    },
    {
        value: 'APPROVED',
        label:
        AI_ASSESSMENT_STATUS_LABELS.APPROVED,
    },
    {
        value: 'EDITED_AND_APPROVED',
        label:
        AI_ASSESSMENT_STATUS_LABELS
            .EDITED_AND_APPROVED,
    },
    {
        value: 'RESPONSE_PUBLISHED',
        label:
        AI_ASSESSMENT_STATUS_LABELS
            .RESPONSE_PUBLISHED,
    },
    {
        value: 'REJECTED',
        label:
        AI_ASSESSMENT_STATUS_LABELS.REJECTED,
    },
    {
        value: 'ESCALATED',
        label:
        AI_ASSESSMENT_STATUS_LABELS.ESCALATED,
    },
]

interface ApiErrorResponse {
    message?: string
    error?: string
}

function getErrorMessage(
    error: unknown,
): string {
    if (
        typeof error === 'object'
        && error !== null
        && 'response' in error
    ) {
        const response =
            (
                error as {
                    response?: {
                        status?: number
                        data?: ApiErrorResponse
                    }
                }
            ).response

        if (response?.data?.message) {
            return response.data.message
        }

        if (response?.data?.error) {
            return response.data.error
        }

        if (response?.status === 403) {
            return (
                'You are not authorised to view '
                + 'AI assessment records.'
            )
        }
    }

    if (
        error instanceof Error
        && error.message.trim().length > 0
    ) {
        return error.message
    }

    return 'Unable to load AI assessments.'
}

function formatDateTime(
    value: string | null,
): string {
    if (!value) {
        return 'Not recorded'
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return value
    }

    return date.toLocaleString()
}

function formatConfidenceScore(
    score: number | null,
): string {
    if (score === null) {
        return 'Not available'
    }

    const normalizedScore =
        score >= 0 && score <= 1
            ? score * 100
            : score

    return `${normalizedScore.toFixed(1)}%`
}

export function TicketAiAssessmentsPage() {
    const [
        assessmentPage,
        setAssessmentPage,
    ] = useState<
        SupportTicketAiAssessmentPage | null
    >(null)

    const [
        selectedStatus,
        setSelectedStatus,
    ] = useState<
        TicketAiAssessmentStatus | ''
    >('')

    const [
        currentPage,
        setCurrentPage,
    ] = useState(0)

    const [
        isLoading,
        setIsLoading,
    ] = useState(true)

    const [
        errorMessage,
        setErrorMessage,
    ] = useState('')

    const loadAssessments = useCallback(
        async () => {
            setIsLoading(true)
            setErrorMessage('')

            try {
                const query = {
                    page: currentPage,
                    size: PAGE_SIZE,
                    sort: 'createdAt,desc',
                }

                const loadedPage =
                    selectedStatus
                        ? await getTicketAiAssessmentsByStatus(
                            selectedStatus,
                            query
                        )
                        : await getTicketAiAssessments(
                            query
                        )

                setAssessmentPage(loadedPage)
            } catch (error) {
                setAssessmentPage(null)
                setErrorMessage(
                    getErrorMessage(error)
                )
            } finally {
                setIsLoading(false)
            }
        },
        [
            currentPage,
            selectedStatus,
        ]
    )

    useEffect(() => {
        let isActive = true

        const query = {
            page: currentPage,
            size: PAGE_SIZE,
            sort: 'createdAt,desc',
        }

        const request =
            selectedStatus
                ? getTicketAiAssessmentsByStatus(
                    selectedStatus,
                    query
                )
                : getTicketAiAssessments(query)

        request
            .then((loadedPage) => {
                if (!isActive) {
                    return
                }

                setAssessmentPage(loadedPage)
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (!isActive) {
                    return
                }

                setAssessmentPage(null)
                setErrorMessage(
                    getErrorMessage(error)
                )
            })
            .finally(() => {
                if (isActive) {
                    setIsLoading(false)
                }
            })

        return () => {
            isActive = false
        }
    }, [
        currentPage,
        selectedStatus,
    ])

    function handleStatusChange(
        value: string,
    ) {
        setIsLoading(true)

        setSelectedStatus(
            value as TicketAiAssessmentStatus | ''
        )

        setCurrentPage(0)
    }

    const assessments =
        assessmentPage?.content ?? []

    return (
        <ProcurementLayout>
            <section className="page-header">
                <div>
                    <span className="eyebrow">
                        Clarification governance
                    </span>

                    <h1>
                        AI assessment queue
                    </h1>

                    <p>
                        Review assessment decisions and
                        open the related support ticket
                        to approve, reject, escalate, or
                        publish an authorised response.
                    </p>
                </div>
            </section>

            <section className="content-card">
                <div className="filter-toolbar">
                    <div className="form-group">
                        <label
                            htmlFor={
                                'assessment-status-filter'
                            }
                        >
                            Assessment status
                        </label>

                        <select
                            id={
                                'assessment-status-filter'
                            }
                            value={selectedStatus}
                            disabled={isLoading}
                            onChange={(event) =>
                                handleStatusChange(
                                    event.target.value
                                )
                            }
                        >
                            {STATUS_OPTIONS.map(
                                (option) => (
                                    <option
                                        key={
                                            option.value
                                            || 'ALL'
                                        }
                                        value={
                                            option.value
                                        }
                                    >
                                        {option.label}
                                    </option>
                                )
                            )}
                        </select>
                    </div>

                    <button
                        type="button"
                        className="secondary-button"
                        disabled={isLoading}
                        onClick={() =>
                            void loadAssessments()
                        }
                    >
                        {isLoading
                            ? 'Refreshing...'
                            : 'Refresh'}
                    </button>
                </div>

                {errorMessage && (
                    <div
                        className="error-message"
                        role="alert"
                    >
                        {errorMessage}
                    </div>
                )}

                {isLoading ? (
                    <p>
                        Loading AI assessments...
                    </p>
                ) : assessments.length === 0 ? (
                    <div className="empty-state">
                        <h2>
                            No assessments found
                        </h2>

                        <p>
                            There are no AI assessments
                            matching the selected status.
                        </p>
                    </div>
                ) : (
                    <div className="ticket-list">
                        {assessments.map(
                            (assessment) => (
                                <article
                                    className={
                                        'content-card '
                                        + 'procurement-ticket-card'
                                    }
                                    key={assessment.id}
                                >
                                    <div
                                        className={
                                            'procurement-ticket-card'
                                            + '__header'
                                        }
                                    >
                                        <div>
                                            <span
                                                className={
                                                    'eyebrow'
                                                }
                                            >
                                                {
                                                    assessment
                                                        .assessmentReference
                                                }
                                            </span>

                                            <h2>
                                                Ticket{' '}
                                                {
                                                    assessment
                                                        .ticketId
                                                }
                                            </h2>
                                        </div>

                                        <TicketAiAssessmentStatusBadge
                                            status={
                                                assessment
                                                    .assessmentStatus
                                            }
                                        />
                                    </div>

                                    <p
                                        className={
                                            'tender-detail-text'
                                        }
                                    >
                                        {assessment.summary}
                                    </p>

                                    <dl
                                        className={
                                            'profile-details'
                                        }
                                    >
                                        <div>
                                            <dt>
                                                Category
                                            </dt>

                                            <dd>
                                                {
                                                    formatAiDetectedCategory(
                                                        assessment
                                                            .detectedCategory
                                                    )
                                                }
                                            </dd>
                                        </div>

                                        <div>
                                            <dt>
                                                Priority
                                            </dt>

                                            <dd>
                                                {
                                                    AI_ASSESSMENT_PRIORITY_LABELS[
                                                        assessment
                                                            .priority
                                                        ]
                                                }
                                            </dd>
                                        </div>

                                        <div>
                                            <dt>
                                                Resolution mode
                                            </dt>

                                            <dd>
                                                {
                                                    AI_RESOLUTION_MODE_LABELS[
                                                        assessment
                                                            .resolutionMode
                                                        ]
                                                }
                                            </dd>
                                        </div>

                                        <div>
                                            <dt>
                                                Confidence
                                            </dt>

                                            <dd>
                                                {
                                                    formatConfidenceScore(
                                                        assessment
                                                            .confidenceScore
                                                    )
                                                }
                                            </dd>
                                        </div>

                                        <div>
                                            <dt>
                                                Created
                                            </dt>

                                            <dd>
                                                {
                                                    formatDateTime(
                                                        assessment
                                                            .createdAt
                                                    )
                                                }
                                            </dd>
                                        </div>

                                        <div>
                                            <dt>
                                                Last updated
                                            </dt>

                                            <dd>
                                                {
                                                    formatDateTime(
                                                        assessment
                                                            .updatedAt
                                                    )
                                                }
                                            </dd>
                                        </div>
                                    </dl>

                                    <div
                                        className={
                                            'form-actions'
                                        }
                                    >
                                        <Link
                                            className={
                                                'primary-link-button'
                                            }
                                            to={
                                                '/procurement/'
                                                + 'support-tickets/'
                                                + assessment.ticketId
                                            }
                                        >
                                            Open ticket workflow
                                        </Link>
                                    </div>
                                </article>
                            )
                        )}
                    </div>
                )}

                {assessmentPage
                    && assessmentPage.totalPages > 1 && (
                        <div className="pagination-controls">
                            <button
                                type="button"
                                className="secondary-button"
                                disabled={
                                    isLoading
                                    || assessmentPage.first
                                }
                                onClick={() => {
                                    setIsLoading(true)

                                    setCurrentPage(
                                        (page) =>
                                            Math.max(
                                                page - 1,
                                                0
                                            )
                                    )
                                }}
                            >
                                Previous
                            </button>

                            <span>
                            Page{' '}
                                {assessmentPage.number + 1}
                                {' '}of{' '}
                                {assessmentPage.totalPages}
                        </span>

                            <button
                                type="button"
                                className="secondary-button"
                                disabled={
                                    isLoading
                                    || assessmentPage.last
                                }
                                onClick={() => {
                                    setIsLoading(true)
                                    setCurrentPage(
                                        (page) =>
                                            page + 1
                                    )
                                }}
                            >
                                Next
                            </button>
                        </div>
                    )}
            </section>
        </ProcurementLayout>
    )
}