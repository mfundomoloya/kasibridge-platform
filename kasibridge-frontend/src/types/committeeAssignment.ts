import type {
    ProcurementTenderStatus,
} from './procurementTender'

export type CommitteeRole =
    | 'SPECIFICATION_OFFICER'
    | 'EVALUATOR'
    | 'ADJUDICATOR'

export interface CommitteeAssignment {
    id: number
    tenderId: number
    userId: number
    committeeRole: CommitteeRole
    assignedByUserId: number
    active: boolean
    assignedAt: string
    reason: string | null
}

export interface CurrentUserCommitteeAssignment {
    assignmentId: number

    tenderId: number
    tenderReference: string
    tenderTitle: string
    buyerOrgId: string
    budgetAmount: number
    tenderStatus: ProcurementTenderStatus

    committeeRole: CommitteeRole

    userId: number
    assignedByUserId: number
    assignedAt: string
    assignmentReason: string | null
    active: boolean

    tenderPublishedAt: string | null
    tenderClosedAt: string | null
    tenderUpdatedAt: string
}

export interface CommitteeAssignmentQueueStatistics {
    total: number
    activeWork: number
    awaitingEvaluation: number
    awaitingAdjudication: number
    completed: number
    unavailable: number
}

export const COMMITTEE_ROLE_LABELS:
    Record<CommitteeRole, string> = {
    SPECIFICATION_OFFICER:
        'Specification Officer',
    EVALUATOR:
        'Evaluator',
    ADJUDICATOR:
        'Adjudicator',
}

export function isEvaluationAssignmentReady(
    assignment:
    CurrentUserCommitteeAssignment,
): boolean {
    return (
        assignment.committeeRole
        === 'EVALUATOR'
        && assignment.tenderStatus
        === 'EVALUATION'
        && assignment.active
    )
}

export function isEvaluationAssignmentWaiting(
    assignment:
    CurrentUserCommitteeAssignment,
): boolean {
    return (
        assignment.committeeRole
        === 'EVALUATOR'
        && (
            assignment.tenderStatus
            === 'DRAFT'
            || assignment.tenderStatus
            === 'PUBLISHED'
            || assignment.tenderStatus
            === 'BIDDING_CLOSED'
        )
        && assignment.active
    )
}

export function isAdjudicationAssignmentReady(
    assignment:
    CurrentUserCommitteeAssignment,
): boolean {
    return (
        assignment.committeeRole
        === 'ADJUDICATOR'
        && assignment.tenderStatus
        === 'ADJUDICATION'
        && assignment.active
    )
}

export function isCommitteeAssignmentCompleted(
    assignment:
    CurrentUserCommitteeAssignment,
): boolean {
    return (
        assignment.tenderStatus
        === 'AWARDED'
        || assignment.tenderStatus
        === 'CANCELLED'
    )
}

export function canOpenEvaluationWorkspace(
    assignment:
    CurrentUserCommitteeAssignment,
): boolean {
    return isEvaluationAssignmentReady(
        assignment
    )
}

export function canOpenAdjudicationWorkspace(
    assignment:
    CurrentUserCommitteeAssignment,
): boolean {
    return isAdjudicationAssignmentReady(
        assignment
    )
}

export function calculateCommitteeQueueStatistics(
    assignments:
    CurrentUserCommitteeAssignment[],
): CommitteeAssignmentQueueStatistics {
    const statistics:
        CommitteeAssignmentQueueStatistics = {
        total: assignments.length,
        activeWork: 0,
        awaitingEvaluation: 0,
        awaitingAdjudication: 0,
        completed: 0,
        unavailable: 0,
    }

    for (const assignment of assignments) {
        if (!assignment.active) {
            statistics.unavailable += 1
            continue
        }

        if (
            isCommitteeAssignmentCompleted(
                assignment
            )
        ) {
            statistics.completed += 1
            continue
        }

        if (
            isEvaluationAssignmentReady(
                assignment
            )
        ) {
            statistics.activeWork += 1
            statistics.awaitingEvaluation += 1
            continue
        }

        if (
            isAdjudicationAssignmentReady(
                assignment
            )
        ) {
            statistics.activeWork += 1
            statistics.awaitingAdjudication += 1
            continue
        }

        if (
            isEvaluationAssignmentWaiting(
                assignment
            )
        ) {
            continue
        }

        statistics.unavailable += 1
    }

    return statistics
}

export function getCommitteeAssignmentStateLabel(
    assignment:
    CurrentUserCommitteeAssignment,
): string {
    if (!assignment.active) {
        return 'Assignment inactive'
    }

    if (
        isCommitteeAssignmentCompleted(
            assignment
        )
    ) {
        return assignment.tenderStatus
        === 'AWARDED'
            ? 'Award completed'
            : 'Tender cancelled'
    }

    if (
        isEvaluationAssignmentReady(
            assignment
        )
    ) {
        return 'Ready for evaluation'
    }

    if (
        isAdjudicationAssignmentReady(
            assignment
        )
    ) {
        return 'Ready for adjudication'
    }

    if (
        isEvaluationAssignmentWaiting(
            assignment
        )
    ) {
        return 'Awaiting evaluation stage'
    }

    if (
        assignment.committeeRole
        === 'ADJUDICATOR'
    ) {
        return 'Awaiting adjudication stage'
    }

    return 'No action available'
}