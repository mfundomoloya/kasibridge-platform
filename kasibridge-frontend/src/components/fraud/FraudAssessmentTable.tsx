import type {
    FraudAssessment,
    FraudAssessmentPageDetails,
} from '../../types/fraudAssessment'

import {
    useEffect,
    useRef,
} from 'react'

import {
    getRiskProbabilityPercentage,
} from '../../types/fraudAssessment'

import {
    FraudReviewStatusBadge,
    FraudRiskLevelBadge,
} from './FraudAssessmentStatusBadge'

interface FraudAssessmentTableProps {
    assessments: FraudAssessment[]
    page: FraudAssessmentPageDetails | null
    selectedReference: string | null
    isLoading: boolean
    onSelect: (
        assessment: FraudAssessment,
    ) => void
    onPageChange: (
        pageNumber: number,
    ) => void
}

const currencyFormatter =
    new Intl.NumberFormat(
        'en-ZA',
        {
            style: 'currency',
            currency: 'ZAR',
            maximumFractionDigits: 2,
        }
    )

const dateTimeFormatter =
    new Intl.DateTimeFormat(
        'en-ZA',
        {
            dateStyle: 'medium',
            timeStyle: 'short',
        }
    )

export function FraudAssessmentTable({
                                         assessments,
                                         page,
                                         selectedReference,
                                         isLoading,
                                         onSelect,
                                         onPageChange,
                                     }: FraudAssessmentTableProps) {
    const tableScrollReference =
        useRef<HTMLDivElement | null>(
            null
        )

    useEffect(
        () => {
            tableScrollReference
                .current
                ?.scrollTo({
                    left: 0,
                    behavior: 'auto',
                })
        },
        [
            page?.number,
            assessments,
        ]
    )

    if (
        isLoading
        && assessments.length === 0
    ) {
        return (
            <section
                className={
                    'fraud-queue-card '
                    + 'fraud-queue-loading'
                }
                aria-busy="true"
                aria-label={
                    'Loading fraud assessment queue'
                }
            >
                <div
                    className="fraud-table-loading-row"
                />

                <div
                    className="fraud-table-loading-row"
                />

                <div
                    className="fraud-table-loading-row"
                />
            </section>
        )
    }

    if (
        !isLoading
        && assessments.length === 0
    ) {
        return (
            <section className="fraud-queue-card">
                <div className="empty-state">
                    <h3>
                        No assessments found
                    </h3>

                    <p>
                        No fraud assessments match the
                        selected queue filters. Reset the
                        filters or broaden the search
                        criteria.
                    </p>
                </div>
            </section>
        )
    }

    const currentPage =
        page?.number ?? 0

    const totalPages =
        page?.totalPages ?? 0

    const totalElements =
        page?.totalElements
        ?? assessments.length

    const hasPreviousPage =
        currentPage > 0

    const hasNextPage =
        currentPage + 1 < totalPages

    return (
        <section
            className="fraud-queue-card"
            aria-labelledby={
                'fraud-assessment-queue-heading'
            }
        >
            <div className="fraud-queue-heading">
                <div>
                    <span className="eyebrow">
                        Analyst work queue
                    </span>

                    <h2
                        id={
                            'fraud-assessment-queue-heading'
                        }
                    >
                        Fraud assessments
                    </h2>
                </div>

                <span className="fraud-result-count">
                    {totalElements}
                    {' '}
                    {totalElements === 1
                        ? 'assessment'
                        : 'assessments'}
                </span>
            </div>

            <div
                ref={tableScrollReference}
                className="fraud-table-scroll"
                aria-busy={isLoading}
            >
                <table className="fraud-table">
                    <thead>
                    <tr>
                        <th scope="col">
                            Assessment
                        </th>

                        <th scope="col">
                            Supplier
                        </th>

                        <th scope="col">
                            Invoice amount
                        </th>

                        <th scope="col">
                            Risk
                        </th>

                        <th scope="col">
                            Probability
                        </th>

                        <th scope="col">
                            Review status
                        </th>

                        <th scope="col">
                            Assessed
                        </th>

                        <th scope="col">
                            Action
                        </th>
                    </tr>
                    </thead>

                    <tbody>
                    {assessments.map(
                        (assessment) => {
                            const isSelected =
                                assessment
                                    .assessmentReference
                                === selectedReference

                            return (
                                <tr
                                    key={
                                        assessment
                                            .assessmentReference
                                    }
                                    className={
                                        isSelected
                                            ? 'is-selected'
                                            : undefined
                                    }
                                >
                                    <td>
                                        <strong
                                            className={
                                                'fraud-assessment-reference'
                                            }
                                        >
                                            {
                                                assessment
                                                    .assessmentReference
                                            }
                                        </strong>

                                        <span
                                            className={
                                                'fraud-table-secondary'
                                            }
                                        >
                                                {
                                                    assessment
                                                        .invoiceReference
                                                }
                                            </span>
                                    </td>

                                    <td>
                                            <span>
                                                {
                                                    assessment
                                                        .supplierId
                                                    ?? 'Not supplied'
                                                }
                                            </span>

                                        <span
                                            className={
                                                'fraud-table-secondary'
                                            }
                                        >
                                                {
                                                    assessment
                                                        .departmentId
                                                    ?? 'No department'
                                                }
                                            </span>
                                    </td>

                                    <td>
                                        <strong>
                                            {
                                                formatInvoiceAmount(
                                                    assessment
                                                        .invoiceAmount
                                                )
                                            }
                                        </strong>
                                    </td>

                                    <td>
                                        <FraudRiskLevelBadge
                                            riskLevel={
                                                assessment
                                                    .riskLevel
                                            }
                                        />
                                    </td>

                                    <td>
                                            <span
                                                className={
                                                    'fraud-probability'
                                                }
                                            >
                                                {
                                                    formatProbability(
                                                        assessment
                                                            .riskProbability
                                                    )
                                                }
                                            </span>
                                    </td>

                                    <td>
                                        <FraudReviewStatusBadge
                                            status={
                                                assessment
                                                    .reviewStatus
                                            }
                                        />
                                    </td>

                                    <td>
                                        <time
                                            dateTime={
                                                assessment
                                                    .assessedAt
                                            }
                                        >
                                            {
                                                formatDateTime(
                                                    assessment
                                                        .assessedAt
                                                )
                                            }
                                        </time>
                                    </td>

                                    <td>
                                        <button
                                            className={
                                                'secondary-button '
                                                + 'fraud-review-button'
                                            }
                                            type="button"
                                            onClick={
                                                () =>
                                                    onSelect(
                                                        assessment
                                                    )
                                            }
                                        >
                                            {isSelected
                                                ? 'Selected'
                                                : 'Review'}
                                        </button>
                                    </td>
                                </tr>
                            )
                        }
                    )}
                    </tbody>
                </table>
            </div>

            <div className="fraud-pagination">
                <button
                    className="secondary-button"
                    type="button"
                    onClick={
                        () =>
                            onPageChange(
                                currentPage - 1
                            )
                    }
                    disabled={
                        isLoading
                        || !hasPreviousPage
                    }
                >
                    Previous
                </button>

                <span>
                    Page
                    {' '}
                    <strong>
                        {totalPages === 0
                            ? 0
                            : currentPage + 1}
                    </strong>
                    {' '}
                    of
                    {' '}
                    <strong>
                        {totalPages}
                    </strong>
                </span>

                <button
                    className="secondary-button"
                    type="button"
                    onClick={
                        () =>
                            onPageChange(
                                currentPage + 1
                            )
                    }
                    disabled={
                        isLoading
                        || !hasNextPage
                    }
                >
                    Next
                </button>
            </div>
        </section>
    )
}

function formatInvoiceAmount(
    amount: number,
): string {
    if (!Number.isFinite(amount)) {
        return 'Unavailable'
    }

    return currencyFormatter.format(
        amount
    )
}

function formatProbability(
    probability: number,
): string {
    if (!Number.isFinite(probability)) {
        return 'Unavailable'
    }

    return (
        getRiskProbabilityPercentage(
            probability
        )
            .toFixed(2)
        + '%'
    )
}

function formatDateTime(
    value: string,
): string {
    const parsedDate =
        new Date(value)

    if (
        Number.isNaN(
            parsedDate.getTime()
        )
    ) {
        return 'Unavailable'
    }

    return dateTimeFormatter.format(
        parsedDate
    )
}