import type {
    ProcurementTenderStatus,
} from '../../types/procurementTender'

import {
    PROCUREMENT_TENDER_STATUS_LABELS,
} from '../../types/procurementTender'

interface ProcurementTenderStatusBadgeProps {
    status: ProcurementTenderStatus
}

export function ProcurementTenderStatusBadge({
                                                 status,
                                             }: ProcurementTenderStatusBadgeProps) {
    return (
        <span
            className={
                'procurement-tender-status-badge '
                + getTenderStatusClassName(
                    status
                )
            }
        >
            {
                PROCUREMENT_TENDER_STATUS_LABELS[
                    status
                    ]
            }
        </span>
    )
}

function getTenderStatusClassName(
    status: ProcurementTenderStatus,
): string {
    switch (status) {
        case 'DRAFT':
            return 'procurement-tender-draft'

        case 'PUBLISHED':
            return 'procurement-tender-published'

        case 'BIDDING_CLOSED':
            return 'procurement-tender-closed'

        case 'EVALUATION':
            return 'procurement-tender-evaluation'

        case 'ADJUDICATION':
            return 'procurement-tender-adjudication'

        case 'AWARDED':
            return 'procurement-tender-awarded'

        case 'CANCELLED':
            return 'procurement-tender-cancelled'
    }
}