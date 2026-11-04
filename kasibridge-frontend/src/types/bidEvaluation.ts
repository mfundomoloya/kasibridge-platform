import type {
    ProcurementTender,
} from './procurementTender'

export interface BlindEvaluationBid {
    bidId: number
    tenderId: number
    bidderAlias: string
    technicalProposal: string
}

export interface EvaluateBidRequest {
    technicalScore: number
    comments: string
}

export interface BidEvaluation {
    id: number
    tenderId: number
    bidId: number
    evaluatorUserId: number
    technicalScore: number
    priceScore: number
    totalScore: number
    comments: string
    submittedAt: string
}

export interface TenderBidActivity {
    tenderId: number
    tenderReference: string
    totalBids: number
    activeBids: number
    compliantBids: number
    complianceFailedBids: number
    withdrawnBids: number
    underEvaluationBids: number
    lastBidSubmittedAt: string | null
}

export interface EvaluationWorkspace {
    tender: ProcurementTender
    bids: BlindEvaluationBid[]
}

export interface BidEvaluationFormState {
    technicalScore: string
    comments: string
}

export interface BidEvaluationFormErrors {
    technicalScore?: string
    comments?: string
    form?: string
}

export interface EvaluationFieldError {
    field: string
    message: string
}

export interface EvaluationApiErrorResponse {
    status?: number
    error?: string
    message?: string
    timestamp?: string
    fieldErrors?: EvaluationFieldError[] | null
}

export type EvaluationBidStatus =
    | 'NOT_SCORED'
    | 'SUBMITTING'
    | 'SCORED'
    | 'FAILED'

export interface EvaluationBidState {
    bid: BlindEvaluationBid
    status: EvaluationBidStatus
    evaluation: BidEvaluation | null
    errorMessage: string | null
}

export interface EvaluationWorkspaceStatistics {
    totalBids: number
    scoredBids: number
    remainingBids: number
    averageTechnicalScore: number | null
}

export const EMPTY_BID_EVALUATION_FORM:
    BidEvaluationFormState = {
    technicalScore: '',
    comments: '',
}

export function canScoreBlindBid(
    tender: ProcurementTender,
): boolean {
    return tender.status === 'EVALUATION'
}

export function validateBidEvaluationForm(
    formState: BidEvaluationFormState,
): BidEvaluationFormErrors {
    const errors:
        BidEvaluationFormErrors = {}

    const scoreText =
        formState.technicalScore.trim()

    if (scoreText.length === 0) {
        errors.technicalScore =
            'Technical score is required.'
    } else {
        const score =
            Number(scoreText)

        if (!Number.isFinite(score)) {
            errors.technicalScore =
                'Technical score must be '
                + 'a valid number.'
        } else if (
            score < 0
            || score > 100
        ) {
            errors.technicalScore =
                'Technical score must be '
                + 'between 0 and 100.'
        }
    }

    const comments =
        formState.comments.trim()

    if (comments.length === 0) {
        errors.comments =
            'Evaluation comments are required.'
    } else if (
        comments.length < 10
    ) {
        errors.comments =
            'Evaluation comments must contain '
            + 'at least 10 characters.'
    } else if (
        comments.length > 1000
    ) {
        errors.comments =
            'Evaluation comments cannot exceed '
            + '1000 characters.'
    }

    return errors
}

export function buildEvaluateBidRequest(
    formState: BidEvaluationFormState,
): EvaluateBidRequest {
    const errors =
        validateBidEvaluationForm(
            formState
        )

    if (
        Object.keys(errors).length > 0
    ) {
        throw new Error(
            'The bid evaluation form '
            + 'contains invalid values.'
        )
    }

    return {
        technicalScore:
            Number(
                formState.technicalScore
            ),

        comments:
            formState.comments.trim(),
    }
}

export function calculateEvaluationStatistics(
    bidStates: EvaluationBidState[],
): EvaluationWorkspaceStatistics {
    const completedEvaluations =
        bidStates
            .map(
                (bidState) =>
                    bidState.evaluation
            )
            .filter(
                (
                    evaluation,
                ): evaluation is BidEvaluation =>
                    evaluation !== null
            )

    const totalBids =
        bidStates.length

    const scoredBids =
        completedEvaluations.length

    const remainingBids =
        Math.max(
            totalBids - scoredBids,
            0
        )

    if (
        completedEvaluations.length === 0
    ) {
        return {
            totalBids,
            scoredBids,
            remainingBids,
            averageTechnicalScore:
                null,
        }
    }

    const technicalScoreTotal =
        completedEvaluations.reduce(
            (
                total,
                evaluation,
            ) =>
                total
                + evaluation
                    .technicalScore,
            0
        )

    return {
        totalBids,
        scoredBids,
        remainingBids,

        averageTechnicalScore:
            technicalScoreTotal
            / completedEvaluations.length,
    }
}

export function createEvaluationBidStates(
    bids: BlindEvaluationBid[],
): EvaluationBidState[] {
    return bids.map(
        (bid) => ({
            bid,
            status: 'NOT_SCORED',
            evaluation: null,
            errorMessage: null,
        })
    )
}

export function updateEvaluationBidState(
    currentStates:
    EvaluationBidState[],
    bidId: number,
    update:
    Partial<
        Omit<
            EvaluationBidState,
            'bid'
        >
    >,
): EvaluationBidState[] {
    return currentStates.map(
        (bidState) => {
            if (
                bidState.bid.bidId
                !== bidId
            ) {
                return bidState
            }

            return {
                ...bidState,
                ...update,
            }
        }
    )
}

export function formatTechnicalScore(
    score: number,
): string {
    if (!Number.isFinite(score)) {
        return 'Unavailable'
    }

    return (
        score.toFixed(2)
        + '%'
    )
}