import {
    useCallback,
    useEffect,
    useState,
} from 'react'

import {
    Link,
    useParams,
} from 'react-router-dom'

import {
    getBidApiError,
    getCurrentTraderBidOutcome,
} from '../api/bidApi'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import {
    BidStatusBadge,
} from '../components/bids/BidStatusBadge'

import type {
    TraderBidOutcome,
} from '../types/bid'

import {
    getTraderBidOutcomeDescription,
    getTraderBidOutcomeLabel,
    getTraderBidOutcomeTone,
} from '../types/bid'

import {
    formatCurrency,
    formatDateTime,
} from '../utils/tenderFormatters'

import './SpecificationTenderPage.css'

export function TraderBidOutcomePage() {
    const routeParameters =
        useParams()

    const bidId =
        parseBidId(
            routeParameters.bidId
        )

    const [
        outcome,
        setOutcome,
    ] =
        useState<
            TraderBidOutcome | null
        >(null)

    const [
        isLoading,
        setIsLoading,
    ] = useState(true)

    const [
        isRefreshing,
        setIsRefreshing,
    ] = useState(false)

    const [
        errorMessage,
        setErrorMessage,
    ] =
        useState<string | null>(
            bidId === null
                ? 'A valid bid ID is required.'
                : null
        )

    const loadOutcome =
        useCallback(
            async (
                manualRefresh:
                boolean = false,
            ): Promise<void> => {
                if (bidId === null) {
                    setIsLoading(false)
                    setIsRefreshing(false)

                    setErrorMessage(
                        'A valid bid ID '
                        + 'is required.'
                    )

                    return
                }

                if (manualRefresh) {
                    setIsRefreshing(true)
                } else {
                    setIsLoading(true)
                }

                setErrorMessage(null)

                try {
                    const response =
                        await getCurrentTraderBidOutcome(
                            bidId
                        )

                    setOutcome(
                        response
                    )
                } catch (error) {
                    const apiError =
                        getBidApiError(
                            error
                        )

                    setErrorMessage(
                        apiError.message
                    )
                } finally {
                    setIsLoading(false)
                    setIsRefreshing(false)
                }
            },
            [
                bidId,
            ]
        )

    useEffect(
        () => {
            queueMicrotask(
                () => {
                    void loadOutcome()
                }
            )
        },
        [
            loadOutcome,
        ]
    )

    return (
        <AppLayout>
            <nav
                className="back-navigation"
                aria-label={
                    'Bid outcome navigation'
                }
            >
                <Link
                    to="/my-bids"
                >
                    Back to my bids
                </Link>
            </nav>

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

            {isLoading
            && outcome === null ? (
                <OutcomeLoadingState />
            ) : outcome === null ? (
                <OutcomeUnavailableState />
            ) : (
                <>
                    <TraderOutcomeHeader
                        outcome={outcome}
                        isRefreshing={
                            isRefreshing
                        }
                        onRefresh={
                            () => {
                                void loadOutcome(
                                    true
                                )
                            }
                        }
                    />

                    <TraderOutcomeSummary
                        outcome={outcome}
                    />

                    <TraderBidDetails
                        outcome={outcome}
                    />

                    <TraderCompliancePanel
                        outcome={outcome}
                    />

                    <TraderOutcomePrivacyNotice />
                </>
            )}
        </AppLayout>
    )
}

interface TraderOutcomeHeaderProps {
    outcome: TraderBidOutcome
    isRefreshing: boolean
    onRefresh: () => void
}

function TraderOutcomeHeader({
                                 outcome,
                                 isRefreshing,
                                 onRefresh,
                             }: TraderOutcomeHeaderProps) {
    return (
        <section className="page-heading">
            <div>
                <span className="eyebrow">
                    Tender outcome
                </span>

                <h1>
                    {outcome.tenderTitle}
                </h1>

                <p>
                    Tender
                    {' '}
                    <strong>
                        {
                            outcome
                                .tenderReference
                        }
                    </strong>
                    {' · '}
                    Bid
                    {' '}
                    <strong>
                        {
                            outcome
                                .bidReference
                        }
                    </strong>
                </p>
            </div>

            <div
                className={
                    'trader-outcome-heading-actions'
                }
            >
                <BidStatusBadge
                    status={
                        outcome.bidStatus
                    }
                />

                <button
                    className="secondary-button"
                    type="button"
                    onClick={onRefresh}
                    disabled={isRefreshing}
                >
                    {isRefreshing
                        ? 'Refreshing...'
                        : 'Refresh outcome'}
                </button>
            </div>
        </section>
    )
}

interface TraderOutcomeSummaryProps {
    outcome: TraderBidOutcome
}

function TraderOutcomeSummary({
                                  outcome,
                              }: TraderOutcomeSummaryProps) {
    const tone =
        getTraderBidOutcomeTone(
            outcome.outcome
        )

    const label =
        getTraderBidOutcomeLabel(
            outcome.outcome
        )

    const description =
        getTraderBidOutcomeDescription(
            outcome.outcome
        )

    return (
        <section
            className={
                'content-card '
                + 'trader-outcome-summary '
                + 'is-'
                + tone
            }
            aria-labelledby={
                'trader-outcome-heading'
            }
        >
            <div
                className={
                    'trader-outcome-status'
                }
            >
                <span>
                    Published result
                </span>

                <h2
                    id={
                        'trader-outcome-heading'
                    }
                >
                    {label}
                </h2>

                <p>
                    {description}
                </p>
            </div>

            <div
                className={
                    'trader-outcome-reason'
                }
            >
                <span>
                    Published outcome reason
                </span>

                <p>
                    {
                        outcome
                            .publishedOutcomeReason
                    }
                </p>
            </div>

            <dl
                className={
                    'trader-outcome-result-grid'
                }
            >
                <OutcomeDetailItem
                    label="Tender status"
                    value={
                        formatStatus(
                            outcome
                                .tenderStatus
                        )
                    }
                />

                <OutcomeDetailItem
                    label="Bid status"
                    value={
                        formatStatus(
                            outcome
                                .bidStatus
                        )
                    }
                />

                <OutcomeDetailItem
                    label={
                        outcome.outcome === 'AWARDED'
                        || outcome.outcome === 'UNSUCCESSFUL'
                            ? 'Tender award date'
                            : 'Award date'
                    }
                    value={
                        outcome.outcome === 'AWARDED'
                        || outcome.outcome === 'UNSUCCESSFUL'
                            ? (
                                outcome.awardedAt === null
                                    ? 'Not recorded'
                                    : formatDateTime(
                                        outcome.awardedAt
                                    )
                            )
                            : 'Not applicable'
                    }
                />
            </dl>
        </section>
    )
}

interface TraderBidDetailsProps {
    outcome: TraderBidOutcome
}

function TraderBidDetails({
                              outcome,
                          }: TraderBidDetailsProps) {
    return (
        <section
            className={
                'content-card '
                + 'trader-outcome-bid-details'
            }
            aria-labelledby={
                'trader-bid-details-heading'
            }
        >
            <div>
                <span className="eyebrow">
                    Your submission
                </span>

                <h2
                    id={
                        'trader-bid-details-heading'
                    }
                >
                    Bid details
                </h2>

                <p>
                    These details relate only to
                    the authenticated Trader’s own
                    submission.
                </p>
            </div>

            <dl
                className={
                    'trader-outcome-details-grid'
                }
            >
                <OutcomeDetailItem
                    label="Bid reference"
                    value={
                        outcome.bidReference
                    }
                />

                <OutcomeDetailItem
                    label="Evaluation alias"
                    value={
                        outcome.bidderAlias
                    }
                />

                <OutcomeDetailItem
                    label="Bid price"
                    value={
                        formatCurrency(
                            outcome.priceAmount
                        )
                    }
                />

                <OutcomeDetailItem
                    label="Submitted"
                    value={
                        formatDateTime(
                            outcome.submittedAt
                        )
                    }
                />

                <OutcomeDetailItem
                    label="Tender reference"
                    value={
                        outcome
                            .tenderReference
                    }
                />

                <OutcomeDetailItem
                    label="Tender ID"
                    value={
                        outcome
                            .tenderId
                            .toString()
                    }
                />
            </dl>
        </section>
    )
}

interface TraderCompliancePanelProps {
    outcome: TraderBidOutcome
}

function TraderCompliancePanel({
                                   outcome,
                               }: TraderCompliancePanelProps) {
    const panelClassName =
        outcome.compliancePassed
            ? (
                'content-card '
                + 'trader-outcome-compliance '
                + 'is-passed'
            )
            : (
                'content-card '
                + 'trader-outcome-compliance '
                + 'is-failed'
            )

    return (
        <section
            className={panelClassName}
            aria-labelledby={
                'trader-compliance-heading'
            }
        >
            <div>
                <span className="eyebrow">
                    Mandatory compliance
                </span>

                <h2
                    id={
                        'trader-compliance-heading'
                    }
                >
                    Compliance assessment
                </h2>
            </div>

            <div
                className={
                    'trader-outcome-compliance-result'
                }
            >
                <span>
                    Result
                </span>

                <strong>
                    {
                        outcome.compliancePassed
                            ? 'Passed'
                            : 'Failed'
                    }
                </strong>
            </div>

            {!outcome.compliancePassed
                && outcome
                    .complianceFailureReason
                !== null && (
                    <div
                        className={
                            'trader-outcome-'
                            + 'compliance-reason'
                        }
                    >
                    <span>
                        Compliance findings
                    </span>

                        <p>
                            {
                                outcome
                                    .complianceFailureReason
                            }
                        </p>
                    </div>
                )}
        </section>
    )
}

function TraderOutcomePrivacyNotice() {
    return (
        <section
            className={
                'content-card '
                + 'trader-outcome-privacy'
            }
            aria-labelledby={
                'trader-outcome-privacy-heading'
            }
        >
            <div>
                <span className="eyebrow">
                    Protected procurement
                    information
                </span>

                <h2
                    id={
                        'trader-outcome-privacy-heading'
                    }
                >
                    Outcome disclosure
                </h2>
            </div>

            <p>
                This page displays only the
                authenticated Trader’s own bid
                outcome and the published tender
                decision. Competing bids,
                Evaluator identities, individual
                scores, evaluation comments and
                internal procurement records
                remain protected.
            </p>
        </section>
    )
}

interface OutcomeDetailItemProps {
    label: string
    value: string
}

function OutcomeDetailItem({
                               label,
                               value,
                           }: OutcomeDetailItemProps) {
    return (
        <div>
            <dt>
                {label}
            </dt>

            <dd>
                {value}
            </dd>
        </div>
    )
}

function OutcomeLoadingState() {
    return (
        <section
            className={
                'content-card '
                + 'trader-outcome-loading'
            }
            aria-busy="true"
        >
            <div className="empty-state">
                <h1>
                    Loading bid outcome
                </h1>

                <p>
                    Retrieving the authenticated
                    Trader’s submitted bid and
                    published tender outcome.
                </p>
            </div>
        </section>
    )
}

function OutcomeUnavailableState() {
    return (
        <section className="content-card">
            <div className="empty-state">
                <h1>
                    Bid outcome unavailable
                </h1>

                <p>
                    The bid could not be found or
                    does not belong to the
                    authenticated Trader.
                </p>

                <Link
                    className={
                        'primary-link-button'
                    }
                    to="/my-bids"
                >
                    Return to my bids
                </Link>
            </div>
        </section>
    )
}

function parseBidId(
    value: string | undefined,
): number | null {
    if (value === undefined) {
        return null
    }

    const parsedValue =
        Number(value)

    if (
        !Number.isInteger(
            parsedValue
        )
        || parsedValue <= 0
    ) {
        return null
    }

    return parsedValue
}

function formatStatus(
    value: string,
): string {
    return value
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