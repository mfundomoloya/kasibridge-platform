import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from 'react'

import {
    Link,
} from 'react-router-dom'

import {
    getCommitteeAssignmentApiErrorMessage,
    getMyAdjudicatorAssignmentQueue,
} from '../api/committeeAssignmentApi'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import {
    ProcurementTenderStatusBadge,
} from '../components/procurement/ProcurementTenderStatusBadge'

import type {
    CurrentUserCommitteeAssignment,
} from '../types/committeeAssignment'

import './SpecificationTenderPage.css'

type AdjudicationQueueCategory =
    | 'READY'
    | 'WAITING'
    | 'COMPLETED'
    | 'UNAVAILABLE'

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

export function AdjudicatorAssignmentQueuePage() {
    const [
        assignments,
        setAssignments,
    ] =
        useState<
            CurrentUserCommitteeAssignment[]
        >([])

    const [
        isLoading,
        setIsLoading,
    ] = useState(true)

    const [
        isRefreshing,
        setIsRefreshing,
    ] = useState(false)

    const [
        errorMessage,
        setErrorMessage,
    ] =
        useState<string | null>(
            null
        )

    const loadAssignments =
        useCallback(
            async (
                manualRefresh:
                boolean = false,
            ): Promise<void> => {
                if (manualRefresh) {
                    setIsRefreshing(true)
                } else {
                    setIsLoading(true)
                }

                setErrorMessage(null)

                try {
                    const response =
                        await getMyAdjudicatorAssignmentQueue()

                    setAssignments(
                        response.filter(
                            (assignment) =>
                                assignment.active
                                && assignment
                                    .committeeRole
                                === 'ADJUDICATOR'
                        )
                    )
                } catch (error) {
                    setErrorMessage(
                        getCommitteeAssignmentApiErrorMessage(
                            error,
                            'Unable to load the '
                            + 'Adjudicator '
                            + 'assignment queue.'
                        )
                    )
                } finally {
                    setIsLoading(false)
                    setIsRefreshing(false)
                }
            },
            []
        )

    useEffect(
        () => {
            queueMicrotask(
                () => {
                    void loadAssignments()
                }
            )
        },
        [
            loadAssignments,
        ]
    )

    const groupedAssignments =
        useMemo(
            () =>
                groupAssignments(
                    assignments
                ),
            [
                assignments,
            ]
        )

    return (
        <ProcurementLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Bid Adjudication Committee
                    </span>

                    <h1>
                        My adjudications
                    </h1>

                    <p>
                        Review tender assignments,
                        open completed evaluation
                        rankings and record final
                        award decisions for tenders
                        assigned to the authenticated
                        Adjudicator.
                    </p>
                </div>

                <button
                    className="secondary-button"
                    type="button"
                    onClick={
                        () => {
                            void loadAssignments(
                                true
                            )
                        }
                    }
                    disabled={
                        isRefreshing
                        || isLoading
                    }
                >
                    {isRefreshing
                        ? 'Refreshing...'
                        : 'Refresh assignments'}
                </button>
            </section>

            {errorMessage !== null && (
                <p
                    className={
                        'form-error-message '
                        + 'page-message'
                    }
                    role="alert"
                >
                    {errorMessage}
                </p>
            )}

            <section
                className={
                    'statistics-grid '
                    + 'adjudication-queue-statistics'
                }
                aria-label={
                    'Adjudication assignment '
                    + 'statistics'
                }
            >
                <AdjudicationQueueStatistic
                    label="Total assignments"
                    value={
                        assignments.length
                    }
                    description={
                        'Active Adjudicator '
                        + 'assignments for the '
                        + 'authenticated user.'
                    }
                    tone="default"
                />

                <AdjudicationQueueStatistic
                    label="Ready"
                    value={
                        groupedAssignments
                            .READY
                            .length
                    }
                    description={
                        'Tenders with completed '
                        + 'evaluation evidence ready '
                        + 'for adjudication.'
                    }
                    tone="positive"
                />

                <AdjudicationQueueStatistic
                    label="Waiting"
                    value={
                        groupedAssignments
                            .WAITING
                            .length
                    }
                    description={
                        'Assigned tenders that have '
                        + 'not yet entered the '
                        + 'Adjudication stage.'
                    }
                    tone="warning"
                />

                <AdjudicationQueueStatistic
                    label="Completed"
                    value={
                        groupedAssignments
                            .COMPLETED
                            .length
                    }
                    description={
                        'Assigned tenders with a '
                        + 'recorded final award.'
                    }
                    tone="info"
                />
            </section>

            {isLoading ? (
                <AdjudicationQueueLoadingState />
            ) : assignments.length === 0 ? (
                <AdjudicationQueueEmptyState />
            ) : (
                <div
                    className={
                        'adjudication-queue-sections'
                    }
                >
                    <AdjudicationQueueSection
                        category="READY"
                        title={
                            'Ready for adjudication'
                        }
                        description={
                            'Evaluation is complete '
                            + 'and the tender is ready '
                            + 'for ranking review and '
                            + 'a final award decision.'
                        }
                        assignments={
                            groupedAssignments.READY
                        }
                    />

                    <AdjudicationQueueSection
                        category="WAITING"
                        title={
                            'Awaiting adjudication'
                        }
                        description={
                            'These assigned tenders '
                            + 'have not yet entered '
                            + 'the Adjudication stage.'
                        }
                        assignments={
                            groupedAssignments.WAITING
                        }
                    />

                    <AdjudicationQueueSection
                        category="COMPLETED"
                        title={
                            'Completed adjudications'
                        }
                        description={
                            'These tenders have '
                            + 'already been awarded. '
                            + 'The adjudication '
                            + 'evidence remains '
                            + 'available read-only.'
                        }
                        assignments={
                            groupedAssignments
                                .COMPLETED
                        }
                    />

                    <AdjudicationQueueSection
                        category="UNAVAILABLE"
                        title={
                            'Unavailable assignments'
                        }
                        description={
                            'These assigned tenders '
                            + 'cannot proceed through '
                            + 'adjudication.'
                        }
                        assignments={
                            groupedAssignments
                                .UNAVAILABLE
                        }
                    />
                </div>
            )}

            <AdjudicationQueueGovernanceNotice />
        </ProcurementLayout>
    )
}

interface AdjudicationQueueStatisticProps {
    label: string
    value: number
    description: string
    tone:
        | 'default'
        | 'positive'
        | 'warning'
        | 'info'
}

function AdjudicationQueueStatistic({
                                        label,
                                        value,
                                        description,
                                        tone,
                                    }: AdjudicationQueueStatisticProps) {
    return (
        <article
            className={
                'statistic-card '
                + 'procurement-dashboard-stat '
                + 'procurement-stat-'
                + tone
            }
        >
            <span>
                {label}
            </span>

            <strong>
                {value}
            </strong>

            <p>
                {description}
            </p>
        </article>
    )
}

interface AdjudicationQueueSectionProps {
    category:
        AdjudicationQueueCategory
    title: string
    description: string
    assignments:
        CurrentUserCommitteeAssignment[]
}

function AdjudicationQueueSection({
                                      category,
                                      title,
                                      description,
                                      assignments,
                                  }: AdjudicationQueueSectionProps) {
    if (assignments.length === 0) {
        return null
    }

    return (
        <section
            className={
                'content-card '
                + 'adjudication-queue-section'
            }
            aria-labelledby={
                'adjudication-section-'
                + category
                    .toLowerCase()
            }
        >
            <div
                className={
                    'adjudication-queue-'
                    + 'section-heading'
                }
            >
                <div>
                    <span className="eyebrow">
                        {getCategoryLabel(
                            category
                        )}
                    </span>

                    <h2
                        id={
                            'adjudication-section-'
                            + category
                                .toLowerCase()
                        }
                    >
                        {title}
                    </h2>

                    <p>
                        {description}
                    </p>
                </div>

                <span
                    className={
                        'adjudication-queue-count'
                    }
                >
                    {assignments.length}
                </span>
            </div>

            <div
                className={
                    'adjudication-assignment-list'
                }
            >
                {assignments.map(
                    (assignment) => (
                        <AdjudicationAssignmentCard
                            key={
                                assignment.assignmentId
                            }
                            assignment={
                                assignment
                            }
                            category={
                                category
                            }
                        />
                    )
                )}
            </div>
        </section>
    )
}

interface AdjudicationAssignmentCardProps {
    assignment:
        CurrentUserCommitteeAssignment
    category:
        AdjudicationQueueCategory
}

function AdjudicationAssignmentCard({
                                        assignment,
                                        category,
                                    }: AdjudicationAssignmentCardProps) {
    const workspaceAvailable =
        category === 'READY'
        || category === 'COMPLETED'

    return (
        <article
            className={
                'adjudication-assignment-card '
                + 'is-'
                + category.toLowerCase()
            }
        >
            <header
                className={
                    'adjudication-assignment-header'
                }
            >
                <div>
                    <span
                        className={
                            'specification-'
                            + 'tender-reference'
                        }
                    >
                        {
                            assignment
                                .tenderReference
                        }
                    </span>

                    <h3>
                        {
                            assignment
                                .tenderTitle
                        }
                    </h3>
                </div>

                <ProcurementTenderStatusBadge
                    status={
                        assignment
                            .tenderStatus
                    }
                />
            </header>

            <dl
                className={
                    'adjudication-assignment-details'
                }
            >
                <AdjudicationAssignmentDetail
                    label="Tender ID"
                    value={
                        assignment
                            .tenderId
                            .toString()
                    }
                />

                <AdjudicationAssignmentDetail
                    label="Buyer organisation"
                    value={
                        assignment
                            .buyerOrgId
                    }
                />

                <AdjudicationAssignmentDetail
                    label="Tender budget"
                    value={
                        formatCurrency(
                            assignment
                                .budgetAmount
                        )
                    }
                />

                <AdjudicationAssignmentDetail
                    label="Assigned"
                    value={
                        formatDateTime(
                            assignment
                                .assignedAt
                        )
                    }
                />
            </dl>

            <div
                className={
                    'adjudication-assignment-reason'
                }
            >
                <span>
                    Assignment reason
                </span>

                <p>
                    {
                        assignment
                            .assignmentReason
                        ?? (
                            'No assignment reason '
                            + 'was recorded.'
                        )
                    }
                </p>
            </div>

            <div
                className={
                    'adjudication-assignment-footer'
                }
            >
                <span
                    className={
                        'adjudication-queue-state '
                        + 'is-'
                        + category.toLowerCase()
                    }
                >
                    {getCategoryStatusText(
                        category
                    )}
                </span>

                {workspaceAvailable ? (
                    <Link
                        className={
                            category === 'READY'
                                ? (
                                    'primary-link-button'
                                )
                                : (
                                    'secondary-link-button'
                                )
                        }
                        to={
                            '/procurement/'
                            + 'adjudications/'
                            + assignment
                                .tenderId
                        }
                    >
                        {category === 'READY'
                            ? (
                                'Open adjudication '
                                + 'workspace'
                            )
                            : (
                                'View award evidence'
                            )}
                    </Link>
                ) : (
                    <span
                        className={
                            'adjudication-queue-'
                            + 'unavailable-message'
                        }
                    >
                        {getUnavailableMessage(
                            assignment
                                .tenderStatus
                        )}
                    </span>
                )}
            </div>
        </article>
    )
}

interface AdjudicationAssignmentDetailProps {
    label: string
    value: string
}

function AdjudicationAssignmentDetail({
                                          label,
                                          value,
                                      }: AdjudicationAssignmentDetailProps) {
    return (
        <div>
            <dt>
                {label}
            </dt>

            <dd>
                {value}
            </dd>
        </div>
    )
}

function AdjudicationQueueLoadingState() {
    return (
        <section
            className={
                'content-card '
                + 'adjudication-queue-loading'
            }
            aria-busy="true"
        >
            <div className="empty-state">
                <h2>
                    Loading adjudications
                </h2>

                <p>
                    Retrieving the authenticated
                    Adjudicator’s active tender
                    assignments.
                </p>
            </div>
        </section>
    )
}

function AdjudicationQueueEmptyState() {
    return (
        <section className="content-card">
            <div className="empty-state">
                <h2>
                    No adjudication assignments
                </h2>

                <p>
                    The authenticated user does
                    not currently have an active
                    Adjudicator assignment.
                </p>
            </div>
        </section>
    )
}

function AdjudicationQueueGovernanceNotice() {
    return (
        <section
            className={
                'content-card '
                + 'adjudication-queue-governance'
            }
            aria-labelledby={
                'adjudication-queue-'
                + 'governance-heading'
            }
        >
            <div>
                <span className="eyebrow">
                    Segregation of duties
                </span>

                <h2
                    id={
                        'adjudication-queue-'
                        + 'governance-heading'
                    }
                >
                    Adjudicator responsibilities
                </h2>
            </div>

            <ul>
                <li>
                    Review only tenders explicitly
                    assigned to the authenticated
                    Adjudicator.
                </li>

                <li>
                    Use completed evaluation
                    evidence and the published
                    tender criteria.
                </li>

                <li>
                    Do not change technical,
                    price, or total scores.
                </li>

                <li>
                    Record a clear reason for the
                    final award decision.
                </li>

                <li>
                    Do not attempt to identify the
                    Trader behind an anonymised
                    bidder alias.
                </li>
            </ul>
        </section>
    )
}

function groupAssignments(
    assignments:
    CurrentUserCommitteeAssignment[],
): Record<
    AdjudicationQueueCategory,
    CurrentUserCommitteeAssignment[]
> {
    const groups:
        Record<
            AdjudicationQueueCategory,
            CurrentUserCommitteeAssignment[]
        > = {
        READY: [],
        WAITING: [],
        COMPLETED: [],
        UNAVAILABLE: [],
    }

    for (
        const assignment
        of assignments
        ) {
        const category =
            getAssignmentCategory(
                assignment
                    .tenderStatus
            )

        groups[category].push(
            assignment
        )
    }

    return groups
}

function getAssignmentCategory(
    tenderStatus: string,
): AdjudicationQueueCategory {
    switch (tenderStatus) {
        case 'ADJUDICATION':
            return 'READY'

        case 'AWARDED':
            return 'COMPLETED'

        case 'CANCELLED':
            return 'UNAVAILABLE'

        case 'DRAFT':
        case 'PUBLISHED':
        case 'BIDDING_CLOSED':
        case 'EVALUATION':
        default:
            return 'WAITING'
    }
}

function getCategoryLabel(
    category:
    AdjudicationQueueCategory,
): string {
    switch (category) {
        case 'READY':
            return 'Action required'

        case 'WAITING':
            return 'Pending workflow'

        case 'COMPLETED':
            return 'Completed work'

        case 'UNAVAILABLE':
            return 'Unavailable'
    }
}

function getCategoryStatusText(
    category:
    AdjudicationQueueCategory,
): string {
    switch (category) {
        case 'READY':
            return 'Ready for adjudication'

        case 'WAITING':
            return 'Awaiting adjudication stage'

        case 'COMPLETED':
            return 'Award completed'

        case 'UNAVAILABLE':
            return 'Adjudication unavailable'
    }
}

function getUnavailableMessage(
    tenderStatus: string,
): string {
    switch (tenderStatus) {
        case 'DRAFT':
            return (
                'Tender specification is '
                + 'still in draft.'
            )

        case 'PUBLISHED':
            return (
                'Bidding remains open.'
            )

        case 'BIDDING_CLOSED':
            return (
                'Evaluation has not started.'
            )

        case 'EVALUATION':
            return (
                'Evaluator scoring is still '
                + 'in progress.'
            )

        case 'CANCELLED':
            return (
                'The tender was cancelled.'
            )

        default:
            return (
                'The adjudication workspace '
                + 'is not currently available.'
            )
    }
}

function formatCurrency(
    amount: number,
): string {
    if (!Number.isFinite(amount)) {
        return 'Unavailable'
    }

    return currencyFormatter.format(
        amount
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