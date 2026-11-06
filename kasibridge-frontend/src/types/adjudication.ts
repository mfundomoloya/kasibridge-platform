export interface BidRanking {
    rank: number
    bidId: number
    tenderId: number
    bidderAlias: string
    averageTechnicalScore: number
    averagePriceScore: number
    averageTotalScore: number
    scoreCount: number
}

export interface AwardTenderRequest {
    winningBidId: number
    awardReason: string
}

export interface AwardTenderResponse {
    tenderId: number
    tenderReference: string
    tenderStatus: string
    winningBidId: number
    winningBidReference: string
    winningBidderAlias: string
    winningBidStatus: string
    adjudicatorUserId: number
    awardReason: string
    awardedAt: string
}

export interface AdjudicationAwardFormState {
    selectedBidId: string
    awardReason: string
}

export interface AdjudicationAwardFormErrors {
    selectedBidId?: string
    awardReason?: string
    form?: string
}

export interface AdjudicationApiFieldError {
    field: string
    message: string
}

export interface AdjudicationApiErrorResponse {
    timestamp?: string
    status?: number
    error?: string
    message?: string

    fieldErrors?:
        | Record<string, string>
        | AdjudicationApiFieldError[]
        | null
}

export type AdjudicationWorkspaceStatus =
    | 'LOADING'
    | 'READY'
    | 'AWARDING'
    | 'AWARDED'
    | 'UNAVAILABLE'
    | 'FAILED'

export interface AdjudicationWorkspaceState {
    status: AdjudicationWorkspaceStatus
    rankings: BidRanking[]
    selectedBidId: number | null
    awardResult: AwardTenderResponse | null
    errorMessage: string | null
}

export const EMPTY_ADJUDICATION_AWARD_FORM:
    AdjudicationAwardFormState = {
    selectedBidId: '',
    awardReason: '',
}

export function validateAdjudicationAwardForm(
    form:
    AdjudicationAwardFormState,
    rankings: BidRanking[],
): AdjudicationAwardFormErrors {
    const errors:
        AdjudicationAwardFormErrors = {}

    const selectedBidId =
        Number(
            form.selectedBidId
        )

    if (
        !Number.isInteger(
            selectedBidId
        )
        || selectedBidId <= 0
    ) {
        errors.selectedBidId =
            'Select a valid ranked bid.'
    } else {
        const rankedBidExists =
            rankings.some(
                (ranking) =>
                    ranking.bidId
                    === selectedBidId
            )

        if (!rankedBidExists) {
            errors.selectedBidId =
                'The selected bid does not '
                + 'appear in the adjudication '
                + 'ranking.'
        }
    }

    const awardReason =
        form.awardReason.trim()

    if (awardReason.length === 0) {
        errors.awardReason =
            'An award reason is required.'
    } else if (
        awardReason.length < 10
    ) {
        errors.awardReason =
            'The award reason must contain '
            + 'at least 10 characters.'
    } else if (
        awardReason.length > 1000
    ) {
        errors.awardReason =
            'The award reason cannot exceed '
            + '1000 characters.'
    }

    return errors
}

export function buildAwardTenderRequest(
    form:
    AdjudicationAwardFormState,
    rankings: BidRanking[],
): AwardTenderRequest {
    const errors =
        validateAdjudicationAwardForm(
            form,
            rankings
        )

    if (
        Object.keys(errors).length > 0
    ) {
        throw new Error(
            'The tender award form '
            + 'contains invalid values.'
        )
    }

    return {
        winningBidId:
            Number(
                form.selectedBidId
            ),

        awardReason:
            form.awardReason.trim(),
    }
}

export function findRankingByBidId(
    rankings: BidRanking[],
    bidId: number,
): BidRanking | null {
    return rankings.find(
        (ranking) =>
            ranking.bidId === bidId
    ) ?? null
}

export function getHighestRankedBid(
    rankings: BidRanking[],
): BidRanking | null {
    if (rankings.length === 0) {
        return null
    }

    return [...rankings]
        .sort(
            (
                left,
                right,
            ) =>
                left.rank
                - right.rank
        )[0] ?? null
}

export function formatAdjudicationScore(
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

export function formatAdjudicationStatus(
    status: string,
): string {
    return status
        .toLowerCase()
        .split('_')
        .map(
            (word) =>
                word.length === 0
                    ? word
                    : (
                        word
                            .charAt(0)
                            .toUpperCase()
                        + word.slice(1)
                    )
        )
        .join(' ')
}