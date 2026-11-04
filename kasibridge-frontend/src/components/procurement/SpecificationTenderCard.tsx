import type {
    ProcurementTender,
} from '../../types/procurementTender'

import {
    canCloseTenderBidding,
    canPublishTender,
    canStartTenderEvaluation,
    canVerifyTenderSpecification,
} from '../../types/procurementTender'

import {
    ProcurementTenderStatusBadge,
} from './ProcurementTenderStatusBadge'

interface SpecificationTenderCardProps {
    tender: ProcurementTender
    isActionPending: boolean
    onView: (
        tender: ProcurementTender,
    ) => void
    onPublish: (
        tender: ProcurementTender,
    ) => void
    onVerifyIntegrity: (
        tender: ProcurementTender,
    ) => void
    onCloseBidding: (
        tender: ProcurementTender,
    ) => void
    onStartEvaluation: (
        tender: ProcurementTender,
    ) => void
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

export function SpecificationTenderCard({
                                            tender,
                                            isActionPending,
                                            onView,
                                            onPublish,
                                            onVerifyIntegrity,
                                            onCloseBidding,
                                            onStartEvaluation,
                                        }: SpecificationTenderCardProps) {
    const canPublish =
        canPublishTender(
            tender
        )

    const canVerify =
        canVerifyTenderSpecification(
            tender
        )

    const canCloseBidding =
        canCloseTenderBidding(
            tender
        )

    const canStartEvaluation =
        canStartTenderEvaluation(
            tender
        )

    return (
        <article className="specification-tender-card">
            <header
                className={
                    'specification-tender-card-header'
                }
            >
                <div>
                    <span
                        className={
                            'specification-tender-reference'
                        }
                    >
                        {tender.tenderReference}
                    </span>

                    <h2>
                        {tender.title}
                    </h2>
                </div>

                <ProcurementTenderStatusBadge
                    status={tender.status}
                />
            </header>

            <p
                className={
                    'specification-tender-description'
                }
            >
                {tender.description}
            </p>

            <dl className="specification-tender-summary">
                <SummaryItem
                    label="Budget"
                    value={
                        formatCurrency(
                            tender.budgetAmount
                        )
                    }
                />

                <SummaryItem
                    label="Buyer organisation"
                    value={tender.buyerOrgId}
                />

                <SummaryItem
                    label="Business type"
                    value={
                        tender.requiredBusinessType
                    }
                />

                <SummaryItem
                    label="Service area"
                    value={tender.serviceArea}
                />

                <SummaryItem
                    label="Created"
                    value={
                        formatDateTime(
                            tender.createdAt
                        )
                    }
                />

                <SummaryItem
                    label="Last updated"
                    value={
                        formatDateTime(
                            tender.updatedAt
                        )
                    }
                />
            </dl>

            <div
                className={
                    'specification-tender-lifecycle'
                }
            >
                <LifecycleItem
                    label="Published"
                    value={
                        tender.publishedAt === null
                            ? 'Not published'
                            : formatDateTime(
                                tender.publishedAt
                            )
                    }
                />

                <LifecycleItem
                    label="Bidding closed"
                    value={
                        tender.closedAt === null
                            ? 'Not closed'
                            : formatDateTime(
                                tender.closedAt
                            )
                    }
                />

                <LifecycleItem
                    label="Specification evidence"
                    value={
                        getSpecificationEvidenceLabel(
                            tender
                        )
                    }
                    code={
                        tender.specificationHash
                        ?? undefined
                    }
                />
            </div>

            <footer
                className={
                    'specification-tender-card-actions'
                }
            >
                <button
                    className="secondary-button"
                    type="button"
                    onClick={
                        () => {
                            onView(tender)
                        }
                    }
                    disabled={isActionPending}
                >
                    View details
                </button>

                <div
                    className={
                        'specification-tender-workflow-actions'
                    }
                >
                    {canVerify && (
                        <button
                            className={
                                'secondary-button '
                                + 'specification-integrity-button'
                            }
                            type="button"
                            onClick={
                                () => {
                                    onVerifyIntegrity(
                                        tender
                                    )
                                }
                            }
                            disabled={
                                isActionPending
                            }
                        >
                            Verify integrity
                        </button>
                    )}

                    {canPublish && (
                        <button
                            className="primary-button"
                            type="button"
                            onClick={
                                () => {
                                    onPublish(tender)
                                }
                            }
                            disabled={
                                isActionPending
                            }
                        >
                            {isActionPending
                                ? 'Publishing...'
                                : 'Publish tender'}
                        </button>
                    )}

                    {canCloseBidding && (
                        <button
                            className={
                                'primary-button '
                                + 'specification-close-button'
                            }
                            type="button"
                            onClick={
                                () => {
                                    onCloseBidding(
                                        tender
                                    )
                                }
                            }
                            disabled={
                                isActionPending
                            }
                        >
                            {isActionPending
                                ? 'Closing...'
                                : 'Close bidding'}
                        </button>
                    )}

                    {canStartEvaluation && (
                        <button
                            className={
                                'primary-button '
                                + 'specification-evaluation-button'
                            }
                            type="button"
                            onClick={
                                () => {
                                    onStartEvaluation(
                                        tender
                                    )
                                }
                            }
                            disabled={
                                isActionPending
                            }
                        >
                            {isActionPending
                                ? 'Starting...'
                                : 'Start evaluation'}
                        </button>
                    )}
                </div>
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

interface LifecycleItemProps {
    label: string
    value: string
    code?: string
}

function LifecycleItem({
                           label,
                           value,
                           code,
                       }: LifecycleItemProps) {
    return (
        <div>
            <span>{label}</span>

            <strong>{value}</strong>

            {code !== undefined && (
                <code title={code}>
                    {abbreviateHash(code)}
                </code>
            )}
        </div>
    )
}

function getSpecificationEvidenceLabel(
    tender: ProcurementTender,
): string {
    if (
        tender.specificationHash === null
        || tender.specificationHash
            .trim()
            .length === 0
    ) {
        return 'Not locked'
    }

    return 'Locked with SHA-256'
}

function abbreviateHash(
    hash: string,
): string {
    if (hash.length <= 24) {
        return hash
    }

    return (
        hash.slice(0, 12)
        + '...'
        + hash.slice(-12)
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