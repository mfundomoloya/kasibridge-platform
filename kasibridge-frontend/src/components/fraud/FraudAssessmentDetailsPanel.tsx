import type {
    FraudAssessment,
    FraudReviewDecision,
} from '../../types/fraudAssessment'

import {
    canConfirmFraudAssessment,
    canEscalateFraudAssessment,
    canMarkFraudAssessmentFalsePositive,
    FRAUD_DETECTION_SOURCE_LABELS,
    FRAUD_RECOMMENDED_ACTION_LABELS,
    getRiskProbabilityPercentage,
    isFraudAssessmentFinal,
} from '../../types/fraudAssessment'

import {
    FraudReviewStatusBadge,
    FraudRiskLevelBadge,
} from './FraudAssessmentStatusBadge'

interface FraudAssessmentDetailsPanelProps {
    assessment: FraudAssessment | null
    isPlatformAdministrator: boolean
    isRefreshing: boolean
    onReview: (
        decision: FraudReviewDecision,
        assessment: FraudAssessment,
    ) => void
    onRefresh: (
        assessmentReference: string,
    ) => void
    onClose: () => void
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

export function FraudAssessmentDetailsPanel({
                                                assessment,
                                                isPlatformAdministrator,
                                                isRefreshing,
                                                onReview,
                                                onRefresh,
                                                onClose,
                                            }: FraudAssessmentDetailsPanelProps) {
    if (assessment === null) {
        return (
            <aside
                className={
                    'fraud-details-panel '
                    + 'fraud-details-empty'
                }
                aria-label={
                    'Fraud assessment details'
                }
            >
                <div className="empty-state">
                    <h3>
                        Select an assessment
                    </h3>

                    <p>
                        Choose an assessment from the
                        analyst queue to inspect its
                        model result, evidence hashes,
                        review history, and available
                        actions.
                    </p>
                </div>
            </aside>
        )
    }

    const canConfirm =
        canConfirmFraudAssessment(
            assessment
        )

    const canMarkFalsePositive =
        canMarkFraudAssessmentFalsePositive(
            assessment
        )

    const canEscalate =
        isPlatformAdministrator
        && canEscalateFraudAssessment(
            assessment
        )

    const isFinal =
        isFraudAssessmentFinal(
            assessment
        )

    return (
        <aside
            className="fraud-details-panel"
            aria-labelledby={
                'fraud-assessment-details-heading'
            }
        >
            <div className="fraud-details-header">
                <div>
                    <span className="eyebrow">
                        Assessment detail
                    </span>

                    <h2
                        id={
                            'fraud-assessment-details-heading'
                        }
                    >
                        {
                            assessment
                                .assessmentReference
                        }
                    </h2>

                    <p>
                        Invoice
                        {' '}
                        <strong>
                            {
                                assessment
                                    .invoiceReference
                            }
                        </strong>
                    </p>
                </div>

                <button
                    className={
                        'text-button '
                        + 'fraud-details-close'
                    }
                    type="button"
                    onClick={onClose}
                    aria-label={
                        'Close assessment details'
                    }
                >
                    Close
                </button>
            </div>

            <div className="fraud-details-badges">
                <FraudRiskLevelBadge
                    riskLevel={
                        assessment.riskLevel
                    }
                />

                <FraudReviewStatusBadge
                    status={
                        assessment.reviewStatus
                    }
                />
            </div>

            <section
                className="fraud-detail-section"
                aria-labelledby={
                    'fraud-risk-result-heading'
                }
            >
                <h3
                    id="fraud-risk-result-heading"
                >
                    Risk result
                </h3>

                <dl className="fraud-detail-list">
                    <DetailItem
                        label="Classification"
                        value={
                            formatEnumLabel(
                                assessment
                                    .classification
                            )
                        }
                    />

                    <DetailItem
                        label="Risk probability"
                        value={
                            formatProbability(
                                assessment
                                    .riskProbability
                            )
                        }
                    />

                    <DetailItem
                        label="Decision threshold"
                        value={
                            formatProbability(
                                assessment.threshold
                            )
                        }
                    />

                    <DetailItem
                        label="Recommended action"
                        value={
                            FRAUD_RECOMMENDED_ACTION_LABELS[
                                assessment
                                    .recommendedAction
                                ]
                        }
                    />

                    <DetailItem
                        label="Detection source"
                        value={
                            FRAUD_DETECTION_SOURCE_LABELS[
                                assessment
                                    .detectionSource
                                ]
                        }
                    />
                </dl>
            </section>

            <section
                className="fraud-detail-section"
                aria-labelledby={
                    'fraud-invoice-detail-heading'
                }
            >
                <h3
                    id="fraud-invoice-detail-heading"
                >
                    Invoice and supplier
                </h3>

                <dl className="fraud-detail-list">
                    <DetailItem
                        label="Invoice amount"
                        value={
                            formatCurrency(
                                assessment
                                    .invoiceAmount
                            )
                        }
                    />

                    <DetailItem
                        label="Supplier ID"
                        value={
                            assessment.supplierId
                            ?? 'Not supplied'
                        }
                    />

                    <DetailItem
                        label="Department ID"
                        value={
                            assessment.departmentId
                            ?? 'Not supplied'
                        }
                    />

                    <DetailItem
                        label="Supplier country"
                        value={
                            assessment
                                .supplierCountry
                            ?? 'Not supplied'
                        }
                    />

                    <DetailItem
                        label="Region"
                        value={
                            assessment.region
                            ?? 'Not supplied'
                        }
                    />

                    <DetailItem
                        label="Assessed"
                        value={
                            formatDateTime(
                                assessment
                                    .assessedAt
                            )
                        }
                    />
                </dl>
            </section>

            <section
                className="fraud-detail-section"
                aria-labelledby={
                    'fraud-model-detail-heading'
                }
            >
                <h3
                    id="fraud-model-detail-heading"
                >
                    Model and evidence
                </h3>

                <dl className="fraud-detail-list">
                    <DetailItem
                        label="Model"
                        value={
                            assessment.modelName
                        }
                    />

                    <DetailItem
                        label="Model version"
                        value={
                            assessment.modelVersion
                        }
                    />

                    <DetailItem
                        label="Experiment"
                        value={
                            assessment.experiment
                        }
                    />

                    <DetailItem
                        label="Record version"
                        value={
                            assessment.version
                                .toString()
                        }
                    />
                </dl>

                <HashValue
                    label="Model SHA-256"
                    value={
                        assessment.modelSha256
                    }
                />

                <HashValue
                    label="Feature snapshot SHA-256"
                    value={
                        assessment
                            .featureSnapshotSha256
                    }
                />
            </section>

            <section
                className="fraud-detail-section"
                aria-labelledby={
                    'fraud-review-history-heading'
                }
            >
                <h3
                    id="fraud-review-history-heading"
                >
                    Review history
                </h3>

                {assessment.reviewedBy === null ? (
                    <p className="fraud-detail-muted">
                        No analyst decision has been
                        recorded for this assessment.
                    </p>
                ) : (
                    <>
                        <dl className="fraud-detail-list">
                            <DetailItem
                                label="Reviewed by"
                                value={
                                    assessment
                                        .reviewedBy
                                }
                            />

                            <DetailItem
                                label="Reviewed at"
                                value={
                                    assessment
                                        .reviewedAt
                                    === null
                                        ? 'Not recorded'
                                        : formatDateTime(
                                            assessment
                                                .reviewedAt
                                        )
                                }
                            />

                            <DetailItem
                                label="Last updated"
                                value={
                                    assessment
                                        .updatedAt
                                    === null
                                        ? 'Not recorded'
                                        : formatDateTime(
                                            assessment
                                                .updatedAt
                                        )
                                }
                            />
                        </dl>

                        {assessment.reviewNotes && (
                            <div
                                className={
                                    'fraud-review-notes'
                                }
                            >
                                <span>
                                    Review notes
                                </span>

                                <p>
                                    {
                                        assessment
                                            .reviewNotes
                                    }
                                </p>
                            </div>
                        )}
                    </>
                )}
            </section>

            <div className="fraud-details-actions">
                <button
                    className="secondary-button"
                    type="button"
                    disabled={isRefreshing}
                    onClick={
                        () =>
                            onRefresh(
                                assessment
                                    .assessmentReference
                            )
                    }
                >
                    {isRefreshing
                        ? 'Refreshing...'
                        : 'Refresh assessment'}
                </button>

                {!isFinal && (
                    <div
                        className={
                            'fraud-review-actions'
                        }
                    >
                        {canMarkFalsePositive && (
                            <button
                                className={
                                    'secondary-button '
                                    + 'fraud-false-positive-button'
                                }
                                type="button"
                                onClick={
                                    () =>
                                        onReview(
                                            'FALSE_POSITIVE',
                                            assessment
                                        )
                                }
                            >
                                Mark false positive
                            </button>
                        )}

                        {canEscalate && (
                            <button
                                className={
                                    'secondary-button '
                                    + 'fraud-escalate-button'
                                }
                                type="button"
                                onClick={
                                    () =>
                                        onReview(
                                            'ESCALATE',
                                            assessment
                                        )
                                }
                            >
                                Escalate
                            </button>
                        )}

                        {canConfirm && (
                            <button
                                className={
                                    'danger-button '
                                    + 'fraud-confirm-button'
                                }
                                type="button"
                                onClick={
                                    () =>
                                        onReview(
                                            'CONFIRM_FRAUD',
                                            assessment
                                        )
                                }
                            >
                                Confirm fraud
                            </button>
                        )}
                    </div>
                )}

                {isFinal && (
                    <p
                        className={
                            'fraud-final-state-message'
                        }
                    >
                        This assessment has reached a
                        final review state and cannot be
                        changed.
                    </p>
                )}
            </div>
        </aside>
    )
}

interface DetailItemProps {
    label: string
    value: string
}

function DetailItem({
                        label,
                        value,
                    }: DetailItemProps) {
    return (
        <div>
            <dt>{label}</dt>
            <dd>{value}</dd>
        </div>
    )
}

interface HashValueProps {
    label: string
    value: string
}

function HashValue({
                       label,
                       value,
                   }: HashValueProps) {
    return (
        <div className="fraud-hash-value">
            <span>{label}</span>

            <code>{value}</code>
        </div>
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

function formatProbability(
    probability: number,
): string {
    if (!Number.isFinite(probability)) {
        return 'Unavailable'
    }

    return (
        getRiskProbabilityPercentage(
            probability
        ).toFixed(2)
        + '%'
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

function formatEnumLabel(
    value: string,
): string {
    return value
        .toLowerCase()
        .split('_')
        .map(
            (word) =>
                word.length === 0
                    ? word
                    : word.charAt(0)
                        .toUpperCase()
                    + word.slice(1)
        )
        .join(' ')
}