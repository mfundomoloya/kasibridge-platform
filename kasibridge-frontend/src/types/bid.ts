import type {
    ProcurementTenderStatus,
} from './procurementTender'

export type BidStatus =
    | "SUBMITTED"
    | "COMPLIANCE_FAILED"
    | "COMPLIANT"
    | "UNDER_EVALUATION"
    | "RECOMMENDED"
    | "REJECTED"
    | "AWARDED"
    | "WITHDRAWN";

export interface BidCompliance {
    id: number;
    bidId: number;
    csdValid: boolean;
    taxClearanceValid: boolean;
    bbbeeValid: boolean;
    requiredDocumentsUploaded: boolean;
    passed: boolean;
    failureReason: string | null;
    checkedAt: string;
}

export interface Bid {
    id: number;
    bidReference: string;
    tenderId: number;
    traderId: number;
    bidderAlias: string;
    technicalProposal: string;
    priceAmount: number;
    status: BidStatus;
    submittedAt: string;
    updatedAt: string;
    compliance: BidCompliance | null;
}

export interface ApiErrorResponse {
    status: number;
    message: string;
    timestamp: string;
    fieldErrors: Record<string, string> | null;
}

export const WITHDRAWABLE_BID_STATUSES: ReadonlySet<BidStatus> =
    new Set<BidStatus>([
        "SUBMITTED",
        "COMPLIANT",
        "COMPLIANCE_FAILED",
    ]);

export function canWithdrawBid(status: BidStatus): boolean {
    return WITHDRAWABLE_BID_STATUSES.has(status);
}

export function isWithdrawnBid(status: BidStatus): boolean {
    return status === "WITHDRAWN";
}

export type TraderBidOutcomeStatus =
    | 'PENDING'
    | 'COMPLIANCE_FAILED'
    | 'WITHDRAWN'
    | 'AWARDED'
    | 'UNSUCCESSFUL'
    | 'CANCELLED'

export interface TraderBidOutcome {
    bidId: number
    bidReference: string
    bidStatus: BidStatus
    bidderAlias: string
    priceAmount: number
    submittedAt: string

    tenderId: number
    tenderReference: string
    tenderTitle: string
    tenderStatus: ProcurementTenderStatus

    compliancePassed: boolean
    complianceFailureReason: string | null

    outcome: TraderBidOutcomeStatus
    awardedAt: string | null
    publishedOutcomeReason: string
}

export function isFinalTraderBidOutcome(
    outcome: TraderBidOutcomeStatus,
): boolean {
    return (
        outcome === 'AWARDED'
        || outcome === 'UNSUCCESSFUL'
        || outcome === 'WITHDRAWN'
        || outcome === 'COMPLIANCE_FAILED'
        || outcome === 'CANCELLED'
    )
}

export function canViewTraderBidOutcome(
    bidStatus: BidStatus,
): boolean {
    return (
        bidStatus === 'AWARDED'
        || bidStatus === 'REJECTED'
        || bidStatus === 'WITHDRAWN'
        || bidStatus === 'COMPLIANCE_FAILED'
    )
}

export function getTraderBidOutcomeLabel(
    outcome: TraderBidOutcomeStatus,
): string {
    switch (outcome) {
        case 'AWARDED':
            return 'Awarded'

        case 'UNSUCCESSFUL':
            return 'Unsuccessful'

        case 'WITHDRAWN':
            return 'Withdrawn'

        case 'COMPLIANCE_FAILED':
            return 'Compliance failed'

        case 'CANCELLED':
            return 'Tender cancelled'

        case 'PENDING':
            return 'Outcome pending'
    }
}

export function getTraderBidOutcomeDescription(
    outcome: TraderBidOutcomeStatus,
): string {
    switch (outcome) {
        case 'AWARDED':
            return (
                'This bid was selected for '
                + 'the tender award.'
            )

        case 'UNSUCCESSFUL':
            return (
                'The tender was finalised, '
                + 'but this bid was not '
                + 'selected for the award.'
            )

        case 'WITHDRAWN':
            return (
                'This bid was withdrawn before '
                + 'the tender outcome was '
                + 'finalised.'
            )

        case 'COMPLIANCE_FAILED':
            return (
                'This bid did not pass the '
                + 'mandatory compliance '
                + 'assessment.'
            )

        case 'CANCELLED':
            return (
                'The tender was cancelled '
                + 'before an award was completed.'
            )

        case 'PENDING':
            return (
                'The tender outcome has not '
                + 'yet been finalised.'
            )
    }
}

export function getTraderBidOutcomeTone(
    outcome: TraderBidOutcomeStatus,
):
    | 'positive'
    | 'negative'
    | 'warning'
    | 'neutral' {
    switch (outcome) {
        case 'AWARDED':
            return 'positive'

        case 'UNSUCCESSFUL':
        case 'COMPLIANCE_FAILED':
            return 'negative'

        case 'WITHDRAWN':
        case 'CANCELLED':
            return 'warning'

        case 'PENDING':
            return 'neutral'
    }
}