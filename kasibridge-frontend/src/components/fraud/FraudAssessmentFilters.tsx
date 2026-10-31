import {
    useEffect,
    useState,
} from 'react'

import type {
    ChangeEvent,
    FormEvent,
} from 'react'

import type {
    FraudAssessmentFilterState,
    FraudAssessmentSort,
    FraudClassification,
    FraudReviewStatus,
    FraudRiskLevel,
} from '../../types/fraudAssessment'

import {
    EMPTY_FRAUD_ASSESSMENT_FILTERS,
    FRAUD_ASSESSMENT_SORT_OPTIONS,
    FRAUD_CLASSIFICATION_LABELS,
    FRAUD_REVIEW_STATUS_LABELS,
    FRAUD_RISK_LEVEL_LABELS,
} from '../../types/fraudAssessment'

interface FraudAssessmentFiltersProps {
    filters: FraudAssessmentFilterState
    sort: FraudAssessmentSort | ''
    isLoading: boolean

    onApply: (
        filters: FraudAssessmentFilterState,
        sort: FraudAssessmentSort | '',
    ) => void

    onReset: () => void
}

const reviewStatusOptions:
    Array<{
        value: FraudReviewStatus
        label: string
    }> = [
    {
        value: 'PENDING_REVIEW',
        label:
        FRAUD_REVIEW_STATUS_LABELS
            .PENDING_REVIEW,
    },
    {
        value: 'AUTO_CLEARED',
        label:
        FRAUD_REVIEW_STATUS_LABELS
            .AUTO_CLEARED,
    },
    {
        value: 'CONFIRMED_FRAUD',
        label:
        FRAUD_REVIEW_STATUS_LABELS
            .CONFIRMED_FRAUD,
    },
    {
        value: 'FALSE_POSITIVE',
        label:
        FRAUD_REVIEW_STATUS_LABELS
            .FALSE_POSITIVE,
    },
    {
        value: 'ESCALATED',
        label:
        FRAUD_REVIEW_STATUS_LABELS
            .ESCALATED,
    },
]

const riskLevelOptions:
    Array<{
        value: FraudRiskLevel
        label: string
    }> = [
    {
        value: 'LOW',
        label:
        FRAUD_RISK_LEVEL_LABELS.LOW,
    },
    {
        value: 'MEDIUM',
        label:
        FRAUD_RISK_LEVEL_LABELS.MEDIUM,
    },
    {
        value: 'HIGH',
        label:
        FRAUD_RISK_LEVEL_LABELS.HIGH,
    },
    {
        value: 'CRITICAL',
        label:
        FRAUD_RISK_LEVEL_LABELS.CRITICAL,
    },
]

const classificationOptions:
    Array<{
        value: FraudClassification
        label: string
    }> = [
    {
        value: 'LEGITIMATE',
        label:
        FRAUD_CLASSIFICATION_LABELS
            .LEGITIMATE,
    },
    {
        value: 'HIGH_RISK',
        label:
        FRAUD_CLASSIFICATION_LABELS
            .HIGH_RISK,
    },
]

export function FraudAssessmentFilters({
                                           filters,
                                           sort,
                                           isLoading,
                                           onApply,
                                           onReset,
                                       }: FraudAssessmentFiltersProps) {
    const [
        draftFilters,
        setDraftFilters,
    ] =
        useState<FraudAssessmentFilterState>(
            filters
        )

    const [
        draftSort,
        setDraftSort,
    ] =
        useState<FraudAssessmentSort | ''>(
            sort
        )

    const [
        validationMessage,
        setValidationMessage,
    ] =
        useState<string | null>(
            null
        )

    useEffect(
        () => {
            queueMicrotask(
                () => {
                    setDraftFilters(filters)
                    setDraftSort(sort)
                }
            )
        },
        [
            filters,
            sort,
        ]
    )

    function updateReviewStatus(
        event:
        ChangeEvent<HTMLSelectElement>,
    ): void {
        const value =
            event.target.value

        setDraftFilters(
            (currentFilters) => ({
                ...currentFilters,
                reviewStatus:
                    value === ''
                        ? ''
                        : value as FraudReviewStatus,
            })
        )

        setValidationMessage(null)
    }

    function updateRiskLevel(
        event:
        ChangeEvent<HTMLSelectElement>,
    ): void {
        const value =
            event.target.value

        setDraftFilters(
            (currentFilters) => ({
                ...currentFilters,
                riskLevel:
                    value === ''
                        ? ''
                        : value as FraudRiskLevel,
            })
        )

        setValidationMessage(null)
    }

    function updateClassification(
        event:
        ChangeEvent<HTMLSelectElement>,
    ): void {
        const value =
            event.target.value

        setDraftFilters(
            (currentFilters) => ({
                ...currentFilters,
                classification:
                    value === ''
                        ? ''
                        : value as FraudClassification,
            })
        )

        setValidationMessage(null)
    }

    function updateSupplierId(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setDraftFilters(
            (currentFilters) => ({
                ...currentFilters,
                supplierId:
                event.target.value,
            })
        )

        setValidationMessage(null)
    }

    function updateDepartmentId(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setDraftFilters(
            (currentFilters) => ({
                ...currentFilters,
                departmentId:
                event.target.value,
            })
        )

        setValidationMessage(null)
    }

    function updateModelVersion(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setDraftFilters(
            (currentFilters) => ({
                ...currentFilters,
                modelVersion:
                event.target.value,
            })
        )

        setValidationMessage(null)
    }

    function updateAssessedFrom(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setDraftFilters(
            (currentFilters) => ({
                ...currentFilters,
                assessedFrom:
                event.target.value,
            })
        )

        setValidationMessage(null)
    }

    function updateAssessedTo(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setDraftFilters(
            (currentFilters) => ({
                ...currentFilters,
                assessedTo:
                event.target.value,
            })
        )

        setValidationMessage(null)
    }

    function updateSort(
        event:
        ChangeEvent<HTMLSelectElement>,
    ): void {
        const value =
            event.target.value

        setDraftSort(
            value === ''
                ? ''
                : value as FraudAssessmentSort
        )
    }

    function handleSubmit(
        event: FormEvent<HTMLFormElement>,
    ): void {
        event.preventDefault()

        const normalizedFilters =
            normalizeFilters(
                draftFilters
            )

        const dateRangeError =
            validateDateRange(
                normalizedFilters
            )

        if (dateRangeError !== null) {
            setValidationMessage(
                dateRangeError
            )

            return
        }

        setValidationMessage(null)

        onApply(
            normalizedFilters,
            draftSort
        )
    }

    function handleReset(): void {
        setDraftFilters({
            ...EMPTY_FRAUD_ASSESSMENT_FILTERS,
        })

        setDraftSort('')
        setValidationMessage(null)

        onReset()
    }

    return (
        <section
            className="fraud-filter-panel"
            aria-labelledby={
                'fraud-assessment-filter-heading'
            }
        >
            <div className="fraud-filter-heading">
                <div>
                    <span className="eyebrow">
                        Queue controls
                    </span>

                    <h2
                        id={
                            'fraud-assessment-filter-heading'
                        }
                    >
                        Filter assessments
                    </h2>

                    <p>
                        Narrow the analyst queue by
                        workflow status, risk, supplier,
                        department, model, or assessment
                        date.
                    </p>
                </div>

                {hasActiveFilters(
                    filters,
                    sort
                ) && (
                    <span
                        className={
                            'fraud-active-filter-indicator'
                        }
                    >
                        Filters applied
                    </span>
                )}
            </div>

            <form
                className="fraud-filter-form"
                onSubmit={handleSubmit}
            >
                <div className="fraud-filter-grid">
                    <div className="form-field">
                        <label
                            htmlFor={
                                'fraud-review-status'
                            }
                        >
                            Review status
                        </label>

                        <select
                            id="fraud-review-status"
                            value={
                                draftFilters
                                    .reviewStatus
                            }
                            onChange={
                                updateReviewStatus
                            }
                            disabled={isLoading}
                        >
                            <option value="">
                                All review statuses
                            </option>

                            {reviewStatusOptions.map(
                                (option) => (
                                    <option
                                        key={
                                            option.value
                                        }
                                        value={
                                            option.value
                                        }
                                    >
                                        {option.label}
                                    </option>
                                )
                            )}
                        </select>
                    </div>

                    <div className="form-field">
                        <label
                            htmlFor={
                                'fraud-risk-level'
                            }
                        >
                            Risk level
                        </label>

                        <select
                            id="fraud-risk-level"
                            value={
                                draftFilters
                                    .riskLevel
                            }
                            onChange={
                                updateRiskLevel
                            }
                            disabled={isLoading}
                        >
                            <option value="">
                                All risk levels
                            </option>

                            {riskLevelOptions.map(
                                (option) => (
                                    <option
                                        key={
                                            option.value
                                        }
                                        value={
                                            option.value
                                        }
                                    >
                                        {option.label}
                                    </option>
                                )
                            )}
                        </select>
                    </div>

                    <div className="form-field">
                        <label
                            htmlFor={
                                'fraud-classification'
                            }
                        >
                            Classification
                        </label>

                        <select
                            id="fraud-classification"
                            value={
                                draftFilters
                                    .classification
                            }
                            onChange={
                                updateClassification
                            }
                            disabled={isLoading}
                        >
                            <option value="">
                                All classifications
                            </option>

                            {
                                classificationOptions
                                    .map(
                                        (option) => (
                                            <option
                                                key={
                                                    option
                                                        .value
                                                }
                                                value={
                                                    option
                                                        .value
                                                }
                                            >
                                                {
                                                    option
                                                        .label
                                                }
                                            </option>
                                        )
                                    )
                            }
                        </select>
                    </div>

                    <div className="form-field">
                        <label htmlFor="fraud-sort">
                            Sort queue
                        </label>

                        <select
                            id="fraud-sort"
                            value={draftSort}
                            onChange={updateSort}
                            disabled={isLoading}
                        >
                            <option value="">
                                Priority queue order
                            </option>

                            {
                                FRAUD_ASSESSMENT_SORT_OPTIONS
                                    .map(
                                        (option) => (
                                            <option
                                                key={
                                                    option
                                                        .value
                                                }
                                                value={
                                                    option
                                                        .value
                                                }
                                            >
                                                {
                                                    option
                                                        .label
                                                }
                                            </option>
                                        )
                                    )
                            }
                        </select>
                    </div>

                    <div className="form-field">
                        <label
                            htmlFor={
                                'fraud-supplier-id'
                            }
                        >
                            Supplier ID
                        </label>

                        <input
                            id="fraud-supplier-id"
                            type="text"
                            value={
                                draftFilters
                                    .supplierId
                            }
                            onChange={
                                updateSupplierId
                            }
                            disabled={isLoading}
                            placeholder={
                                'SUPPLIER-DEMO-001'
                            }
                            autoComplete="off"
                        />
                    </div>

                    <div className="form-field">
                        <label
                            htmlFor={
                                'fraud-department-id'
                            }
                        >
                            Department ID
                        </label>

                        <input
                            id="fraud-department-id"
                            type="text"
                            value={
                                draftFilters
                                    .departmentId
                            }
                            onChange={
                                updateDepartmentId
                            }
                            disabled={isLoading}
                            placeholder="DPT-025"
                            autoComplete="off"
                        />
                    </div>

                    <div className="form-field">
                        <label
                            htmlFor={
                                'fraud-model-version'
                            }
                        >
                            Model version
                        </label>

                        <input
                            id="fraud-model-version"
                            type="text"
                            value={
                                draftFilters
                                    .modelVersion
                            }
                            onChange={
                                updateModelVersion
                            }
                            disabled={isLoading}
                            placeholder={
                                'procurement-fraud-v1'
                            }
                            autoComplete="off"
                        />
                    </div>

                    <div
                        className={
                            'form-field '
                            + 'fraud-filter-spacer'
                        }
                        aria-hidden="true"
                    />

                    <div className="form-field">
                        <label
                            htmlFor={
                                'fraud-assessed-from'
                            }
                        >
                            Assessed from
                        </label>

                        <input
                            id="fraud-assessed-from"
                            type="datetime-local"
                            value={
                                draftFilters
                                    .assessedFrom
                            }
                            onChange={
                                updateAssessedFrom
                            }
                            disabled={isLoading}
                        />
                    </div>

                    <div className="form-field">
                        <label
                            htmlFor={
                                'fraud-assessed-to'
                            }
                        >
                            Assessed to
                        </label>

                        <input
                            id="fraud-assessed-to"
                            type="datetime-local"
                            value={
                                draftFilters
                                    .assessedTo
                            }
                            onChange={
                                updateAssessedTo
                            }
                            disabled={isLoading}
                        />
                    </div>
                </div>

                {validationMessage !== null && (
                    <p
                        className={
                            'form-error-message '
                            + 'fraud-filter-error'
                        }
                        role="alert"
                    >
                        {validationMessage}
                    </p>
                )}

                <div className="fraud-filter-actions">
                    <button
                        className="secondary-button"
                        type="button"
                        onClick={handleReset}
                        disabled={isLoading}
                    >
                        Reset filters
                    </button>

                    <button
                        className="primary-button"
                        type="submit"
                        disabled={isLoading}
                    >
                        {isLoading
                            ? 'Loading queue...'
                            : 'Apply filters'}
                    </button>
                </div>
            </form>
        </section>
    )
}

function normalizeFilters(
    filters: FraudAssessmentFilterState,
): FraudAssessmentFilterState {
    return {
        ...filters,
        supplierId:
            filters.supplierId.trim(),
        departmentId:
            filters.departmentId.trim(),
        modelVersion:
            filters.modelVersion.trim(),
    }
}

function validateDateRange(
    filters: FraudAssessmentFilterState,
): string | null {
    if (
        filters.assessedFrom === ''
        || filters.assessedTo === ''
    ) {
        return null
    }

    const assessedFrom =
        new Date(
            filters.assessedFrom
        )

    const assessedTo =
        new Date(
            filters.assessedTo
        )

    if (
        Number.isNaN(
            assessedFrom.getTime()
        )
        || Number.isNaN(
            assessedTo.getTime()
        )
    ) {
        return (
            'Enter a valid assessment '
            + 'date range.'
        )
    }

    if (
        assessedFrom.getTime()
        > assessedTo.getTime()
    ) {
        return (
            'Assessment start time cannot '
            + 'be after assessment end time.'
        )
    }

    return null
}

function hasActiveFilters(
    filters: FraudAssessmentFilterState,
    sort: FraudAssessmentSort | '',
): boolean {
    return (
        filters.reviewStatus !== ''
        || filters.riskLevel !== ''
        || filters.classification !== ''
        || filters.supplierId.trim() !== ''
        || filters.departmentId.trim() !== ''
        || filters.modelVersion.trim() !== ''
        || filters.assessedFrom !== ''
        || filters.assessedTo !== ''
        || sort !== ''
    )
}