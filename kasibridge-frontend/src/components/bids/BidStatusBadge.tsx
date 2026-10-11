import type {
    BidStatus,
} from '../../types/bid'

interface BidStatusBadgeProps {
    status: BidStatus
}

const bidStatusLabels: Record<
    BidStatus,
    string
> = {
    SUBMITTED: 'Submitted',
    COMPLIANCE_FAILED: 'Compliance failed',
    COMPLIANT: 'Compliant',
    UNDER_EVALUATION: 'Under evaluation',
    RECOMMENDED: 'Recommended',
    REJECTED: 'Rejected',
    AWARDED: 'Awarded',
    WITHDRAWN: 'Withdrawn',
}

const bidStatusClasses: Record<
    BidStatus,
    string
> = {
    SUBMITTED: 'status-submitted',
    COMPLIANCE_FAILED: 'status-failed',
    COMPLIANT: 'status-compliant',
    UNDER_EVALUATION: 'status-evaluation',
    RECOMMENDED: 'status-recommended',
    REJECTED: 'status-rejected',
    AWARDED: 'status-awarded',
    WITHDRAWN: 'status-withdrawn',
}

export function BidStatusBadge({
                                   status,
                               }: BidStatusBadgeProps) {
    const label = bidStatusLabels[status]
    const statusClass =
        bidStatusClasses[status]

    return (
        <span
            className={
                `bid-status-badge ${statusClass}`
            }
            aria-label={`Bid status: ${label}`}
        >
            {label}
        </span>
    )
}