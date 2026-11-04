import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from 'react'

import type {
    ChangeEvent,
} from 'react'

import {
    Link,
    useNavigate,
} from 'react-router-dom'

import {
    closeProcurementTenderBidding,
    getProcurementTenderApiErrorMessage,
    getProcurementTenders,
    publishProcurementTender,
    startProcurementTenderEvaluation,
    verifyProcurementTenderSpecification,
} from '../api/procurementTenderApi'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import {
    SpecificationTenderCard,
} from '../components/procurement/SpecificationTenderCard'

import type {
    ProcurementTender,
    ProcurementTenderSort,
    ProcurementTenderStatus,
    SpecificationIntegrity,
} from '../types/procurementTender'

import {
    calculateTenderStatistics,
    PROCUREMENT_TENDER_SORT_OPTIONS,
    PROCUREMENT_TENDER_STATUS_LABELS,
} from '../types/procurementTender'

import './SpecificationTenderPage.css'

const backendPageSize = 100
const visiblePageSize = 10

type TenderWorkflowAction =
    | 'PUBLISH'
    | 'VERIFY_INTEGRITY'
    | 'CLOSE_BIDDING'
    | 'START_EVALUATION'

interface PendingTenderAction {
    tenderId: number
    action: TenderWorkflowAction
}

interface TenderStatusOption {
    value: ProcurementTenderStatus
    label: string
}

const tenderStatusOptions:
    TenderStatusOption[] = [
    {
        value: 'DRAFT',
        label:
        PROCUREMENT_TENDER_STATUS_LABELS
            .DRAFT,
    },
    {
        value: 'PUBLISHED',
        label:
        PROCUREMENT_TENDER_STATUS_LABELS
            .PUBLISHED,
    },
    {
        value: 'BIDDING_CLOSED',
        label:
        PROCUREMENT_TENDER_STATUS_LABELS
            .BIDDING_CLOSED,
    },
    {
        value: 'EVALUATION',
        label:
        PROCUREMENT_TENDER_STATUS_LABELS
            .EVALUATION,
    },
    {
        value: 'ADJUDICATION',
        label:
        PROCUREMENT_TENDER_STATUS_LABELS
            .ADJUDICATION,
    },
    {
        value: 'AWARDED',
        label:
        PROCUREMENT_TENDER_STATUS_LABELS
            .AWARDED,
    },
    {
        value: 'CANCELLED',
        label:
        PROCUREMENT_TENDER_STATUS_LABELS
            .CANCELLED,
    },
]

export function SpecificationTenderQueuePage() {
    const navigate =
        useNavigate()

    const [
        tenders,
        setTenders,
    ] =
        useState<ProcurementTender[]>(
            []
        )

    const [
        searchTerm,
        setSearchTerm,
    ] = useState('')

    const [
        statusFilter,
        setStatusFilter,
    ] =
        useState<
            ProcurementTenderStatus | ''
        >('')

    const [
        sort,
        setSort,
    ] =
        useState<ProcurementTenderSort>(
            'createdAt,desc'
        )

    const [
        pageNumber,
        setPageNumber,
    ] = useState(0)

    const [
        isLoading,
        setIsLoading,
    ] = useState(true)

    const [
        pendingAction,
        setPendingAction,
    ] =
        useState<
            PendingTenderAction | null
        >(null)

    const [
        errorMessage,
        setErrorMessage,
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

    const [
        integrityResult,
        setIntegrityResult,
    ] =
        useState<
            SpecificationIntegrity | null
        >(null)

    const loadTenders =
        useCallback(
            async (): Promise<void> => {
                setIsLoading(true)
                setErrorMessage(null)

                try {
                    const response =
                        await getProcurementTenders({
                            page: 0,
                            size:
                            backendPageSize,
                            sort,
                        })

                    setTenders(
                        response.content
                    )
                } catch (error) {
                    setErrorMessage(
                        getProcurementTenderApiErrorMessage(
                            error,
                            'Unable to load the '
                            + 'specification '
                            + 'tender queue.'
                        )
                    )
                } finally {
                    setIsLoading(false)
                }
            },
            [
                sort,
            ]
        )

    useEffect(
        () => {
            queueMicrotask(
                () => {
                    void loadTenders()
                }
            )
        },
        [
            loadTenders,
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
                        setSuccessMessage(
                            null
                        )
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

    const statistics =
        useMemo(
            () =>
                calculateTenderStatistics(
                    tenders
                ),
            [
                tenders,
            ]
        )

    const filteredTenders =
        useMemo(
            () => {
                const normalizedSearchTerm =
                    searchTerm
                        .trim()
                        .toLowerCase()

                return tenders.filter(
                    (tender) => {
                        if (
                            statusFilter !== ''
                            && tender.status
                            !== statusFilter
                        ) {
                            return false
                        }

                        if (
                            normalizedSearchTerm
                                .length === 0
                        ) {
                            return true
                        }

                        const searchableValues = [
                            tender.tenderReference,
                            tender.title,
                            tender.description,
                            tender.buyerOrgId,
                            tender.requiredBusinessType,
                            tender.serviceArea,
                        ]

                        return searchableValues.some(
                            (value) =>
                                value
                                    .toLowerCase()
                                    .includes(
                                        normalizedSearchTerm
                                    )
                        )
                    }
                )
            },
            [
                tenders,
                searchTerm,
                statusFilter,
            ]
        )

    const totalPages =
        filteredTenders.length === 0
            ? 0
            : Math.ceil(
                filteredTenders.length
                / visiblePageSize
            )

    const currentPageNumber =
        totalPages === 0
            ? 0
            : Math.min(
                pageNumber,
                totalPages - 1
            )

    const visibleTenders =
        useMemo(
            () => {
                const firstTenderIndex =
                    currentPageNumber
                    * visiblePageSize

                return filteredTenders.slice(
                    firstTenderIndex,
                    firstTenderIndex
                    + visiblePageSize
                )
            },
            [
                filteredTenders,
                currentPageNumber,
            ]
        )

    function handleSearchChange(
        event:
        ChangeEvent<HTMLInputElement>,
    ): void {
        setSearchTerm(
            event.target.value
        )

        setPageNumber(0)
        setIntegrityResult(null)
    }

    function handleStatusChange(
        event:
        ChangeEvent<HTMLSelectElement>,
    ): void {
        setStatusFilter(
            parseTenderStatus(
                event.target.value
            )
        )

        setPageNumber(0)
        setIntegrityResult(null)
    }

    function handleSortChange(
        event:
        ChangeEvent<HTMLSelectElement>,
    ): void {
        setSort(
            parseTenderSort(
                event.target.value
            )
        )

        setPageNumber(0)
        setIntegrityResult(null)
    }

    function handleResetFilters(): void {
        setSearchTerm('')
        setStatusFilter('')
        setSort('createdAt,desc')
        setPageNumber(0)
        setIntegrityResult(null)
    }

    function handlePreviousPage(): void {
        if (currentPageNumber === 0) {
            return
        }

        setPageNumber(
            currentPageNumber - 1
        )
    }

    function handleNextPage(): void {
        if (
            totalPages === 0
            || currentPageNumber + 1
            >= totalPages
        ) {
            return
        }

        setPageNumber(
            currentPageNumber + 1
        )
    }

    async function handlePublish(
        tender: ProcurementTender,
    ): Promise<void> {
        const confirmed =
            window.confirm(
                'Publish '
                + tender.tenderReference
                + '? The specification will '
                + 'be locked with a SHA-256 '
                + 'hash.'
            )

        if (!confirmed) {
            return
        }

        await executeTenderAction(
            tender,
            'PUBLISH',
            () =>
                publishProcurementTender(
                    tender.id
                ),
            'Tender published successfully.'
        )
    }

    async function handleCloseBidding(
        tender: ProcurementTender,
    ): Promise<void> {
        const confirmed =
            window.confirm(
                'Close bidding for '
                + tender.tenderReference
                + '? Traders will no longer '
                + 'be able to submit new bids.'
            )

        if (!confirmed) {
            return
        }

        await executeTenderAction(
            tender,
            'CLOSE_BIDDING',
            () =>
                closeProcurementTenderBidding(
                    tender.id
                ),
            'Tender bidding was closed.'
        )
    }

    async function handleStartEvaluation(
        tender: ProcurementTender,
    ): Promise<void> {
        const confirmed =
            window.confirm(
                'Move '
                + tender.tenderReference
                + ' into evaluation? Only '
                + 'assigned evaluators should '
                + 'score its bids.'
            )

        if (!confirmed) {
            return
        }

        await executeTenderAction(
            tender,
            'START_EVALUATION',
            () =>
                startProcurementTenderEvaluation(
                    tender.id
                ),
            'Tender moved to evaluation.'
        )
    }

    async function handleVerifyIntegrity(
        tender: ProcurementTender,
    ): Promise<void> {
        setPendingAction({
            tenderId: tender.id,
            action:
                'VERIFY_INTEGRITY',
        })

        setErrorMessage(null)
        setSuccessMessage(null)
        setIntegrityResult(null)

        try {
            const response =
                await verifyProcurementTenderSpecification(
                    tender.id
                )

            setIntegrityResult(
                response
            )

            if (response.integrityValid) {
                setSuccessMessage(
                    response.message
                )
            } else {
                setErrorMessage(
                    response.message
                )
            }
        } catch (error) {
            setErrorMessage(
                getProcurementTenderApiErrorMessage(
                    error,
                    'Unable to verify the '
                    + 'published '
                    + 'specification.'
                )
            )
        } finally {
            setPendingAction(null)
        }
    }

    async function executeTenderAction(
        tender: ProcurementTender,
        action: TenderWorkflowAction,
        actionRequest:
        () => Promise<ProcurementTender>,
        successText: string,
    ): Promise<void> {
        setPendingAction({
            tenderId: tender.id,
            action,
        })

        setErrorMessage(null)
        setSuccessMessage(null)
        setIntegrityResult(null)

        try {
            const updatedTender =
                await actionRequest()

            updateTenderInState(
                updatedTender
            )

            setSuccessMessage(
                successText
                + ' '
                + updatedTender
                    .tenderReference
            )
        } catch (error) {
            setErrorMessage(
                getProcurementTenderApiErrorMessage(
                    error,
                    'Unable to complete the '
                    + 'tender workflow action.'
                )
            )

            await loadTenders()
        } finally {
            setPendingAction(null)
        }
    }

    function updateTenderInState(
        updatedTender:
        ProcurementTender,
    ): void {
        setTenders(
            (currentTenders) =>
                currentTenders.map(
                    (tender) =>
                        tender.id
                        === updatedTender.id
                            ? updatedTender
                            : tender
                )
        )
    }

    function isTenderActionPending(
        tenderId: number,
    ): boolean {
        return pendingAction
            ?.tenderId === tenderId
    }

    return (
        <ProcurementLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Specification workflow
                    </span>

                    <h1>
                        Tender specifications
                    </h1>

                    <p>
                        Create, publish, lock, and
                        progress procurement
                        specifications through the
                        controlled tender lifecycle.
                    </p>
                </div>

                <Link
                    className="primary-link-button"
                    to={
                        '/procurement/'
                        + 'specifications/new'
                    }
                >
                    Create specification
                </Link>
            </section>

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

            {successMessage !== null && (
                <p
                    className={
                        'success-message '
                        + 'page-message'
                    }
                    role="status"
                    aria-live="polite"
                >
                    {successMessage}
                </p>
            )}

            {integrityResult !== null && (
                <SpecificationIntegrityNotice
                    result={
                        integrityResult
                    }
                />
            )}

            <section
                className={
                    'statistics-grid '
                    + 'specification-statistics-grid'
                }
                aria-label={
                    'Tender specification statistics'
                }
            >
                <StatisticCard
                    label="Total tenders"
                    value={statistics.total}
                    description={
                        'All internal procurement '
                        + 'tenders.'
                    }
                />

                <StatisticCard
                    label="Draft specifications"
                    value={statistics.draft}
                    description={
                        'Specifications awaiting '
                        + 'publication.'
                    }
                />

                <StatisticCard
                    label="Published"
                    value={
                        statistics.published
                    }
                    description={
                        'Tenders accepting trader '
                        + 'submissions.'
                    }
                />

                <StatisticCard
                    label="Bidding closed"
                    value={
                        statistics.biddingClosed
                    }
                    description={
                        'Tenders ready to enter '
                        + 'evaluation.'
                    }
                />

                <StatisticCard
                    label="Evaluation"
                    value={
                        statistics.evaluation
                    }
                    description={
                        'Tenders undergoing blind '
                        + 'bid evaluation.'
                    }
                />

                <StatisticCard
                    label="Adjudication"
                    value={
                        statistics.adjudication
                    }
                    description={
                        'Recommendations awaiting '
                        + 'an adjudication decision.'
                    }
                />

                <StatisticCard
                    label="Awarded"
                    value={
                        statistics.awarded
                    }
                    description={
                        'Completed tender awards.'
                    }
                />

                <StatisticCard
                    label="Cancelled"
                    value={
                        statistics.cancelled
                    }
                    description={
                        'Tenders removed from the '
                        + 'active workflow.'
                    }
                />
            </section>

            <section
                className={
                    'specification-queue-toolbar'
                }
                aria-labelledby={
                    'specification-filter-heading'
                }
            >
                <div>
                    <span className="eyebrow">
                        Queue controls
                    </span>

                    <h2
                        id={
                            'specification-filter-heading'
                        }
                    >
                        Tender queue
                    </h2>
                </div>

                <div
                    className={
                        'specification-filter-grid'
                    }
                >
                    <div className="form-field">
                        <label
                            htmlFor={
                                'specification-search'
                            }
                        >
                            Search tenders
                        </label>

                        <input
                            id="specification-search"
                            type="search"
                            value={searchTerm}
                            onChange={
                                handleSearchChange
                            }
                            placeholder={
                                'Reference, title, '
                                + 'organisation, or '
                                + 'service area'
                            }
                            disabled={isLoading}
                        />
                    </div>

                    <div className="form-field">
                        <label
                            htmlFor={
                                'specification-status'
                            }
                        >
                            Workflow status
                        </label>

                        <select
                            id="specification-status"
                            value={statusFilter}
                            onChange={
                                handleStatusChange
                            }
                            disabled={isLoading}
                        >
                            <option value="">
                                All statuses
                            </option>

                            {
                                tenderStatusOptions
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
                                'specification-sort'
                            }
                        >
                            Sort tenders
                        </label>

                        <select
                            id="specification-sort"
                            value={sort}
                            onChange={
                                handleSortChange
                            }
                            disabled={isLoading}
                        >
                            {
                                PROCUREMENT_TENDER_SORT_OPTIONS
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

                    <button
                        className="secondary-button"
                        type="button"
                        onClick={
                            handleResetFilters
                        }
                        disabled={isLoading}
                    >
                        Reset filters
                    </button>
                </div>

                <div
                    className={
                        'specification-result-summary'
                    }
                >
                    <span>
                        {filteredTenders.length}
                        {' '}
                        {
                            filteredTenders.length
                            === 1
                                ? 'tender'
                                : 'tenders'
                        }
                    </span>

                    <button
                        className="text-button"
                        type="button"
                        onClick={
                            () => {
                                void loadTenders()
                            }
                        }
                        disabled={isLoading}
                    >
                        {isLoading
                            ? 'Refreshing...'
                            : 'Refresh queue'}
                    </button>
                </div>
            </section>

            {isLoading
            && tenders.length === 0 ? (
                <section
                    className={
                        'content-card '
                        + 'specification-loading-card'
                    }
                    aria-busy="true"
                >
                    <p>
                        Loading tender
                        specifications...
                    </p>
                </section>
            ) : visibleTenders.length === 0 ? (
                <section className="content-card">
                    <div className="empty-state">
                        <h3>
                            No tenders found
                        </h3>

                        <p>
                            No internal tenders match
                            the selected search and
                            workflow filters.
                        </p>
                    </div>
                </section>
            ) : (
                <section
                    className={
                        'specification-tender-list'
                    }
                    aria-label={
                        'Specification tender queue'
                    }
                >
                    {visibleTenders.map(
                        (tender) => (
                            <SpecificationTenderCard
                                key={tender.id}
                                tender={tender}
                                isActionPending={
                                    isTenderActionPending(
                                        tender.id
                                    )
                                }
                                onView={
                                    (
                                        selectedTender
                                    ) => {
                                        navigate(
                                            '/procurement/'
                                            + 'specifications/'
                                            + selectedTender.id
                                        )
                                    }
                                }
                                onPublish={
                                    (
                                        selectedTender
                                    ) => {
                                        void handlePublish(
                                            selectedTender
                                        )
                                    }
                                }
                                onVerifyIntegrity={
                                    (
                                        selectedTender
                                    ) => {
                                        void handleVerifyIntegrity(
                                            selectedTender
                                        )
                                    }
                                }
                                onCloseBidding={
                                    (
                                        selectedTender
                                    ) => {
                                        void handleCloseBidding(
                                            selectedTender
                                        )
                                    }
                                }
                                onStartEvaluation={
                                    (
                                        selectedTender
                                    ) => {
                                        void handleStartEvaluation(
                                            selectedTender
                                        )
                                    }
                                }
                            />
                        )
                    )}
                </section>
            )}

            <div
                className={
                    'pagination-controls '
                    + 'specification-pagination'
                }
            >
                <button
                    className="secondary-button"
                    type="button"
                    onClick={
                        handlePreviousPage
                    }
                    disabled={
                        isLoading
                        || currentPageNumber === 0
                    }
                >
                    Previous
                </button>

                <span>
                    Page
                    {' '}
                    <strong>
                        {totalPages === 0
                            ? 0
                            : currentPageNumber + 1}
                    </strong>
                    {' '}
                    of
                    {' '}
                    <strong>
                        {totalPages}
                    </strong>
                </span>

                <button
                    className="secondary-button"
                    type="button"
                    onClick={
                        handleNextPage
                    }
                    disabled={
                        isLoading
                        || totalPages === 0
                        || currentPageNumber + 1
                        >= totalPages
                    }
                >
                    Next
                </button>
            </div>
        </ProcurementLayout>
    )
}

interface StatisticCardProps {
    label: string
    value: number
    description: string
}

function StatisticCard({
                           label,
                           value,
                           description,
                       }: StatisticCardProps) {
    return (
        <article className="statistic-card">
            <span>{label}</span>

            <strong>{value}</strong>

            <p>{description}</p>
        </article>
    )
}

interface SpecificationIntegrityNoticeProps {
    result: SpecificationIntegrity
}

function SpecificationIntegrityNotice({
                                          result,
                                      }: SpecificationIntegrityNoticeProps) {
    const noticeClassName =
        result.integrityValid
            ? (
                'specification-integrity-notice '
                + 'is-valid'
            )
            : (
                'specification-integrity-notice '
                + 'is-invalid'
            )

    return (
        <section
            className={noticeClassName}
            aria-live="polite"
        >
            <div>
                <span className="eyebrow">
                    Specification integrity
                </span>

                <h2>
                    {result.integrityValid
                        ? 'Integrity verified'
                        : 'Integrity check failed'}
                </h2>

                <p>{result.message}</p>
            </div>

            <dl>
                <div>
                    <dt>Tender</dt>

                    <dd>
                        {
                            result
                                .tenderReference
                        }
                    </dd>
                </div>

                <div>
                    <dt>Stored hash</dt>

                    <dd>
                        <code>
                            {result.storedHash}
                        </code>
                    </dd>
                </div>

                <div>
                    <dt>Current hash</dt>

                    <dd>
                        <code>
                            {result.currentHash}
                        </code>
                    </dd>
                </div>
            </dl>
        </section>
    )
}

function parseTenderStatus(
    value: string,
): ProcurementTenderStatus | '' {
    switch (value) {
        case 'DRAFT':
            return 'DRAFT'

        case 'PUBLISHED':
            return 'PUBLISHED'

        case 'BIDDING_CLOSED':
            return 'BIDDING_CLOSED'

        case 'EVALUATION':
            return 'EVALUATION'

        case 'ADJUDICATION':
            return 'ADJUDICATION'

        case 'AWARDED':
            return 'AWARDED'

        case 'CANCELLED':
            return 'CANCELLED'

        default:
            return ''
    }
}

function parseTenderSort(
    value: string,
): ProcurementTenderSort {
    switch (value) {
        case 'createdAt,desc':
            return 'createdAt,desc'

        case 'createdAt,asc':
            return 'createdAt,asc'

        case 'updatedAt,desc':
            return 'updatedAt,desc'

        case 'updatedAt,asc':
            return 'updatedAt,asc'

        case 'publishedAt,desc':
            return 'publishedAt,desc'

        case 'publishedAt,asc':
            return 'publishedAt,asc'

        case 'tenderReference,asc':
            return 'tenderReference,asc'

        case 'tenderReference,desc':
            return 'tenderReference,desc'

        case 'title,asc':
            return 'title,asc'

        case 'title,desc':
            return 'title,desc'

        case 'budgetAmount,desc':
            return 'budgetAmount,desc'

        case 'budgetAmount,asc':
            return 'budgetAmount,asc'

        case 'status,asc':
            return 'status,asc'

        case 'status,desc':
            return 'status,desc'

        default:
            return 'createdAt,desc'
    }
}