import type {
    Bid,
} from '../../types/bid'

import {
    canWithdrawBid,
} from '../../types/bid'

import {
    formatCurrency,
    formatDateTime,
} from '../../utils/tenderFormatters'

import {
    BidStatusBadge,
} from './BidStatusBadge'

interface BidCardProps {
    bid: Bid
    isWithdrawing?: boolean
    onWithdraw?: (bid: Bid) => void
}

export function BidCard({
                            bid,
                            isWithdrawing = false,
                            onWithdraw,
                        }: BidCardProps) {
    const withdrawalAllowed =
        canWithdrawBid(bid.status)

    return (
        <article className="content-card bid-card">
            <div className="bid-card-header">
                <div>
                    <span className="eyebrow">
                        Bid reference
                    </span>

                    <h2>{bid.bidReference}</h2>
                </div>

                <BidStatusBadge
                    status={bid.status}
                />
            </div>

            <dl className="profile-details">
                <div>
                    <dt>Bidder alias</dt>
                    <dd>{bid.bidderAlias}</dd>
                </div>

                <div>
                    <dt>Bid price</dt>
                    <dd>
                        {formatCurrency(
                            bid.priceAmount
                        )}
                    </dd>
                </div>

                <div>
                    <dt>Submitted</dt>
                    <dd>
                        {formatDateTime(
                            bid.submittedAt
                        )}
                    </dd>
                </div>

                <div>
                    <dt>Last updated</dt>
                    <dd>
                        {formatDateTime(
                            bid.updatedAt
                        )}
                    </dd>
                </div>
            </dl>

            <div className="bid-card-proposal">
                <h3>Technical proposal</h3>

                <p className="tender-detail-text">
                    {bid.technicalProposal}
                </p>
            </div>

            {bid.compliance && (
                <div className="bid-compliance-summary">
                    <h3>Compliance assessment</h3>

                    <p>
                        Result:{' '}
                        <strong>
                            {bid.compliance.passed
                                ? 'Passed'
                                : 'Failed'}
                        </strong>
                    </p>

                    {bid.compliance.failureReason && (
                        <p
                            className="error-message"
                            role="status"
                        >
                            {
                                bid.compliance
                                    .failureReason
                            }
                        </p>
                    )}

                    <p>
                        Checked:{' '}
                        {formatDateTime(
                            bid.compliance.checkedAt
                        )}
                    </p>
                </div>
            )}

            {withdrawalAllowed && onWithdraw && (
                <div className="form-actions">
                    <button
                        className="secondary-button"
                        type="button"
                        disabled={isWithdrawing}
                        onClick={() => onWithdraw(bid)}
                    >
                        {isWithdrawing
                            ? 'Withdrawing bid...'
                            : 'Withdraw bid'}
                    </button>
                </div>
            )}
        </article>
    )
}