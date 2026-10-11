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