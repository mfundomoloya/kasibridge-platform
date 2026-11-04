import {
    Link,
} from 'react-router-dom'

import {
    ProcurementTenderStatusBadge,
} from './ProcurementTenderStatusBadge'

import type {
    CurrentUserCommitteeAssignment,
} from '../../types/committeeAssignment'

import {
    canOpenAdjudicationWorkspace,
    canOpenEvaluationWorkspace,
    COMMITTEE_ROLE_LABELS,
    getCommitteeAssignmentStateLabel,
    isCommitteeAssignmentCompleted,
} from '../../types/committeeAssignment'

interface CommitteeAssignmentCardProps {
    assignment:
        CurrentUserCommitteeAssignment
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

export function CommitteeAssignmentCard({
                                            assignment,
                                        }: CommitteeAssignmentCardProps) {
    const canOpenEvaluation =
        canOpenEvaluationWorkspace(
            assignment
        )

    const canOpenAdjudication =
        canOpenAdjudicationWorkspace(
            assignment
        )

    const isCompleted =
        isCommitteeAssignmentCompleted(
            assignment
        )

    const stateLabel =
        getCommitteeAssignmentStateLabel(
            assignment
        )

    return (
        <article
            className={
                'committee-assignment-card '
                + getAssignmentCardClassName(
                    assignment
                )
            }
        >
            <header
                className={
                    'committee-assignment-header'
                }
            >
                <div>
                    <span
                        className={
                            'committee-assignment-reference'
                        }
                    >
                        {
                            assignment
                                .tenderReference
                        }
                    </span>

                    <h2>
                        {
                            assignment
                                .tenderTitle
                        }
                    </h2>
                </div>

                <div
                    className={
                        'committee-assignment-badges'
                    }
                >
                    <span
                        className={
                            'committee-role-badge '
                            + getCommitteeRoleClassName(
                                assignment
                                    .committeeRole
                            )
                        }
                    >
                        {
                            COMMITTEE_ROLE_LABELS[
                                assignment
                                    .committeeRole
                                ]
                        }
                    </span>

                    <ProcurementTenderStatusBadge
                        status={
                            assignment
                                .tenderStatus
                        }
                    />
                </div>
            </header>

            <div
                className={
                    'committee-assignment-state'
                }
            >
                <span>
                    Assignment state
                </span>

                <strong>
                    {stateLabel}
                </strong>
            </div>

            <dl
                className={
                    'committee-assignment-summary'
                }
            >
                <SummaryItem
                    label="Buyer organisation"
                    value={
                        assignment.buyerOrgId
                    }
                />

                <SummaryItem
                    label="Tender budget"
                    value={
                        formatCurrency(
                            assignment
                                .budgetAmount
                        )
                    }
                />

                <SummaryItem
                    label="Assigned"
                    value={
                        formatDateTime(
                            assignment
                                .assignedAt
                        )
                    }
                />

                <SummaryItem
                    label="Assigned by user"
                    value={
                        assignment
                            .assignedByUserId
                            .toString()
                    }
                />

                <SummaryItem
                    label="Tender published"
                    value={
                        assignment
                            .tenderPublishedAt
                        === null
                            ? 'Not published'
                            : formatDateTime(
                                assignment
                                    .tenderPublishedAt
                            )
                    }
                />

                <SummaryItem
                    label="Bidding closed"
                    value={
                        assignment
                            .tenderClosedAt
                        === null
                            ? 'Not closed'
                            : formatDateTime(
                                assignment
                                    .tenderClosedAt
                            )
                    }
                />
            </dl>

            <section
                className={
                    'committee-assignment-reason'
                }
                aria-label={
                    'Committee assignment reason'
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
            </section>

            <footer
                className={
                    'committee-assignment-actions'
                }
            >
                {canOpenEvaluation && (
                    <Link
                        className={
                            'primary-link-button'
                        }
                        to={
                            '/procurement/'
                            + 'evaluations/'
                            + assignment.tenderId
                        }
                    >
                        Open evaluation workspace
                    </Link>
                )}

                {canOpenAdjudication && (
                    <Link
                        className={
                            'primary-link-button'
                        }
                        to={
                            '/procurement/'
                            + 'adjudications/'
                            + assignment.tenderId
                        }
                    >
                        Open adjudication workspace
                    </Link>
                )}

                {!canOpenEvaluation
                    && !canOpenAdjudication && (
                        <span
                            className={
                                'committee-action-message'
                            }
                        >
                        {
                            getUnavailableActionMessage(
                                assignment,
                                isCompleted
                            )
                        }
                    </span>
                    )}
            </footer>
        </article>
    )
}

interface SummaryItemProps {
    label: string
    value: string
}

function SummaryItem({
                         label,
                         value,
                     }: SummaryItemProps) {
    return (
        <div>
            <dt>{label}</dt>
            <dd>{value}</dd>
        </div>
    )
}

function getAssignmentCardClassName(
    assignment:
    CurrentUserCommitteeAssignment,
): string {
    if (!assignment.active) {
        return 'committee-assignment-inactive'
    }

    if (
        isCommitteeAssignmentCompleted(
            assignment
        )
    ) {
        return 'committee-assignment-completed'
    }

    if (
        canOpenEvaluationWorkspace(
            assignment
        )
        || canOpenAdjudicationWorkspace(
            assignment
        )
    ) {
        return 'committee-assignment-actionable'
    }

    return 'committee-assignment-waiting'
}

function getCommitteeRoleClassName(
    role:
    CurrentUserCommitteeAssignment[
        'committeeRole'
        ],
): string {
    switch (role) {
        case 'SPECIFICATION_OFFICER':
            return 'committee-role-specification'

        case 'EVALUATOR':
            return 'committee-role-evaluator'

        case 'ADJUDICATOR':
            return 'committee-role-adjudicator'
    }
}

function getUnavailableActionMessage(
    assignment:
    CurrentUserCommitteeAssignment,
    isCompleted: boolean,
): string {
    if (!assignment.active) {
        return (
            'This committee assignment '
            + 'is no longer active.'
        )
    }

    if (isCompleted) {
        return (
            'This tender has reached a '
            + 'final workflow state.'
        )
    }

    if (
        assignment.committeeRole
        === 'EVALUATOR'
    ) {
        if (
            assignment.tenderStatus
            === 'ADJUDICATION'
        ) {
            return (
                'Evaluation has been completed '
                + 'and the tender is now under '
                + 'adjudication.'
            )
        }

        return (
            'The evaluation workspace becomes '
            + 'available when the tender enters '
            + 'the Evaluation stage.'
        )
    }

    if (
        assignment.committeeRole
        === 'ADJUDICATOR'
    ) {
        return (
            'The adjudication workspace becomes '
            + 'available when the tender enters '
            + 'the Adjudication stage.'
        )
    }

    return (
        'No committee action is available '
        + 'for the current tender state.'
    )
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