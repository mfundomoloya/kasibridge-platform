import {
    useCallback,
    useEffect,
    useState,
} from 'react'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import {
    FraudAssessmentDetailsPanel,
} from '../components/fraud/FraudAssessmentDetailsPanel'

import {
    FraudAssessmentFilters,
} from '../components/fraud/FraudAssessmentFilters'

import {
    FraudAssessmentReviewDialog,
} from '../components/fraud/FraudAssessmentReviewDialog'

import {
    FraudAssessmentTable,
} from '../components/fraud/FraudAssessmentTable'

import {
    FraudMetricCard,
} from '../components/fraud/FraudMetricCard'

import {
    confirmFraudAssessment,
    escalateFraudAssessment,
    getFraudApiErrorMessage,
    getFraudAssessment,
    getFraudAssessmentDashboard,
    getFraudAssessments,
    isFraudAssessmentStateConflict,
    isFraudAssessmentVersionConflict,
    markFraudAssessmentFalsePositive,
} from '../api/fraudAssessmentApi'

import {
    useAuth,
} from '../auth/useAuth'

import type {
    FraudAssessment,
    FraudAssessmentDashboard,
    FraudAssessmentFilterState,
    FraudAssessmentPage,
    FraudAssessmentQuery,
    FraudAssessmentSort,
    FraudReviewDecision,
} from '../types/fraudAssessment'

import {
    EMPTY_FRAUD_ASSESSMENT_FILTERS,
} from '../types/fraudAssessment'

import './FraudAssessmentDashboardPage.css'

const pageSize = 20

export function FraudAssessmentDashboardPage() {
    const {
        hasRole,
    } = useAuth()

    const [
        dashboard,
        setDashboard,
    ] =
        useState<
            FraudAssessmentDashboard | null
        >(null)

    const [
        assessmentPage,
        setAssessmentPage,
    ] =
        useState<FraudAssessmentPage | null>(
            null
        )

    const [
        filters,
        setFilters,
    ] =
        useState<FraudAssessmentFilterState>({
            ...EMPTY_FRAUD_ASSESSMENT_FILTERS,
        })

    const [
        sort,
        setSort,
    ] =
        useState<FraudAssessmentSort | ''>(
            ''
        )

    const [
        pageNumber,
        setPageNumber,
    ] = useState(0)

    const [
        selectedAssessment,
        setSelectedAssessment,
    ] =
        useState<FraudAssessment | null>(
            null
        )

    const [
        reviewDecision,
        setReviewDecision,
    ] =
        useState<FraudReviewDecision | null>(
            null
        )

    const [
        isDashboardLoading,
        setIsDashboardLoading,
    ] = useState(true)

    const [
        isQueueLoading,
        setIsQueueLoading,
    ] = useState(true)

    const [
        isAssessmentRefreshing,
        setIsAssessmentRefreshing,
    ] = useState(false)

    const [
        isReviewSubmitting,
        setIsReviewSubmitting,
    ] = useState(false)

    const [
        dashboardError,
        setDashboardError,
    ] =
        useState<string | null>(
            null
        )

    const [
        queueError,
        setQueueError,
    ] =
        useState<string | null>(
            null
        )

    const [
        reviewError,
        setReviewError,
    ] =
        useState<string | null>(
            null
        )

    const [
        successMessage,
        setSuccessMessage,
    ] =
        useState<string | null>(
            null
        )

    const isPlatformAdministrator =
        hasRole(
            'ROLE_PLATFORM_ADMIN'
        )

    const loadDashboard =
        useCallback(
            async (): Promise<void> => {
                setIsDashboardLoading(true)
                setDashboardError(null)

                try {
                    const response =
                        await getFraudAssessmentDashboard()

                    setDashboard(response)
                } catch (error) {
                    setDashboardError(
                        getFraudApiErrorMessage(
                            error,
                            'Unable to load fraud '
                            + 'dashboard metrics.'
                        )
                    )
                } finally {
                    setIsDashboardLoading(false)
                }
            },
            []
        )

    const loadAssessmentQueue =
        useCallback(
            async (): Promise<void> => {
                setIsQueueLoading(true)
                setQueueError(null)

                try {
                    const query =
                        buildAssessmentQuery(
                            filters,
                            sort,
                            pageNumber
                        )

                    const response =
                        await getFraudAssessments(
                            query
                        )

                    setAssessmentPage(response)

                    setSelectedAssessment(
                        (currentAssessment) =>
                            reconcileSelection(
                                currentAssessment,
                                response
                                    .content
                            )
                    )
                } catch (error) {
                    setQueueError(
                        getFraudApiErrorMessage(
                            error,
                            'Unable to load the '
                            + 'fraud assessment '
                            + 'queue.'
                        )
                    )
                } finally {
                    setIsQueueLoading(false)
                }
            },
            [
                filters,
                sort,
                pageNumber,
            ]
        )

    useEffect(
        () => {
            queueMicrotask(
                () => {
                    void loadDashboard()
                }
            )
        },
        [
            loadDashboard,
        ]
    )

    useEffect(
        () => {
            queueMicrotask(
                () => {
                    void loadAssessmentQueue()
                }
            )
        },
        [
            loadAssessmentQueue,
        ]
    )

    useEffect(
        () => {
            if (successMessage === null) {
                return
            }

            const dismissalTimer =
                window.setTimeout(
                    () => {
                        setSuccessMessage(null)
                    },
                    5000
                )

            return () => {
                window.clearTimeout(
                    dismissalTimer
                )
            }
        },
        [
            successMessage,
        ]
    )

    function handleApplyFilters(
        nextFilters:
        FraudAssessmentFilterState,
        nextSort:
            FraudAssessmentSort | '',
    ): void {
        setSuccessMessage(null)
        setQueueError(null)
        setSelectedAssessment(null)
        setPageNumber(0)
        setFilters(nextFilters)
        setSort(nextSort)
    }

    function handleResetFilters(): void {
        setSuccessMessage(null)
        setQueueError(null)
        setSelectedAssessment(null)
        setPageNumber(0)

        setFilters({
            ...EMPTY_FRAUD_ASSESSMENT_FILTERS,
        })

        setSort('')
    }

    function handlePageChange(
        nextPageNumber: number,
    ): void {
        if (nextPageNumber < 0) {
            return
        }

        setSuccessMessage(null)
        setSelectedAssessment(null)
        setPageNumber(nextPageNumber)
    }

    function handleSelectAssessment(
        assessment: FraudAssessment,
    ): void {
        setSelectedAssessment(
            assessment
        )

        setSuccessMessage(null)
        setReviewError(null)
    }

    function handleStartReview(
        decision: FraudReviewDecision,
        assessment: FraudAssessment,
    ): void {
        setSelectedAssessment(
            assessment
        )

        setReviewDecision(
            decision
        )

        setReviewError(null)
        setSuccessMessage(null)
    }

    function handleCloseReview(): void {
        if (isReviewSubmitting) {
            return
        }

        setReviewDecision(null)
        setReviewError(null)
    }

    async function handleRefreshAssessment(
        assessmentReference: string,
    ): Promise<void> {
        setIsAssessmentRefreshing(true)
        setQueueError(null)

        try {
            const refreshedAssessment =
                await getFraudAssessment(
                    assessmentReference
                )

            updateAssessmentInState(
                refreshedAssessment
            )
        } catch (error) {
            setQueueError(
                getFraudApiErrorMessage(
                    error,
                    'Unable to refresh the '
                    + 'selected fraud '
                    + 'assessment.'
                )
            )
        } finally {
            setIsAssessmentRefreshing(
                false
            )
        }
    }

    async function handleSubmitReview(
        assessment: FraudAssessment,
        decision: FraudReviewDecision,
        reviewNotes: string,
    ): Promise<void> {
        setIsReviewSubmitting(true)
        setReviewError(null)
        setSuccessMessage(null)

        try {
            const request = {
                reviewNotes,
                expectedVersion:
                assessment.version,
            }

            const updatedAssessment =
                await submitReviewDecision(
                    assessment
                        .assessmentReference,
                    decision,
                    request
                )

            updateAssessmentInState(
                updatedAssessment
            )

            setReviewDecision(null)

            setSuccessMessage(
                getReviewSuccessMessage(
                    decision,
                    updatedAssessment
                )
            )

            await Promise.all([
                loadDashboard(),
                loadAssessmentQueue(),
            ])
        } catch (error) {
            if (
                isFraudAssessmentVersionConflict(
                    error
                )
            ) {
                setReviewError(
                    'This assessment was updated '
                    + 'by another request. The '
                    + 'latest assessment will be '
                    + 'loaded before another '
                    + 'decision can be submitted.'
                )

                await refreshAfterConflict(
                    assessment
                        .assessmentReference
                )

                return
            }

            if (
                isFraudAssessmentStateConflict(
                    error
                )
            ) {
                setReviewError(
                    getFraudApiErrorMessage(
                        error,
                        'The assessment no longer '
                        + 'allows this review '
                        + 'decision.'
                    )
                )

                await refreshAfterConflict(
                    assessment
                        .assessmentReference
                )

                return
            }

            setReviewError(
                getFraudApiErrorMessage(
                    error,
                    'Unable to submit the fraud '
                    + 'assessment review.'
                )
            )
        } finally {
            setIsReviewSubmitting(false)
        }
    }

    async function refreshAfterConflict(
        assessmentReference: string,
    ): Promise<void> {
        try {
            const refreshedAssessment =
                await getFraudAssessment(
                    assessmentReference
                )

            updateAssessmentInState(
                refreshedAssessment
            )

            await Promise.all([
                loadDashboard(),
                loadAssessmentQueue(),
            ])
        } catch (refreshError) {
            setReviewError(
                getFraudApiErrorMessage(
                    refreshError,
                    'The assessment changed, but '
                    + 'the latest version could '
                    + 'not be loaded. Close the '
                    + 'dialog and refresh the '
                    + 'queue.'
                )
            )
        }
    }

    function updateAssessmentInState(
        updatedAssessment:
        FraudAssessment,
    ): void {
        setSelectedAssessment(
            updatedAssessment
        )

        setAssessmentPage(
            (currentPage) => {
                if (currentPage === null) {
                    return currentPage
                }

                return {
                    ...currentPage,
                    content:
                        currentPage.content.map(
                            (assessment) =>
                                assessment
                                    .assessmentReference
                                === updatedAssessment
                                    .assessmentReference
                                    ? updatedAssessment
                                    : assessment
                        ),
                }
            }
        )
    }

    const currentAssessments =
        assessmentPage?.content ?? []

    const pageDetails =
        assessmentPage?.page ?? null

    return (
        <ProcurementLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Explainable integrity controls
                    </span>

                    <h1>
                        Fraud assessment dashboard
                    </h1>

                    <p>
                        Review model-backed invoice
                        assessments, inspect immutable
                        evidence references, and record
                        accountable analyst decisions.
                    </p>
                </div>

                <button
                    className="secondary-button"
                    type="button"
                    onClick={
                        () => {
                            setSuccessMessage(null)

                            void Promise.all([
                                loadDashboard(),
                                loadAssessmentQueue(),
                            ])
                        }
                    }
                    disabled={
                        isDashboardLoading
                        || isQueueLoading
                    }
                >
                    {isDashboardLoading
                    || isQueueLoading
                        ? 'Refreshing...'
                        : 'Refresh dashboard'}
                </button>
            </section>

            {dashboardError !== null && (
                <p
                    className={
                        'form-error-message '
                        + 'page-message'
                    }
                    role="alert"
                >
                    {dashboardError}
                </p>
            )}

            {successMessage !== null && (
                <p
                    className={
                        'success-message '
                        + 'fraud-success-toast'
                    }
                    role="status"
                    aria-live="polite"
                >
                    {successMessage}
                </p>
            )}

            <section
                className="fraud-metric-grid"
                aria-label={
                    'Fraud assessment summary'
                }
            >
                <FraudMetricCard
                    label="Total assessments"
                    value={
                        dashboard
                            ?.totalAssessments
                        ?? 0
                    }
                    description={
                        'Persisted invoice '
                        + 'assessments across all '
                        + 'workflow states.'
                    }
                    isLoading={
                        isDashboardLoading
                    }
                    tone="info"
                    footer={
                        dashboard
                            ?.latestAssessedAt
                            ? (
                                <span>
                                Latest assessment:
                                    {' '}
                                    {
                                        formatDateTime(
                                            dashboard
                                                .latestAssessedAt
                                        )
                                    }
                            </span>
                            )
                            : undefined
                    }
                />

                <FraudMetricCard
                    label="Pending review"
                    value={
                        dashboard
                            ?.pendingReview
                        ?? 0
                    }
                    description={
                        'Assessments waiting for '
                        + 'an accountable analyst '
                        + 'decision.'
                    }
                    isLoading={
                        isDashboardLoading
                    }
                    tone={
                        dashboard
                        && dashboard
                            .pendingReview > 0
                            ? 'warning'
                            : 'positive'
                    }
                    footer={
                        getPendingAgeDescription(
                            dashboard
                        )
                    }
                />

                <FraudMetricCard
                    label="Open high-risk work"
                    value={
                        dashboard
                            ?.openHighOrCriticalRisk
                        ?? 0
                    }
                    description={
                        'High or critical cases '
                        + 'that remain pending or '
                        + 'escalated.'
                    }
                    isLoading={
                        isDashboardLoading
                    }
                    tone={
                        dashboard
                        && dashboard
                            .openHighOrCriticalRisk
                        > 0
                            ? 'danger'
                            : 'positive'
                    }
                />

                <FraudMetricCard
                    label="Confirmed fraud"
                    value={
                        dashboard
                            ?.confirmedFraud
                        ?? 0
                    }
                    description={
                        'Assessments confirmed as '
                        + 'fraud after authorised '
                        + 'review.'
                    }
                    isLoading={
                        isDashboardLoading
                    }
                    tone={
                        dashboard
                        && dashboard
                            .confirmedFraud > 0
                            ? 'danger'
                            : 'default'
                    }
                />

                <FraudMetricCard
                    label="Escalated"
                    value={
                        dashboard?.escalated
                        ?? 0
                    }
                    description={
                        'Cases awaiting senior '
                        + 'investigation or further '
                        + 'evidence.'
                    }
                    isLoading={
                        isDashboardLoading
                    }
                    tone={
                        dashboard
                        && dashboard
                            .escalated > 0
                            ? 'warning'
                            : 'default'
                    }
                />

                <FraudMetricCard
                    label="False positives"
                    value={
                        dashboard
                            ?.falsePositive
                        ?? 0
                    }
                    description={
                        'Flagged assessments '
                        + 'cleared through '
                        + 'documented review.'
                    }
                    isLoading={
                        isDashboardLoading
                    }
                    tone="info"
                />

                <FraudMetricCard
                    label="Critical risk"
                    value={
                        dashboard
                            ?.criticalRisk
                        ?? 0
                    }
                    description={
                        'Assessments classified at '
                        + 'the highest operational '
                        + 'risk level.'
                    }
                    isLoading={
                        isDashboardLoading
                    }
                    tone={
                        dashboard
                        && dashboard
                            .criticalRisk > 0
                            ? 'danger'
                            : 'positive'
                    }
                />

                <FraudMetricCard
                    label="High risk"
                    value={
                        dashboard?.highRisk
                        ?? 0
                    }
                    description={
                        'Assessments classified as '
                        + 'high risk by the active '
                        + 'model.'
                    }
                    isLoading={
                        isDashboardLoading
                    }
                    tone={
                        dashboard
                        && dashboard.highRisk > 0
                            ? 'warning'
                            : 'default'
                    }
                />
            </section>

            <FraudAssessmentFilters
                filters={filters}
                sort={sort}
                isLoading={isQueueLoading}
                onApply={
                    handleApplyFilters
                }
                onReset={
                    handleResetFilters
                }
            />

            {queueError !== null && (
                <p
                    className={
                        'form-error-message '
                        + 'page-message'
                    }
                    role="alert"
                >
                    {queueError}
                </p>
            )}

            <div className="fraud-workspace-grid">
                <FraudAssessmentTable
                    assessments={
                        currentAssessments
                    }
                    page={pageDetails}
                    selectedReference={
                        selectedAssessment
                            ?.assessmentReference
                        ?? null
                    }
                    isLoading={
                        isQueueLoading
                    }
                    onSelect={
                        handleSelectAssessment
                    }
                    onPageChange={
                        handlePageChange
                    }
                />

                <FraudAssessmentDetailsPanel
                    assessment={
                        selectedAssessment
                    }
                    isPlatformAdministrator={
                        isPlatformAdministrator
                    }
                    isRefreshing={
                        isAssessmentRefreshing
                    }
                    onReview={
                        handleStartReview
                    }
                    onRefresh={
                        (assessmentReference) => {
                            void handleRefreshAssessment(
                                assessmentReference
                            )
                        }
                    }
                    onClose={
                        () => {
                            setSelectedAssessment(
                                null
                            )
                        }
                    }
                />
            </div>

            <FraudAssessmentReviewDialog
                assessment={
                    selectedAssessment
                }
                decision={
                    reviewDecision
                }
                isSubmitting={
                    isReviewSubmitting
                }
                errorMessage={
                    reviewError
                }
                onSubmit={
                    handleSubmitReview
                }
                onClose={
                    handleCloseReview
                }
            />
        </ProcurementLayout>
    )
}

function buildAssessmentQuery(
    filters: FraudAssessmentFilterState,
    sort: FraudAssessmentSort | '',
    pageNumber: number,
): FraudAssessmentQuery {
    return {
        reviewStatus:
            filters.reviewStatus
            || undefined,

        riskLevel:
            filters.riskLevel
            || undefined,

        classification:
            filters.classification
            || undefined,

        supplierId:
            filters.supplierId
            || undefined,

        departmentId:
            filters.departmentId
            || undefined,

        modelVersion:
            filters.modelVersion
            || undefined,

        assessedFrom:
            filters.assessedFrom
            || undefined,

        assessedTo:
            filters.assessedTo
            || undefined,

        page:
        pageNumber,

        size:
        pageSize,

        sort:
            sort || undefined,
    }
}

function reconcileSelection(
    selectedAssessment:
        FraudAssessment | null,
    assessments: FraudAssessment[],
): FraudAssessment | null {
    if (selectedAssessment === null) {
        return null
    }

    return assessments.find(
        (assessment) =>
            assessment.assessmentReference
            === selectedAssessment
                .assessmentReference
    ) ?? null
}

async function submitReviewDecision(
    assessmentReference: string,
    decision: FraudReviewDecision,
    request: {
        reviewNotes: string
        expectedVersion: number
    },
): Promise<FraudAssessment> {
    switch (decision) {
        case 'CONFIRM_FRAUD':
            return confirmFraudAssessment(
                assessmentReference,
                request
            )

        case 'FALSE_POSITIVE':
            return markFraudAssessmentFalsePositive(
                assessmentReference,
                request
            )

        case 'ESCALATE':
            return escalateFraudAssessment(
                assessmentReference,
                request
            )
    }
}

function getReviewSuccessMessage(
    decision: FraudReviewDecision,
    assessment: FraudAssessment,
): string {
    switch (decision) {
        case 'CONFIRM_FRAUD':
            return (
                'Fraud was confirmed for '
                + assessment
                    .assessmentReference
                + '.'
            )

        case 'FALSE_POSITIVE':
            return (
                assessment
                    .assessmentReference
                + ' was marked as a '
                + 'false positive.'
            )

        case 'ESCALATE':
            return (
                assessment
                    .assessmentReference
                + ' was escalated for '
                + 'senior investigation.'
            )
    }
}

function getPendingAgeDescription(
    dashboard:
        FraudAssessmentDashboard | null,
): string | undefined {
    if (
        dashboard === null
        || dashboard
            .oldestPendingAgeHours
        === null
    ) {
        return undefined
    }

    const age =
        dashboard.oldestPendingAgeHours

    return (
        'Oldest pending assessment: '
        + age
        + ' '
        + (
            age === 1
                ? 'hour'
                : 'hours'
        )
        + '.'
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

    return new Intl.DateTimeFormat(
        'en-ZA',
        {
            dateStyle: 'medium',
            timeStyle: 'short',
        }
    ).format(parsedDate)
}