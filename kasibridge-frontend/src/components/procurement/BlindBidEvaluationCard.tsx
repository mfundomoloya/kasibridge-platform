import {
    useState,
} from 'react'

import type {
    ChangeEvent,
    SyntheticEvent,
} from 'react'

import type {
    BidEvaluationFormErrors,
    BidEvaluationFormState,
    EvaluationBidState,
    EvaluateBidRequest,
} from '../../types/bidEvaluation'

import {
    buildEvaluateBidRequest,
    EMPTY_BID_EVALUATION_FORM,
    formatTechnicalScore,
    validateBidEvaluationForm,
} from '../../types/bidEvaluation'

interface BlindBidEvaluationCardProps {
    bidState: EvaluationBidState
    isWorkspaceReadOnly: boolean
    onSubmit: (
        bidId: number,
        request: EvaluateBidRequest,
    ) => Promise<void>
}

const dateTimeFormatter =
    new Intl.DateTimeFormat(
        'en-ZA',
        {
            dateStyle: 'medium',
            timeStyle: 'short',
        }
    )

export function BlindBidEvaluationCard({
                                           bidState,
                                           isWorkspaceReadOnly,
                                           onSubmit,
                                       }: BlindBidEvaluationCardProps) {
    const [
        formState,
        setFormState,
    ] =
        useState<BidEvaluationFormState>(
            EMPTY_BID_EVALUATION_FORM
        )

    const [
        formErrors,
        setFormErrors,
    ] =
        useState<BidEvaluationFormErrors>(
            {}
        )

    const {
        bid,
        evaluation,
        status,
        errorMessage,
    } = bidState

    const isSubmitting =
        status === 'SUBMITTING'

    const isScored =
        evaluation !== null
        || status === 'SCORED'

    const isFormDisabled =
        isWorkspaceReadOnly
        || isSubmitting
        || isScored



    function handleScoreChange(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setFormState(
            (currentState) => ({
                ...currentState,
                technicalScore:
                event.target.value,
            })
        )

        clearFormError(
            'technicalScore'
        )
    }

    function handleCommentsChange(
        event:
        ChangeEvent<HTMLTextAreaElement>,
    ): void {
        setFormState(
            (currentState) => ({
                ...currentState,
                comments:
                event.target.value,
            })
        )

        clearFormError(
            'comments'
        )
    }

    function clearFormError(
        field:
        keyof BidEvaluationFormErrors,
    ): void {
        setFormErrors(
            (currentErrors) => {
                const nextErrors = {
                    ...currentErrors,
                }

                delete nextErrors[field]
                delete nextErrors.form

                return nextErrors
            }
        )
    }

    async function handleSubmit(
        event:
        SyntheticEvent<HTMLFormElement>,
    ): Promise<void> {
        event.preventDefault()

        if (isFormDisabled) {
            return
        }

        const validationErrors =
            validateBidEvaluationForm(
                formState
            )

        if (
            Object.keys(
                validationErrors
            ).length > 0
        ) {
            setFormErrors(
                validationErrors
            )

            return
        }

        try {
            const request =
                buildEvaluateBidRequest(
                    formState
                )

            setFormErrors({})

            await onSubmit(
                bid.bidId,
                request
            )
        } catch (error) {
            setFormErrors({
                form:
                    getLocalErrorMessage(
                        error
                    ),
            })
        }
    }

    return (
        <article
            className={
                'blind-bid-evaluation-card '
                + getBidCardStatusClassName(
                    bidState
                )
            }
        >
            <header
                className={
                    'blind-bid-evaluation-header'
                }
            >
                <div>
                    <span
                        className={
                            'blind-bid-reference'
                        }
                    >
                        Blind bid
                        {' '}
                        {bid.bidId}
                    </span>

                    <h2>
                        {bid.bidderAlias}
                    </h2>

                    <p>
                        Tender ID:
                        {' '}
                        <strong>
                            {bid.tenderId}
                        </strong>
                    </p>
                </div>

                <EvaluationStateBadge
                    bidState={bidState}
                />
            </header>

            <section
                className={
                    'blind-bid-proposal'
                }
                aria-labelledby={
                    'blind-bid-proposal-'
                    + bid.bidId
                }
            >
                <span className="eyebrow">
                    Anonymous technical proposal
                </span>

                <h3
                    id={
                        'blind-bid-proposal-'
                        + bid.bidId
                    }
                >
                    Technical submission
                </h3>

                <p>
                    {bid.technicalProposal}
                </p>
            </section>

            {evaluation !== null ? (
                <PersistedEvaluationResult
                    bidState={bidState}
                />
            ) : (
                <form
                    className={
                        'blind-bid-evaluation-form'
                    }
                    onSubmit={handleSubmit}
                    noValidate
                >
                    <div
                        className={
                            'blind-bid-form-heading'
                        }
                    >
                        <div>
                            <span className="eyebrow">
                                Evaluator decision
                            </span>

                            <h3>
                                Record technical score
                            </h3>
                        </div>

                        <p>
                            Apply only the criteria
                            published before bidding
                            opened. The bidder’s real
                            identity remains hidden.
                        </p>
                    </div>

                    <div
                        className={
                            'blind-bid-form-grid'
                        }
                    >
                        <div className="form-field">
                            <label
                                htmlFor={
                                    'technical-score-'
                                    + bid.bidId
                                }
                            >
                                Technical score
                            </label>

                            <div
                                className={
                                    'technical-score-input'
                                }
                            >
                                <input
                                    id={
                                        'technical-score-'
                                        + bid.bidId
                                    }
                                    type="number"
                                    min="0"
                                    max="100"
                                    step="0.01"
                                    inputMode="decimal"
                                    value={
                                        formState
                                            .technicalScore
                                    }
                                    onChange={
                                        handleScoreChange
                                    }
                                    disabled={
                                        isFormDisabled
                                    }
                                    aria-invalid={
                                        formErrors
                                            .technicalScore
                                        !== undefined
                                    }
                                    aria-describedby={
                                        formErrors
                                            .technicalScore
                                            ? (
                                                'technical-score-error-'
                                                + bid.bidId
                                            )
                                            : (
                                                'technical-score-help-'
                                                + bid.bidId
                                            )
                                    }
                                    placeholder="0.00"
                                />

                                <span
                                    aria-hidden="true"
                                >
                                    / 100
                                </span>
                            </div>

                            <small
                                id={
                                    'technical-score-help-'
                                    + bid.bidId
                                }
                            >
                                Enter a value between
                                0 and 100.
                            </small>

                            {
                                formErrors
                                    .technicalScore
                                && (
                                    <p
                                        id={
                                            'technical-score-error-'
                                            + bid.bidId
                                        }
                                        className={
                                            'field-error'
                                        }
                                        role="alert"
                                    >
                                        {
                                            formErrors
                                                .technicalScore
                                        }
                                    </p>
                                )
                            }
                        </div>

                        <div
                            className={
                                'form-field '
                                + 'blind-bid-comments-field'
                            }
                        >
                            <label
                                htmlFor={
                                    'evaluation-comments-'
                                    + bid.bidId
                                }
                            >
                                Evaluation comments
                            </label>

                            <textarea
                                id={
                                    'evaluation-comments-'
                                    + bid.bidId
                                }
                                value={
                                    formState.comments
                                }
                                onChange={
                                    handleCommentsChange
                                }
                                disabled={
                                    isFormDisabled
                                }
                                rows={6}
                                minLength={10}
                                maxLength={1000}
                                aria-invalid={
                                    formErrors.comments
                                    !== undefined
                                }
                                aria-describedby={
                                    formErrors.comments
                                        ? (
                                            'evaluation-comments-error-'
                                            + bid.bidId
                                        )
                                        : (
                                            'evaluation-comments-help-'
                                            + bid.bidId
                                        )
                                }
                                placeholder={
                                    'Explain how the '
                                    + 'technical proposal '
                                    + 'meets or fails the '
                                    + 'published criteria.'
                                }
                            />

                            <div
                                className={
                                    'blind-bid-field-help'
                                }
                            >
                                <small
                                    id={
                                        'evaluation-comments-help-'
                                        + bid.bidId
                                    }
                                >
                                    Minimum 10 characters.
                                </small>

                                <small>
                                    {
                                        formState
                                            .comments
                                            .length
                                    }
                                    {' / 1000'}
                                </small>
                            </div>

                            {formErrors.comments && (
                                <p
                                    id={
                                        'evaluation-comments-error-'
                                        + bid.bidId
                                    }
                                    className="field-error"
                                    role="alert"
                                >
                                    {
                                        formErrors
                                            .comments
                                    }
                                </p>
                            )}
                        </div>
                    </div>

                    {formErrors.form && (
                        <p
                            className={
                                'form-error-message '
                                + 'blind-bid-form-error'
                            }
                            role="alert"
                        >
                            {formErrors.form}
                        </p>
                    )}

                    {errorMessage !== null && (
                        <p
                            className={
                                'form-error-message '
                                + 'blind-bid-form-error'
                            }
                            role="alert"
                        >
                            {errorMessage}
                        </p>
                    )}

                    {isWorkspaceReadOnly && (
                        <p
                            className={
                                'blind-bid-read-only-message'
                            }
                        >
                            Scoring is unavailable
                            because this tender is not
                            currently in the Evaluation
                            stage.
                        </p>
                    )}

                    <div
                        className={
                            'blind-bid-form-actions'
                        }
                    >
                        <button
                            className="primary-button"
                            type="submit"
                            disabled={
                                isFormDisabled
                            }
                        >
                            {isSubmitting
                                ? 'Submitting score...'
                                : 'Submit evaluation'}
                        </button>
                    </div>
                </form>
            )}
        </article>
    )
}

interface EvaluationStateBadgeProps {
    bidState: EvaluationBidState
}

function EvaluationStateBadge({
                                  bidState,
                              }: EvaluationStateBadgeProps) {
    return (
        <span
            className={
                'evaluation-state-badge '
                + getEvaluationStateBadgeClassName(
                    bidState.status
                )
            }
        >
            {
                getEvaluationStateLabel(
                    bidState
                )
            }
        </span>
    )
}

interface PersistedEvaluationResultProps {
    bidState: EvaluationBidState
}

function PersistedEvaluationResult({
                                       bidState,
                                   }: PersistedEvaluationResultProps) {
    const evaluation =
        bidState.evaluation

    if (evaluation === null) {
        return null
    }

    return (
        <section
            className={
                'persisted-evaluation-result'
            }
            aria-label={
                'Submitted bid evaluation'
            }
        >
            <div
                className={
                    'persisted-evaluation-heading'
                }
            >
                <div>
                    <span className="eyebrow">
                        Submitted evaluation
                    </span>

                    <h3>
                        Technical scoring recorded
                    </h3>
                </div>

                <strong>
                    {
                        formatTechnicalScore(
                            evaluation
                                .technicalScore
                        )
                    }
                </strong>
            </div>

            <dl
                className={
                    'persisted-evaluation-summary'
                }
            >
                <EvaluationResultItem
                    label="Technical score"
                    value={
                        formatTechnicalScore(
                            evaluation
                                .technicalScore
                        )
                    }
                />

                <EvaluationResultItem
                    label="Price score"
                    value={
                        formatTechnicalScore(
                            evaluation.priceScore
                        )
                    }
                />

                <EvaluationResultItem
                    label="Total score"
                    value={
                        formatTechnicalScore(
                            evaluation.totalScore
                        )
                    }
                />

                <EvaluationResultItem
                    label="Evaluator user ID"
                    value={
                        evaluation
                            .evaluatorUserId
                            .toString()
                    }
                />

                <EvaluationResultItem
                    label="Submitted"
                    value={
                        formatDateTime(
                            evaluation
                                .submittedAt
                        )
                    }
                />

                <EvaluationResultItem
                    label="Evaluation ID"
                    value={
                        evaluation.id.toString()
                    }
                />
            </dl>

            <div
                className={
                    'persisted-evaluation-comments'
                }
            >
                <span>
                    Evaluation comments
                </span>

                <p>
                    {evaluation.comments}
                </p>
            </div>

            <p
                className={
                    'evaluation-final-message'
                }
            >
                This evaluation has been submitted
                and cannot be changed from this
                workspace.
            </p>
        </section>
    )
}

interface EvaluationResultItemProps {
    label: string
    value: string
}

function EvaluationResultItem({
                                  label,
                                  value,
                              }: EvaluationResultItemProps) {
    return (
        <div>
            <dt>{label}</dt>
            <dd>{value}</dd>
        </div>
    )
}

function getBidCardStatusClassName(
    bidState: EvaluationBidState,
): string {
    switch (bidState.status) {
        case 'NOT_SCORED':
            return 'blind-bid-not-scored'

        case 'SUBMITTING':
            return 'blind-bid-submitting'

        case 'SCORED':
            return 'blind-bid-scored'

        case 'FAILED':
            return 'blind-bid-failed'
    }
}

function getEvaluationStateBadgeClassName(
    status:
    EvaluationBidState['status'],
): string {
    switch (status) {
        case 'NOT_SCORED':
            return 'evaluation-state-pending'

        case 'SUBMITTING':
            return 'evaluation-state-submitting'

        case 'SCORED':
            return 'evaluation-state-complete'

        case 'FAILED':
            return 'evaluation-state-failed'
    }
}

function getEvaluationStateLabel(
    bidState: EvaluationBidState,
): string {
    if (bidState.evaluation !== null) {
        return 'Evaluation submitted'
    }

    switch (bidState.status) {
        case 'NOT_SCORED':
            return 'Awaiting score'

        case 'SUBMITTING':
            return 'Submitting'

        case 'SCORED':
            return 'Evaluation submitted'

        case 'FAILED':
            return 'Submission failed'
    }
}

function getLocalErrorMessage(
    error: unknown,
): string {
    if (error instanceof Error) {
        const message =
            error.message.trim()

        if (message.length > 0) {
            return message
        }
    }

    return (
        'Unable to prepare the bid '
        + 'evaluation request.'
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