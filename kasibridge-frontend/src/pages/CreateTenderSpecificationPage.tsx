import {
    useState,
} from 'react'

import type {
    ChangeEvent,
    SyntheticEvent,
} from 'react'

import {
    Link,
    useNavigate,
} from 'react-router-dom'

import {
    createProcurementTender,
    getProcurementTenderApiErrorMessage,
} from '../api/procurementTenderApi'

import {
    useAuth,
} from '../auth/useAuth'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import type {
    CreateProcurementTenderRequest,
} from '../types/procurementTender'

import './SpecificationTenderPage.css'

interface TenderSpecificationFormState {
    title: string
    description: string
    evaluationCriteria: string
    budgetAmount: string
    buyerOrgId: string
    requiredBusinessType: string
    serviceArea: string
}

interface TenderSpecificationFormErrors {
    title?: string
    description?: string
    evaluationCriteria?: string
    budgetAmount?: string
    buyerOrgId?: string
    requiredBusinessType?: string
    serviceArea?: string
    form?: string
}

const initialFormState:
    TenderSpecificationFormState = {
    title: '',
    description: '',
    evaluationCriteria: '',
    budgetAmount: '',
    buyerOrgId: '',
    requiredBusinessType: '',
    serviceArea: '',
}

export function CreateTenderSpecificationPage() {
    const navigate =
        useNavigate()

    const {
        user,
    } = useAuth()

    const [
        formState,
        setFormState,
    ] =
        useState<TenderSpecificationFormState>(
            initialFormState
        )

    const [
        formErrors,
        setFormErrors,
    ] =
        useState<TenderSpecificationFormErrors>(
            {}
        )

    const [
        isSubmitting,
        setIsSubmitting,
    ] = useState(false)

    function clearFieldError(
        field:
        keyof TenderSpecificationFormErrors,
    ): void {
        setFormErrors(
            (currentErrors) => {
                const updatedErrors = {
                    ...currentErrors,
                }

                delete updatedErrors[field]
                delete updatedErrors.form

                return updatedErrors
            }
        )
    }

    function handleTitleChange(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setFormState(
            (currentState) => ({
                ...currentState,
                title:
                event.target.value,
            })
        )

        clearFieldError('title')
    }

    function handleDescriptionChange(
        event:
        ChangeEvent<HTMLTextAreaElement>,
    ): void {
        setFormState(
            (currentState) => ({
                ...currentState,
                description:
                event.target.value,
            })
        )

        clearFieldError('description')
    }

    function handleEvaluationCriteriaChange(
        event:
        ChangeEvent<HTMLTextAreaElement>,
    ): void {
        setFormState(
            (currentState) => ({
                ...currentState,
                evaluationCriteria:
                event.target.value,
            })
        )

        clearFieldError(
            'evaluationCriteria'
        )
    }

    function handleBudgetAmountChange(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setFormState(
            (currentState) => ({
                ...currentState,
                budgetAmount:
                event.target.value,
            })
        )

        clearFieldError('budgetAmount')
    }

    function handleBuyerOrgIdChange(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setFormState(
            (currentState) => ({
                ...currentState,
                buyerOrgId:
                event.target.value,
            })
        )

        clearFieldError('buyerOrgId')
    }

    function handleBusinessTypeChange(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setFormState(
            (currentState) => ({
                ...currentState,
                requiredBusinessType:
                event.target.value,
            })
        )

        clearFieldError(
            'requiredBusinessType'
        )
    }

    function handleServiceAreaChange(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setFormState(
            (currentState) => ({
                ...currentState,
                serviceArea:
                event.target.value,
            })
        )

        clearFieldError('serviceArea')
    }

    async function handleSubmit(
        event:
        SyntheticEvent<HTMLFormElement>,
    ): Promise<void> {
        event.preventDefault()

        const validationErrors =
            validateTenderSpecification(
                formState,
                user?.userId ?? null
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

        if (user === null) {
            setFormErrors({
                form:
                    'An authenticated '
                    + 'Specification Officer '
                    + 'is required.',
            })

            return
        }

        setIsSubmitting(true)
        setFormErrors({})

        try {
            const request:
                CreateProcurementTenderRequest = {
                title:
                    formState.title.trim(),

                description:
                    formState.description.trim(),

                evaluationCriteria:
                    formState
                        .evaluationCriteria
                        .trim(),

                budgetAmount:
                    Number(
                        formState.budgetAmount
                    ),

                buyerOrgId:
                    formState
                        .buyerOrgId
                        .trim(),

                requiredBusinessType:
                    formState
                        .requiredBusinessType
                        .trim(),

                serviceArea:
                    formState
                        .serviceArea
                        .trim(),

                createdByUserId:
                user.userId,
            }

            const createdTender =
                await createProcurementTender(
                    request
                )

            navigate(
                '/procurement/specifications/'
                + createdTender.id,
                {
                    replace: true,
                    state: {
                        successMessage:
                            'Tender specification '
                            + 'created as a draft.',
                    },
                }
            )
        } catch (error) {
            setFormErrors({
                form:
                    getProcurementTenderApiErrorMessage(
                        error,
                        'Unable to create the '
                        + 'tender specification.'
                    ),
            })
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <ProcurementLayout>
            <nav
                className="back-navigation"
                aria-label={
                    'Specification navigation'
                }
            >
                <Link
                    to={
                        '/procurement/'
                        + 'specifications'
                    }
                >
                    Back to tender specifications
                </Link>
            </nav>

            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Specification workflow
                    </span>

                    <h1>
                        Create tender specification
                    </h1>

                    <p>
                        Define the procurement need,
                        objective evaluation criteria,
                        approved budget, supplier
                        requirements, and service area.
                        The tender remains in draft
                        until publication.
                    </p>
                </div>
            </section>

            <form
                className={
                    'specification-create-form'
                }
                onSubmit={handleSubmit}
                noValidate
            >
                <section
                    className={
                        'content-card '
                        + 'specification-form-section'
                    }
                    aria-labelledby={
                        'specification-overview-heading'
                    }
                >
                    <div
                        className={
                            'specification-form-heading'
                        }
                    >
                        <span className="eyebrow">
                            Procurement requirement
                        </span>

                        <h2
                            id={
                                'specification-overview-heading'
                            }
                        >
                            Tender overview
                        </h2>

                        <p>
                            Capture a clear title and
                            complete description of the
                            goods or services being
                            procured.
                        </p>
                    </div>

                    <div
                        className={
                            'specification-form-grid'
                        }
                    >
                        <div
                            className={
                                'form-field '
                                + 'specification-form-wide'
                            }
                        >
                            <label
                                htmlFor={
                                    'specification-title'
                                }
                            >
                                Tender title
                            </label>

                            <input
                                id="specification-title"
                                type="text"
                                value={
                                    formState.title
                                }
                                onChange={
                                    handleTitleChange
                                }
                                disabled={
                                    isSubmitting
                                }
                                maxLength={150}
                                aria-invalid={
                                    formErrors.title
                                    !== undefined
                                }
                                placeholder={
                                    'Supply and delivery '
                                    + 'of office equipment'
                                }
                            />

                            {formErrors.title && (
                                <p
                                    className="field-error"
                                    role="alert"
                                >
                                    {formErrors.title}
                                </p>
                            )}
                        </div>

                        <div
                            className={
                                'form-field '
                                + 'specification-form-wide'
                            }
                        >
                            <label
                                htmlFor={
                                    'specification-description'
                                }
                            >
                                Tender description
                            </label>

                            <textarea
                                id={
                                    'specification-description'
                                }
                                value={
                                    formState
                                        .description
                                }
                                onChange={
                                    handleDescriptionChange
                                }
                                disabled={
                                    isSubmitting
                                }
                                rows={7}
                                maxLength={3000}
                                aria-invalid={
                                    formErrors
                                        .description
                                    !== undefined
                                }
                                placeholder={
                                    'Describe the '
                                    + 'procurement need, '
                                    + 'scope, expected '
                                    + 'deliverables, and '
                                    + 'service conditions.'
                                }
                            />

                            <div
                                className={
                                    'specification-field-help'
                                }
                            >
                                <small>
                                    Include enough detail
                                    for comparable bids.
                                </small>

                                <small>
                                    {
                                        formState
                                            .description
                                            .length
                                    }
                                    {' / 3000'}
                                </small>
                            </div>

                            {formErrors.description && (
                                <p
                                    className="field-error"
                                    role="alert"
                                >
                                    {
                                        formErrors
                                            .description
                                    }
                                </p>
                            )}
                        </div>
                    </div>
                </section>

                <section
                    className={
                        'content-card '
                        + 'specification-form-section'
                    }
                    aria-labelledby={
                        'specification-evaluation-heading'
                    }
                >
                    <div
                        className={
                            'specification-form-heading'
                        }
                    >
                        <span className="eyebrow">
                            Locked evaluation basis
                        </span>

                        <h2
                            id={
                                'specification-evaluation-heading'
                            }
                        >
                            Evaluation criteria
                        </h2>

                        <p>
                            Define objective criteria,
                            scoring weights, and any
                            qualifying threshold before
                            publication.
                        </p>
                    </div>

                    <div className="form-field">
                        <label
                            htmlFor={
                                'specification-evaluation-criteria'
                            }
                        >
                            Published evaluation
                            criteria
                        </label>

                        <textarea
                            id={
                                'specification-evaluation-criteria'
                            }
                            value={
                                formState
                                    .evaluationCriteria
                            }
                            onChange={
                                handleEvaluationCriteriaChange
                            }
                            disabled={
                                isSubmitting
                            }
                            rows={9}
                            maxLength={3000}
                            aria-invalid={
                                formErrors
                                    .evaluationCriteria
                                !== undefined
                            }
                            placeholder={
                                '1. Technical approach: '
                                + '40 points\n'
                                + '2. Relevant experience: '
                                + '30 points\n'
                                + '3. Delivery capacity: '
                                + '30 points\n'
                                + 'Minimum qualifying '
                                + 'score: 70 points'
                            }
                        />

                        <div
                            className={
                                'specification-field-help'
                            }
                        >
                            <small>
                                Criteria become part of
                                the published immutable
                                specification evidence.
                            </small>

                            <small>
                                {
                                    formState
                                        .evaluationCriteria
                                        .length
                                }
                                {' / 3000'}
                            </small>
                        </div>

                        {
                            formErrors
                                .evaluationCriteria
                            && (
                                <p
                                    className={
                                        'field-error'
                                    }
                                    role="alert"
                                >
                                    {
                                        formErrors
                                            .evaluationCriteria
                                    }
                                </p>
                            )
                        }
                    </div>
                </section>

                <section
                    className={
                        'content-card '
                        + 'specification-form-section'
                    }
                    aria-labelledby={
                        'specification-commercial-heading'
                    }
                >
                    <div
                        className={
                            'specification-form-heading'
                        }
                    >
                        <span className="eyebrow">
                            Commercial and matching data
                        </span>

                        <h2
                            id={
                                'specification-commercial-heading'
                            }
                        >
                            Budget and supplier profile
                        </h2>

                        <p>
                            Define the approved budget,
                            buying organisation, required
                            business type, and service
                            area.
                        </p>
                    </div>

                    <div
                        className={
                            'specification-form-grid'
                        }
                    >
                        <div className="form-field">
                            <label
                                htmlFor={
                                    'specification-budget'
                                }
                            >
                                Budget amount
                            </label>

                            <input
                                id="specification-budget"
                                type="number"
                                min="0.01"
                                step="0.01"
                                value={
                                    formState
                                        .budgetAmount
                                }
                                onChange={
                                    handleBudgetAmountChange
                                }
                                disabled={
                                    isSubmitting
                                }
                                inputMode="decimal"
                                aria-invalid={
                                    formErrors
                                        .budgetAmount
                                    !== undefined
                                }
                                placeholder="250000.00"
                            />

                            <small>
                                Approved maximum budget
                                in South African rand.
                            </small>

                            {formErrors.budgetAmount && (
                                <p
                                    className="field-error"
                                    role="alert"
                                >
                                    {
                                        formErrors
                                            .budgetAmount
                                    }
                                </p>
                            )}
                        </div>

                        <div className="form-field">
                            <label
                                htmlFor={
                                    'specification-buyer-org'
                                }
                            >
                                Buyer organisation ID
                            </label>

                            <input
                                id={
                                    'specification-buyer-org'
                                }
                                type="text"
                                value={
                                    formState
                                        .buyerOrgId
                                }
                                onChange={
                                    handleBuyerOrgIdChange
                                }
                                disabled={
                                    isSubmitting
                                }
                                autoComplete="off"
                                aria-invalid={
                                    formErrors
                                        .buyerOrgId
                                    !== undefined
                                }
                                placeholder="DPT-025"
                            />

                            {formErrors.buyerOrgId && (
                                <p
                                    className="field-error"
                                    role="alert"
                                >
                                    {
                                        formErrors
                                            .buyerOrgId
                                    }
                                </p>
                            )}
                        </div>

                        <div className="form-field">
                            <label
                                htmlFor={
                                    'specification-business-type'
                                }
                            >
                                Required business type
                            </label>

                            <input
                                id={
                                    'specification-business-type'
                                }
                                type="text"
                                value={
                                    formState
                                        .requiredBusinessType
                                }
                                onChange={
                                    handleBusinessTypeChange
                                }
                                disabled={
                                    isSubmitting
                                }
                                maxLength={150}
                                autoComplete="off"
                                aria-invalid={
                                    formErrors
                                        .requiredBusinessType
                                    !== undefined
                                }
                                placeholder={
                                    'Office equipment '
                                    + 'supplier'
                                }
                            />

                            {
                                formErrors
                                    .requiredBusinessType
                                && (
                                    <p
                                        className={
                                            'field-error'
                                        }
                                        role="alert"
                                    >
                                        {
                                            formErrors
                                                .requiredBusinessType
                                        }
                                    </p>
                                )
                            }
                        </div>

                        <div className="form-field">
                            <label
                                htmlFor={
                                    'specification-service-area'
                                }
                            >
                                Service area
                            </label>

                            <input
                                id={
                                    'specification-service-area'
                                }
                                type="text"
                                value={
                                    formState
                                        .serviceArea
                                }
                                onChange={
                                    handleServiceAreaChange
                                }
                                disabled={
                                    isSubmitting
                                }
                                maxLength={150}
                                autoComplete="off"
                                aria-invalid={
                                    formErrors
                                        .serviceArea
                                    !== undefined
                                }
                                placeholder="Gauteng"
                            />

                            {formErrors.serviceArea && (
                                <p
                                    className="field-error"
                                    role="alert"
                                >
                                    {
                                        formErrors
                                            .serviceArea
                                    }
                                </p>
                            )}
                        </div>
                    </div>
                </section>

                <section
                    className={
                        'specification-submission-notice'
                    }
                >
                    <div>
                        <span className="eyebrow">
                            Draft control
                        </span>

                        <h2>
                            Save as draft
                        </h2>

                        <p>
                            Creating the specification
                            does not publish the tender.
                            Review the draft before
                            publication. Publication
                            generates the specification
                            evidence hash.
                        </p>
                    </div>

                    <dl>
                        <div>
                            <dt>
                                Authenticated creator
                            </dt>

                            <dd>
                                {user?.username
                                    ?? 'Unavailable'}
                            </dd>
                        </div>

                        <div>
                            <dt>
                                Creator user ID
                            </dt>

                            <dd>
                                {user?.userId
                                    ?? 'Unavailable'}
                            </dd>
                        </div>

                        <div>
                            <dt>Initial status</dt>
                            <dd>DRAFT</dd>
                        </div>
                    </dl>
                </section>

                {formErrors.form && (
                    <p
                        className={
                            'form-error-message '
                            + 'specification-form-error'
                        }
                        role="alert"
                    >
                        {formErrors.form}
                    </p>
                )}

                <div
                    className={
                        'specification-form-actions'
                    }
                >
                    <Link
                        className={
                            'secondary-link-button'
                        }
                        to={
                            '/procurement/'
                            + 'specifications'
                        }
                    >
                        Cancel
                    </Link>

                    <button
                        className="primary-button"
                        type="submit"
                        disabled={isSubmitting}
                    >
                        {isSubmitting
                            ? 'Creating draft...'
                            : 'Create draft tender'}
                    </button>
                </div>
            </form>
        </ProcurementLayout>
    )
}

function validateTenderSpecification(
    formState:
    TenderSpecificationFormState,
    authenticatedUserId: number | null,
): TenderSpecificationFormErrors {
    const errors:
        TenderSpecificationFormErrors = {}

    const title =
        formState.title.trim()

    if (title.length === 0) {
        errors.title =
            'Tender title is required.'
    } else if (title.length > 150) {
        errors.title =
            'Tender title cannot exceed '
            + '150 characters.'
    }

    const description =
        formState.description.trim()

    if (description.length === 0) {
        errors.description =
            'Tender description is required.'
    } else if (
        description.length > 3000
    ) {
        errors.description =
            'Tender description cannot exceed '
            + '3000 characters.'
    }

    const evaluationCriteria =
        formState
            .evaluationCriteria
            .trim()

    if (
        evaluationCriteria.length === 0
    ) {
        errors.evaluationCriteria =
            'Evaluation criteria are required.'
    } else if (
        evaluationCriteria.length > 3000
    ) {
        errors.evaluationCriteria =
            'Evaluation criteria cannot exceed '
            + '3000 characters.'
    }

    const budgetText =
        formState.budgetAmount.trim()

    const budgetAmount =
        Number(budgetText)

    if (budgetText.length === 0) {
        errors.budgetAmount =
            'Budget amount is required.'
    } else if (
        !Number.isFinite(
            budgetAmount
        )
        || budgetAmount <= 0
    ) {
        errors.budgetAmount =
            'Budget amount must be greater '
            + 'than zero.'
    }

    if (
        formState.buyerOrgId
            .trim()
            .length === 0
    ) {
        errors.buyerOrgId =
            'Buyer organisation ID is required.'
    }

    const requiredBusinessType =
        formState
            .requiredBusinessType
            .trim()

    if (
        requiredBusinessType.length === 0
    ) {
        errors.requiredBusinessType =
            'Required business type is required.'
    } else if (
        requiredBusinessType.length > 150
    ) {
        errors.requiredBusinessType =
            'Required business type cannot '
            + 'exceed 150 characters.'
    }

    const serviceArea =
        formState.serviceArea.trim()

    if (serviceArea.length === 0) {
        errors.serviceArea =
            'Service area is required.'
    } else if (
        serviceArea.length > 150
    ) {
        errors.serviceArea =
            'Service area cannot exceed '
            + '150 characters.'
    }

    if (
        authenticatedUserId === null
        || !Number.isInteger(
            authenticatedUserId
        )
        || authenticatedUserId <= 0
    ) {
        errors.form =
            'An authenticated creator '
            + 'account is required.'
    }

    return errors
}