import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from 'react'

import {
    getCommitteeAssignmentApiErrorMessage,
    getMyEvaluatorAssignmentQueue,
} from '../api/committeeAssignmentApi'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import {
    CommitteeAssignmentCard,
} from '../components/procurement/CommitteeAssignmentCard'

import type {
    CurrentUserCommitteeAssignment,
} from '../types/committeeAssignment'

import {
    calculateCommitteeQueueStatistics,
    isCommitteeAssignmentCompleted,
    isEvaluationAssignmentReady,
    isEvaluationAssignmentWaiting,
} from '../types/committeeAssignment'

import './SpecificationTenderPage.css'

type EvaluatorQueueFilter =
    | 'ALL'
    | 'ACTIONABLE'
    | 'WAITING'
    | 'COMPLETED'

interface EvaluatorFilterOption {
    value: EvaluatorQueueFilter
    label: string
}

const evaluatorFilterOptions:
    EvaluatorFilterOption[] = [
    {
        value: 'ALL',
        label: 'All assignments',
    },
    {
        value: 'ACTIONABLE',
        label: 'Ready for evaluation',
    },
    {
        value: 'WAITING',
        label: 'Awaiting evaluation',
    },
    {
        value: 'COMPLETED',
        label: 'Completed',
    },
]

export function EvaluatorAssignmentQueuePage() {
    const [
        assignments,
        setAssignments,
    ] =
        useState<
            CurrentUserCommitteeAssignment[]
        >([])

    const [
        selectedFilter,
        setSelectedFilter,
    ] =
        useState<EvaluatorQueueFilter>(
            'ALL'
        )

    const [
        isLoading,
        setIsLoading,
    ] = useState(true)

    const [
        errorMessage,
        setErrorMessage,
    ] =
        useState<string | null>(
            null
        )

    const loadAssignments =
        useCallback(
            async (): Promise<void> => {
                setIsLoading(true)
                setErrorMessage(null)

                try {
                    const response =
                        await getMyEvaluatorAssignmentQueue()

                    setAssignments(response)
                } catch (error) {
                    setErrorMessage(
                        getCommitteeAssignmentApiErrorMessage(
                            error,
                            'Unable to load the '
                            + 'Evaluator assignment '
                            + 'queue.'
                        )
                    )
                } finally {
                    setIsLoading(false)
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

    const statistics =
        useMemo(
            () =>
                calculateCommitteeQueueStatistics(
                    assignments
                ),
            [
                assignments,
            ]
        )

    const actionableAssignments =
        useMemo(
            () =>
                assignments.filter(
                    (assignment) =>
                        isEvaluationAssignmentReady(
                            assignment
                        )
                ),
            [
                assignments,
            ]
        )

    const waitingAssignments =
        useMemo(
            () =>
                assignments.filter(
                    (assignment) =>
                        isEvaluationAssignmentWaiting(
                            assignment
                        )
                        || isEvaluationStageCompleted(
                            assignment
                        )
                ),
            [
                assignments,
            ]
        )

    const completedAssignments =
        useMemo(
            () =>
                assignments.filter(
                    (assignment) =>
                        isCommitteeAssignmentCompleted(
                            assignment
                        )
                ),
            [
                assignments,
            ]
        )

    const visibleAssignments =
        useMemo(
            () => {
                switch (selectedFilter) {
                    case 'ACTIONABLE':
                        return actionableAssignments

                    case 'WAITING':
                        return waitingAssignments

                    case 'COMPLETED':
                        return completedAssignments

                    case 'ALL':
                        return assignments
                }
            },
            [
                selectedFilter,
                assignments,
                actionableAssignments,
                waitingAssignments,
                completedAssignments,
            ]
        )

    function handleFilterSelection(
        filter:
        EvaluatorQueueFilter,
    ): void {
        setSelectedFilter(filter)
    }

    return (
        <ProcurementLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Bid Evaluation Committee
                    </span>

                    <h1>
                        My evaluation assignments
                    </h1>

                    <p>
                        Review only tenders assigned
                        to the authenticated evaluator.
                        Bidder identities remain hidden,
                        and scoring is available only
                        while a tender is in the
                        Evaluation stage.
                    </p>
                </div>

                <button
                    className="secondary-button"
                    type="button"
                    onClick={
                        () => {
                            void loadAssignments()
                        }
                    }
                    disabled={isLoading}
                >
                    {isLoading
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
                    + 'committee-statistics-grid'
                }
                aria-label={
                    'Evaluator assignment statistics'
                }
            >
                <EvaluatorStatisticCard
                    label="Total assignments"
                    value={
                        isLoading
                            ? '...'
                            : statistics.total
                    }
                    description={
                        'Active committee assignments '
                        + 'for the signed-in evaluator.'
                    }
                    tone="info"
                />

                <EvaluatorStatisticCard
                    label="Ready for evaluation"
                    value={
                        isLoading
                            ? '...'
                            : actionableAssignments
                                .length
                    }
                    description={
                        'Assigned tenders currently '
                        + 'open for blind bid scoring.'
                    }
                    tone={
                        actionableAssignments.length
                        > 0
                            ? 'positive'
                            : 'default'
                    }
                />

                <EvaluatorStatisticCard
                    label="Awaiting evaluation"
                    value={
                        isLoading
                            ? '...'
                            : waitingAssignments.length
                    }
                    description={
                        'Assignments not currently '
                        + 'available for scoring.'
                    }
                    tone={
                        waitingAssignments.length > 0
                            ? 'warning'
                            : 'default'
                    }
                />

                <EvaluatorStatisticCard
                    label="Completed"
                    value={
                        isLoading
                            ? '...'
                            : completedAssignments
                                .length
                    }
                    description={
                        'Assigned tenders that reached '
                        + 'a final workflow state.'
                    }
                    tone="info"
                />
            </section>

            <section
                className={
                    'committee-queue-toolbar'
                }
                aria-labelledby={
                    'evaluator-queue-heading'
                }
            >
                <div>
                    <span className="eyebrow">
                        Assignment controls
                    </span>

                    <h2
                        id="evaluator-queue-heading"
                    >
                        Evaluation queue
                    </h2>

                    <p>
                        Filter the queue by current
                        workflow availability. Completed
                        assignments remain visible as
                        read-only evidence.
                    </p>
                </div>

                <div
                    className={
                        'committee-filter-buttons'
                    }
                    aria-label={
                        'Evaluation assignment filters'
                    }
                >
                    {evaluatorFilterOptions.map(
                        (option) => (
                            <button
                                key={option.value}
                                className={
                                    getFilterButtonClassName(
                                        selectedFilter,
                                        option.value
                                    )
                                }
                                type="button"
                                onClick={
                                    () => {
                                        handleFilterSelection(
                                            option.value
                                        )
                                    }
                                }
                                disabled={isLoading}
                                aria-pressed={
                                    selectedFilter
                                    === option.value
                                }
                            >
                                {option.label}
                                {' '}
                                <span>
                                    {
                                        getFilterCount(
                                            option.value,
                                            assignments,
                                            actionableAssignments,
                                            waitingAssignments,
                                            completedAssignments
                                        )
                                    }
                                </span>
                            </button>
                        )
                    )}
                </div>
            </section>

            {isLoading
            && assignments.length === 0 ? (
                <section
                    className={
                        'content-card '
                        + 'committee-loading-card'
                    }
                    aria-busy="true"
                >
                    <div className="empty-state">
                        <h2>
                            Loading assignments
                        </h2>

                        <p>
                            Retrieving the signed-in
                            evaluator’s active committee
                            assignments and tender
                            information.
                        </p>
                    </div>
                </section>
            ) : visibleAssignments.length === 0 ? (
                <section className="content-card">
                    <div className="empty-state">
                        <h2>
                            {
                                getEmptyStateTitle(
                                    selectedFilter
                                )
                            }
                        </h2>

                        <p>
                            {
                                getEmptyStateDescription(
                                    selectedFilter
                                )
                            }
                        </p>
                    </div>
                </section>
            ) : (
                <section
                    className={
                        'committee-assignment-list'
                    }
                    aria-label={
                        'Evaluator assignments'
                    }
                >
                    {visibleAssignments.map(
                        (assignment) => (
                            <CommitteeAssignmentCard
                                key={
                                    assignment
                                        .assignmentId
                                }
                                assignment={
                                    assignment
                                }
                            />
                        )
                    )}
                </section>
            )}

            <section
                className={
                    'content-card '
                    + 'committee-role-guidance'
                }
                aria-labelledby={
                    'evaluation-role-guidance-heading'
                }
            >
                <div>
                    <span className="eyebrow">
                        Segregation of duties
                    </span>

                    <h2
                        id={
                            'evaluation-role-guidance-heading'
                        }
                    >
                        Evaluator responsibilities
                    </h2>
                </div>

                <ul>
                    <li>
                        Review only tenders assigned
                        through an active Evaluation
                        Committee assignment.
                    </li>

                    <li>
                        Evaluate anonymized bids without
                        access to trader identities.
                    </li>

                    <li>
                        Apply only the evaluation
                        criteria published before
                        bidding opened.
                    </li>

                    <li>
                        Record a technical score and
                        evidence-based comments for
                        every evaluated bid.
                    </li>

                    <li>
                        Do not publish specifications,
                        alter bids, adjudicate tender
                        recommendations, or record
                        awards.
                    </li>
                </ul>
            </section>
        </ProcurementLayout>
    )
}

interface EvaluatorStatisticCardProps {
    label: string
    value: number | string
    description: string
    tone:
        | 'default'
        | 'positive'
        | 'warning'
        | 'info'
}

function EvaluatorStatisticCard({
                                    label,
                                    value,
                                    description,
                                    tone,
                                }: EvaluatorStatisticCardProps) {
    return (
        <article
            className={
                'statistic-card '
                + 'procurement-dashboard-stat '
                + `procurement-stat-${tone}`
            }
        >
            <span>{label}</span>

            <strong>{value}</strong>

            <p>{description}</p>
        </article>
    )
}

function isEvaluationStageCompleted(
    assignment:
    CurrentUserCommitteeAssignment,
): boolean {
    return (
        assignment.active
        && assignment.committeeRole
        === 'EVALUATOR'
        && assignment.tenderStatus
        === 'ADJUDICATION'
    )
}

function getFilterButtonClassName(
    selectedFilter:
    EvaluatorQueueFilter,
    optionValue:
    EvaluatorQueueFilter,
): string {
    if (selectedFilter === optionValue) {
        return (
            'committee-filter-button '
            + 'is-active'
        )
    }

    return 'committee-filter-button'
}

function getFilterCount(
    filter:
    EvaluatorQueueFilter,
    allAssignments:
    CurrentUserCommitteeAssignment[],
    actionableAssignments:
    CurrentUserCommitteeAssignment[],
    waitingAssignments:
    CurrentUserCommitteeAssignment[],
    completedAssignments:
    CurrentUserCommitteeAssignment[],
): number {
    switch (filter) {
        case 'ALL':
            return allAssignments.length

        case 'ACTIONABLE':
            return actionableAssignments.length

        case 'WAITING':
            return waitingAssignments.length

        case 'COMPLETED':
            return completedAssignments.length
    }
}

function getEmptyStateTitle(
    filter:
    EvaluatorQueueFilter,
): string {
    switch (filter) {
        case 'ALL':
            return 'No evaluator assignments'

        case 'ACTIONABLE':
            return 'No tenders ready for evaluation'

        case 'WAITING':
            return 'No waiting assignments'

        case 'COMPLETED':
            return 'No completed assignments'
    }
}

function getEmptyStateDescription(
    filter:
    EvaluatorQueueFilter,
): string {
    switch (filter) {
        case 'ALL':
            return (
                'The authenticated evaluator does '
                + 'not currently have an active '
                + 'committee assignment.'
            )

        case 'ACTIONABLE':
            return (
                'No assigned tender is currently '
                + 'in the Evaluation stage.'
            )

        case 'WAITING':
            return (
                'No assigned tender is waiting for '
                + 'the Evaluation stage or a later '
                + 'committee transition.'
            )

        case 'COMPLETED':
            return (
                'No assigned tender has reached an '
                + 'Awarded or Cancelled state.'
            )
    }
}