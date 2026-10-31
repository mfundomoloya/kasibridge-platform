import {
    useEffect,
    useRef,
    useState,
} from 'react'

import type {
    FormEvent,
    SyntheticEvent,
} from 'react'

import type {
    FraudAssessment,
    FraudReviewDecision,
} from '../../types/fraudAssessment'

interface FraudAssessmentReviewDialogProps {
    assessment: FraudAssessment | null
    decision: FraudReviewDecision | null
    isSubmitting: boolean
    errorMessage: string | null

    onSubmit: (
        assessment: FraudAssessment,
        decision: FraudReviewDecision,
        reviewNotes: string,
    ) => Promise<void>

    onClose: () => void
}

interface DecisionContent {
    eyebrow: string
    title: string
    description: string
    notesLabel: string
    notesPlaceholder: string
    submitLabel: string
    submitClassName: string
}

const minimumNotesLength = 5
const maximumNotesLength = 1000

export function FraudAssessmentReviewDialog({
                                                assessment,
                                                decision,
                                                isSubmitting,
                                                errorMessage,
                                                onSubmit,
                                                onClose,
                                            }: FraudAssessmentReviewDialogProps) {
    const dialogReference =
        useRef<HTMLDialogElement | null>(
            null
        )

    const [
        reviewNotes,
        setReviewNotes,
    ] = useState('')

    const [
        validationMessage,
        setValidationMessage,
    ] =
        useState<string | null>(
            null
        )

    const isOpen =
        assessment !== null
        && decision !== null

    useEffect(
        () => {
            const dialog =
                dialogReference.current

            if (!dialog) {
                return
            }

            if (
                isOpen
                && !dialog.open
            ) {
                dialog.showModal()
            }

            if (
                !isOpen
                && dialog.open
            ) {
                dialog.close()
            }
        },
        [
            isOpen,
        ]
    )

    useEffect(
        () => {
            if (!isOpen) {
                queueMicrotask(
                    () => {
                        setReviewNotes('')
                        setValidationMessage(null)
                    }
                )
            }
        },
        [
            isOpen,
        ]
    )

    function handleDialogCancel(
        event: SyntheticEvent<
            HTMLDialogElement
        >
    ): void {
        event.preventDefault()

        if (!isSubmitting) {
            onClose()
        }
    }

    function handleDialogClose(): void {
        if (
            !isSubmitting
            && isOpen
        ) {
            onClose()
        }
    }

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> {
        event.preventDefault()

        if (
            assessment === null
            || decision === null
        ) {
            return
        }

        const normalizedNotes =
            reviewNotes.trim()

        if (
            normalizedNotes.length
            < minimumNotesLength
        ) {
            setValidationMessage(
                'Review notes must contain at '
                + 'least 5 characters.'
            )

            return
        }

        if (
            normalizedNotes.length
            > maximumNotesLength
        ) {
            setValidationMessage(
                'Review notes cannot exceed '
                + '1000 characters.'
            )

            return
        }

        setValidationMessage(null)

        await onSubmit(
            assessment,
            decision,
            normalizedNotes
        )
    }

    const decisionContent =
        decision === null
            ? null
            : getDecisionContent(
                decision
            )

    return (
        <dialog
            ref={dialogReference}
            className="fraud-review-dialog"
            aria-labelledby={
                'fraud-review-dialog-heading'
            }
            onCancel={handleDialogCancel}
            onClose={handleDialogClose}
        >
            {assessment !== null
                && decision !== null
                && decisionContent !== null && (
                    <form
                        className={
                            'fraud-review-dialog-content'
                        }
                        onSubmit={handleSubmit}
                    >
                        <div
                            className={
                                'fraud-review-dialog-heading'
                            }
                        >
                            <div>
                            <span className="eyebrow">
                                {
                                    decisionContent
                                        .eyebrow
                                }
                            </span>

                                <h2
                                    id={
                                        'fraud-review-dialog-heading'
                                    }
                                >
                                    {
                                        decisionContent
                                            .title
                                    }
                                </h2>
                            </div>

                            <button
                                className={
                                    'text-button '
                                    + 'fraud-dialog-close'
                                }
                                type="button"
                                onClick={onClose}
                                disabled={isSubmitting}
                                aria-label={
                                    'Close review dialog'
                                }
                            >
                                Close
                            </button>
                        </div>

                        <p
                            className={
                                'fraud-review-dialog-description'
                            }
                        >
                            {
                                decisionContent
                                    .description
                            }
                        </p>

                        <div
                            className={
                                'fraud-review-assessment-summary'
                            }
                        >
                            <div>
                            <span>
                                Assessment
                            </span>

                                <strong>
                                    {
                                        assessment
                                            .assessmentReference
                                    }
                                </strong>
                            </div>

                            <div>
                            <span>
                                Invoice
                            </span>

                                <strong>
                                    {
                                        assessment
                                            .invoiceReference
                                    }
                                </strong>
                            </div>

                            <div>
                            <span>
                                Current status
                            </span>

                                <strong>
                                    {
                                        formatEnumLabel(
                                            assessment
                                                .reviewStatus
                                        )
                                    }
                                </strong>
                            </div>

                            <div>
                            <span>
                                Record version
                            </span>

                                <strong>
                                    {
                                        assessment
                                            .version
                                    }
                                </strong>
                            </div>
                        </div>

                        <div className="form-field">
                            <label
                                htmlFor={
                                    'fraud-review-notes'
                                }
                            >
                                {
                                    decisionContent
                                        .notesLabel
                                }
                            </label>

                            <textarea
                                id="fraud-review-notes"
                                value={reviewNotes}
                                onChange={
                                    (event) => {
                                        setReviewNotes(
                                            event.target
                                                .value
                                        )

                                        setValidationMessage(
                                            null
                                        )
                                    }
                                }
                                disabled={isSubmitting}
                                placeholder={
                                    decisionContent
                                        .notesPlaceholder
                                }
                                minLength={
                                    minimumNotesLength
                                }
                                maxLength={
                                    maximumNotesLength
                                }
                                rows={7}
                                autoFocus
                                required
                            />

                            <div
                                className={
                                    'fraud-review-notes-help'
                                }
                            >
                                <small>
                                    Provide evidence-based
                                    notes for the immutable
                                    audit trail.
                                </small>

                                <small>
                                    {
                                        reviewNotes
                                            .length
                                    }
                                    {' / '}
                                    {
                                        maximumNotesLength
                                    }
                                </small>
                            </div>
                        </div>

                        {validationMessage !== null && (
                            <p
                                className={
                                    'form-error-message'
                                }
                                role="alert"
                            >
                                {validationMessage}
                            </p>
                        )}

                        {errorMessage !== null && (
                            <p
                                className={
                                    'form-error-message'
                                }
                                role="alert"
                            >
                                {errorMessage}
                            </p>
                        )}

                        <div
                            className={
                                'fraud-review-dialog-actions'
                            }
                        >
                            <button
                                className={
                                    'secondary-button'
                                }
                                type="button"
                                onClick={onClose}
                                disabled={isSubmitting}
                            >
                                Cancel
                            </button>

                            <button
                                className={
                                    decisionContent
                                        .submitClassName
                                }
                                type="submit"
                                disabled={isSubmitting}
                            >
                                {isSubmitting
                                    ? 'Submitting...'
                                    : decisionContent
                                        .submitLabel}
                            </button>
                        </div>
                    </form>
                )}
        </dialog>
    )
}

function getDecisionContent(
    decision: FraudReviewDecision,
): DecisionContent {
    switch (decision) {
        case 'CONFIRM_FRAUD':
            return {
                eyebrow:
                    'Final analyst decision',

                title:
                    'Confirm fraudulent activity',

                description:
                    'This action records the '
                    + 'assessment as confirmed fraud. '
                    + 'The decision becomes a final '
                    + 'workflow state and cannot be '
                    + 'replaced by a false-positive '
                    + 'decision.',

                notesLabel:
                    'Fraud confirmation notes',

                notesPlaceholder:
                    'Describe the evidence that '
                    + 'confirms fraudulent invoice '
                    + 'activity.',

                submitLabel:
                    'Confirm fraud',

                submitClassName:
                    'danger-button '
                    + 'fraud-dialog-submit',
            }

        case 'FALSE_POSITIVE':
            return {
                eyebrow:
                    'Final analyst decision',

                title:
                    'Mark as false positive',

                description:
                    'This action records the '
                    + 'assessment as legitimate after '
                    + 'review. The decision becomes a '
                    + 'final workflow state.',

                notesLabel:
                    'False-positive review notes',

                notesPlaceholder:
                    'Describe the documents or '
                    + 'evidence that verify the '
                    + 'invoice as legitimate.',

                submitLabel:
                    'Mark false positive',

                submitClassName:
                    'primary-button '
                    + 'fraud-dialog-submit',
            }

        case 'ESCALATE':
            return {
                eyebrow:
                    'Senior investigation',

                title:
                    'Escalate assessment',

                description:
                    'Escalation sends the assessment '
                    + 'for senior investigation. Only '
                    + 'platform administrators can '
                    + 'perform this action.',

                notesLabel:
                    'Escalation reason',

                notesPlaceholder:
                    'Explain why the assessment '
                    + 'requires senior investigation '
                    + 'or additional evidence.',

                submitLabel:
                    'Escalate assessment',

                submitClassName:
                    'primary-button '
                    + 'fraud-dialog-submit '
                    + 'fraud-dialog-escalate',
            }
    }
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
                    : word
                        .charAt(0)
                        .toUpperCase()
                    + word.slice(1)
        )
        .join(' ')
}