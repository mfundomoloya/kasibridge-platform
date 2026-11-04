import {
    type FormEvent,
    useState,
} from 'react'

import {
    getBidApiError,
    submitBid,
} from '../../api/bidApi'

import type {
    Bid,
} from '../../types/bid'

interface BidSubmissionFormProps {
    tenderId: number
    onBidSubmitted?: (
        bid: Bid,
    ) => void
    onCancel?: () => void
}

interface BidFormState {
    technicalProposal: string
    priceAmount: string
    csdValid: boolean
    taxClearanceValid: boolean
    bbbeeValid: boolean
    requiredDocumentsUploaded: boolean
}

interface BidFormErrors {
    technicalProposal?: string
    priceAmount?: string
    complianceDeclarations?: string
    general?: string
}

type ComplianceField =
    | 'csdValid'
    | 'taxClearanceValid'
    | 'bbbeeValid'
    | 'requiredDocumentsUploaded'

const initialFormState:
    BidFormState = {
    technicalProposal: '',
    priceAmount: '',
    csdValid: false,
    taxClearanceValid: false,
    bbbeeValid: false,
    requiredDocumentsUploaded: false,
}

export default function BidSubmissionForm({
                                              tenderId,
                                              onBidSubmitted,
                                              onCancel,
                                          }: BidSubmissionFormProps) {
    const [
        form,
        setForm,
    ] =
        useState<BidFormState>(
            initialFormState
        )

    const [
        errors,
        setErrors,
    ] =
        useState<BidFormErrors>(
            {}
        )

    const [
        isSubmitting,
        setIsSubmitting,
    ] = useState(false)

    const [
        submittedBid,
        setSubmittedBid,
    ] =
        useState<Bid | null>(
            null
        )

    function updateTextField(
        field:
            'technicalProposal'
            | 'priceAmount',
        value: string,
    ): void {
        setForm(
            (currentForm) => ({
                ...currentForm,
                [field]: value,
            })
        )

        setErrors(
            (currentErrors) => ({
                ...currentErrors,
                [field]: undefined,
                general: undefined,
            })
        )
    }

    function updateComplianceField(
        field: ComplianceField,
        checked: boolean,
    ): void {
        setForm(
            (currentForm) => ({
                ...currentForm,
                [field]: checked,
            })
        )

        setErrors(
            (currentErrors) => ({
                ...currentErrors,
                complianceDeclarations:
                undefined,
                general: undefined,
            })
        )
    }

    function validateForm():
        BidFormErrors {
        const validationErrors:
            BidFormErrors = {}

        const proposal =
            form.technicalProposal
                .trim()

        const amount =
            Number(
                form.priceAmount
            )

        if (!proposal) {
            validationErrors
                .technicalProposal =
                'Technical proposal '
                + 'is required.'
        }

        if (
            !form.priceAmount.trim()
        ) {
            validationErrors
                .priceAmount =
                'Bid price is required.'
        } else if (
            !Number.isFinite(amount)
        ) {
            validationErrors
                .priceAmount =
                'Enter a valid bid price.'
        } else if (amount <= 0) {
            validationErrors
                .priceAmount =
                'Bid price must be '
                + 'greater than zero.'
        }

        if (
            !form.csdValid
            || !form.taxClearanceValid
            || !form.bbbeeValid
            || !form
                .requiredDocumentsUploaded
        ) {
            validationErrors
                .complianceDeclarations =
                'Confirm all mandatory '
                + 'compliance declarations '
                + 'before submitting the bid.'
        }

        return validationErrors
    }

    async function handleSubmit(
        event:
        FormEvent<HTMLFormElement>,
    ): Promise<void> {
        event.preventDefault()

        const validationErrors =
            validateForm()

        if (
            Object.keys(
                validationErrors
            ).length > 0
        ) {
            setErrors(
                validationErrors
            )

            return
        }

        setIsSubmitting(true)
        setErrors({})

        try {
            const bid =
                await submitBid(
                    tenderId,
                    {
                        technicalProposal:
                            form
                                .technicalProposal
                                .trim(),

                        priceAmount:
                            Number(
                                form
                                    .priceAmount
                            ),

                        csdValid:
                        form.csdValid,

                        taxClearanceValid:
                        form
                            .taxClearanceValid,

                        bbbeeValid:
                        form.bbbeeValid,

                        requiredDocumentsUploaded:
                        form
                            .requiredDocumentsUploaded,
                    }
                )

            setSubmittedBid(bid)

            setForm(
                initialFormState
            )

            onBidSubmitted?.(
                bid
            )
        } catch (error) {
            const apiError =
                getBidApiError(
                    error
                )

            setErrors({
                general:
                apiError.message,

                technicalProposal:
                apiError
                    .fieldErrors
                    ?.technicalProposal,

                priceAmount:
                apiError
                    .fieldErrors
                    ?.priceAmount,

                complianceDeclarations:
                    getComplianceApiError(
                        apiError
                            .fieldErrors
                    ),
            })
        } finally {
            setIsSubmitting(false)
        }
    }

    const complianceConfirmed =
        form.csdValid
        && form.taxClearanceValid
        && form.bbbeeValid
        && form
            .requiredDocumentsUploaded

    if (submittedBid !== null) {
        return (
            <section
                className={
                    'bid-submission-success'
                }
                aria-live="polite"
            >
                <h2>
                    Bid submitted successfully
                </h2>

                <p>
                    Your bid reference is
                    {' '}
                    <strong>
                        {
                            submittedBid
                                .bidReference
                        }
                    </strong>
                    .
                </p>

                <p>
                    Status:
                    {' '}
                    <strong>
                        {
                            formatBidStatus(
                                submittedBid
                                    .status
                            )
                        }
                    </strong>
                </p>

                {submittedBid.compliance
                    !== null
                    && submittedBid.compliance
                    !== undefined && (
                        <div
                            className={
                                submittedBid
                                    .compliance
                                    .passed
                                    ? (
                                        'bid-compliance-result '
                                        + 'is-passed'
                                    )
                                    : (
                                        'bid-compliance-result '
                                        + 'is-failed'
                                    )
                            }
                        >
                        <span>
                            Compliance assessment
                        </span>

                            <strong>
                                {
                                    submittedBid
                                        .compliance
                                        .passed
                                        ? 'Passed'
                                        : 'Failed'
                                }
                            </strong>

                            {
                                submittedBid
                                    .compliance
                                    .failureReason
                                && (
                                    <p>
                                        {
                                            submittedBid
                                                .compliance
                                                .failureReason
                                        }
                                    </p>
                                )
                            }
                        </div>
                    )}
            </section>
        )
    }

    return (
        <section
            className={
                'bid-submission-form'
            }
        >
            <h2>
                Submit a bid
            </h2>

            <p>
                Provide the technical proposal,
                bid price and mandatory compliance
                declarations. The declarations are
                assessed immediately after
                submission.
            </p>

            {errors.general && (
                <div
                    className={
                        'form-error-message'
                    }
                    role="alert"
                >
                    {errors.general}
                </div>
            )}

            <form
                onSubmit={handleSubmit}
                noValidate
            >
                <div
                    className="form-group"
                >
                    <label
                        htmlFor={
                            'technicalProposal'
                        }
                    >
                        Technical proposal
                    </label>

                    <textarea
                        id={
                            'technicalProposal'
                        }
                        name={
                            'technicalProposal'
                        }
                        value={
                            form
                                .technicalProposal
                        }
                        onChange={
                            (event) =>
                                updateTextField(
                                    'technicalProposal',
                                    event
                                        .target
                                        .value
                                )
                        }
                        disabled={
                            isSubmitting
                        }
                        aria-invalid={
                            Boolean(
                                errors
                                    .technicalProposal
                            )
                        }
                        aria-describedby={
                            errors
                                .technicalProposal
                                ? (
                                    'technicalProposal-error'
                                )
                                : (
                                    'technicalProposal-help'
                                )
                        }
                    />

                    <p
                        id={
                            'technicalProposal-help'
                        }
                        className={
                            'form-field-help'
                        }
                    >
                        Explain the proposed
                        technical approach,
                        delivery capability,
                        experience and support
                        arrangements against the
                        published criteria.
                    </p>

                    {
                        errors
                            .technicalProposal
                        && (
                            <p
                                id={
                                    'technicalProposal-error'
                                }
                                className={
                                    'field-error'
                                }
                                role="alert"
                            >
                                {
                                    errors
                                        .technicalProposal
                                }
                            </p>
                        )
                    }
                </div>

                <div
                    className="form-group"
                >
                    <label
                        htmlFor={
                            'priceAmount'
                        }
                    >
                        Bid price
                    </label>

                    <input
                        id={
                            'priceAmount'
                        }
                        name={
                            'priceAmount'
                        }
                        type="number"
                        min="0.01"
                        step="0.01"
                        inputMode="decimal"
                        value={
                            form.priceAmount
                        }
                        onChange={
                            (event) =>
                                updateTextField(
                                    'priceAmount',
                                    event
                                        .target
                                        .value
                                )
                        }
                        disabled={
                            isSubmitting
                        }
                        aria-invalid={
                            Boolean(
                                errors
                                    .priceAmount
                            )
                        }
                        aria-describedby={
                            errors.priceAmount
                                ? (
                                    'priceAmount-error'
                                )
                                : (
                                    'priceAmount-help'
                                )
                        }
                    />

                    <p
                        id={
                            'priceAmount-help'
                        }
                        className={
                            'form-field-help'
                        }
                    >
                        Enter the total bid amount
                        in South African rand.
                    </p>

                    {errors.priceAmount && (
                        <p
                            id={
                                'priceAmount-error'
                            }
                            className={
                                'field-error'
                            }
                            role="alert"
                        >
                            {
                                errors
                                    .priceAmount
                            }
                        </p>
                    )}
                </div>

                <fieldset
                    className={
                        'bid-compliance-fieldset'
                    }
                    aria-describedby={
                        errors
                            .complianceDeclarations
                            ? (
                                'compliance-declarations-error'
                            )
                            : (
                                'compliance-declarations-help'
                            )
                    }
                >
                    <legend>
                        Mandatory compliance
                        declarations
                    </legend>

                    <p
                        id={
                            'compliance-declarations-help'
                        }
                        className={
                            'bid-compliance-introduction'
                        }
                    >
                        Confirm each statement
                        before submitting. False or
                        missing declarations cause
                        the automated compliance
                        assessment to fail.
                    </p>

                    <ComplianceDeclaration
                        id="csdValid"
                        checked={
                            form.csdValid
                        }
                        disabled={
                            isSubmitting
                        }
                        label={
                            'The business has a '
                            + 'valid and active '
                            + 'Central Supplier '
                            + 'Database registration.'
                        }
                        onChange={
                            (checked) =>
                                updateComplianceField(
                                    'csdValid',
                                    checked
                                )
                        }
                    />

                    <ComplianceDeclaration
                        id="taxClearanceValid"
                        checked={
                            form
                                .taxClearanceValid
                        }
                        disabled={
                            isSubmitting
                        }
                        label={
                            'The business has valid '
                            + 'tax-clearance or tax-'
                            + 'compliance status.'
                        }
                        onChange={
                            (checked) =>
                                updateComplianceField(
                                    'taxClearanceValid',
                                    checked
                                )
                        }
                    />

                    <ComplianceDeclaration
                        id="bbbeeValid"
                        checked={
                            form.bbbeeValid
                        }
                        disabled={
                            isSubmitting
                        }
                        label={
                            'The required valid '
                            + 'B-BBEE evidence is '
                            + 'available.'
                        }
                        onChange={
                            (checked) =>
                                updateComplianceField(
                                    'bbbeeValid',
                                    checked
                                )
                        }
                    />

                    <ComplianceDeclaration
                        id={
                            'requiredDocumentsUploaded'
                        }
                        checked={
                            form
                                .requiredDocumentsUploaded
                        }
                        disabled={
                            isSubmitting
                        }
                        label={
                            'All mandatory tender '
                            + 'documents have been '
                            + 'uploaded or supplied.'
                        }
                        onChange={
                            (checked) =>
                                updateComplianceField(
                                    'requiredDocumentsUploaded',
                                    checked
                                )
                        }
                    />

                    {
                        errors
                            .complianceDeclarations
                        && (
                            <p
                                id={
                                    'compliance-declarations-error'
                                }
                                className={
                                    'field-error '
                                    + 'bid-compliance-error'
                                }
                                role="alert"
                            >
                                {
                                    errors
                                        .complianceDeclarations
                                }
                            </p>
                        )
                    }

                    <div
                        className={
                            complianceConfirmed
                                ? (
                                    'bid-compliance-summary '
                                    + 'is-complete'
                                )
                                : (
                                    'bid-compliance-summary '
                                    + 'is-incomplete'
                                )
                        }
                        aria-live="polite"
                    >
                        <strong>
                            {
                                complianceConfirmed
                                    ? (
                                        'All compliance '
                                        + 'declarations '
                                        + 'confirmed'
                                    )
                                    : (
                                        'Compliance '
                                        + 'confirmation '
                                        + 'incomplete'
                                    )
                            }
                        </strong>

                        <span>
                            {
                                countConfirmedDeclarations(
                                    form
                                )
                            }
                            {' of 4 confirmed'}
                        </span>
                    </div>
                </fieldset>

                <div
                    className="form-actions"
                >
                    <button
                        className="primary-button"
                        type="submit"
                        disabled={
                            isSubmitting
                        }
                    >
                        {isSubmitting
                            ? 'Submitting bid...'
                            : 'Submit bid'}
                    </button>

                    {onCancel && (
                        <button
                            className={
                                'secondary-button'
                            }
                            type="button"
                            onClick={onCancel}
                            disabled={
                                isSubmitting
                            }
                        >
                            Cancel
                        </button>
                    )}
                </div>
            </form>
        </section>
    )
}

interface ComplianceDeclarationProps {
    id: ComplianceField
    checked: boolean
    disabled: boolean
    label: string
    onChange: (
        checked: boolean,
    ) => void
}

function ComplianceDeclaration({
                                   id,
                                   checked,
                                   disabled,
                                   label,
                                   onChange,
                               }: ComplianceDeclarationProps) {
    return (
        <label
            className={
                checked
                    ? (
                        'bid-compliance-option '
                        + 'is-confirmed'
                    )
                    : (
                        'bid-compliance-option'
                    )
            }
            htmlFor={id}
        >
            <input
                id={id}
                name={id}
                type="checkbox"
                checked={checked}
                onChange={
                    (event) =>
                        onChange(
                            event
                                .target
                                .checked
                        )
                }
                disabled={disabled}
            />

            <span>
                {label}
            </span>
        </label>
    )
}

function countConfirmedDeclarations(
    form: BidFormState,
): number {
    return [
        form.csdValid,
        form.taxClearanceValid,
        form.bbbeeValid,
        form.requiredDocumentsUploaded,
    ].filter(Boolean).length
}

function getComplianceApiError(
    fieldErrors:
        Record<string, string>
        | null,
): string | undefined {
    if (fieldErrors === null) {
        return undefined
    }

    return (
        fieldErrors.csdValid
        ?? fieldErrors
            .taxClearanceValid
        ?? fieldErrors.bbbeeValid
        ?? fieldErrors
            .requiredDocumentsUploaded
    )
}

function formatBidStatus(
    status: string,
): string {
    return status
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