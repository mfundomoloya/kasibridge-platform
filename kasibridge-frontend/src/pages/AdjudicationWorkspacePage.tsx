import {
    type ChangeEvent,
    type FormEvent,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from 'react'

import {
    Link,
    useParams,
} from 'react-router-dom'

import {
    awardTender,
    getAdjudicationApiErrorMessage,
    getAdjudicationSummary,
} from '../api/adjudicationApi'

import {
    getCommitteeAssignmentApiErrorMessage,
    getMyAdjudicatorAssignmentQueue,
} from '../api/committeeAssignmentApi'

import {
    getProcurementTender,
    getProcurementTenderApiErrorMessage,
} from '../api/procurementTenderApi'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import {
    AdjudicationRankingCard,
} from '../components/procurement/AdjudicationRankingCard'

import {
    ProcurementTenderStatusBadge,
} from '../components/procurement/ProcurementTenderStatusBadge'

import type {
    AdjudicationAwardFormErrors,
    AdjudicationAwardFormState,
    AwardTenderResponse,
    BidRanking,
} from '../types/adjudication'

import {
    buildAwardTenderRequest,
    EMPTY_ADJUDICATION_AWARD_FORM,
    findRankingByBidId,
    getHighestRankedBid,
    validateAdjudicationAwardForm,
} from '../types/adjudication'

import type {
    CurrentUserCommitteeAssignment,
} from '../types/committeeAssignment'

import type {
    ProcurementTender,
} from '../types/procurementTender'

import './SpecificationTenderPage.css'

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

export function AdjudicationWorkspacePage() {
    const routeParameters =
        useParams()

    const tenderId =
        parseTenderId(
            routeParameters.tenderId
        )

    const [
        tender,
        setTender,
    ] =
        useState<ProcurementTender | null>(
            null
        )

    const awardConfirmationDialogRef =
        useRef<HTMLDialogElement>(null)

    const [
        isAwardConfirmationOpen,
        setIsAwardConfirmationOpen,
    ] = useState(false)

    const [
        pendingAwardBid,
        setPendingAwardBid,
    ] = useState<BidRanking | null>(null)

    const [
        assignment,
        setAssignment,
    ] =
        useState<
            CurrentUserCommitteeAssignment
            | null
        >(null)

    const [
        rankings,
        setRankings,
    ] =
        useState<BidRanking[]>(
            []
        )

    const [
        form,
        setForm,
    ] =
        useState<
            AdjudicationAwardFormState
        >(
            EMPTY_ADJUDICATION_AWARD_FORM
        )

    const [
        formErrors,
        setFormErrors,
    ] =
        useState<
            AdjudicationAwardFormErrors
        >({})

    const [
        awardResult,
        setAwardResult,
    ] =
        useState<
            AwardTenderResponse | null
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
        isAwarding,
        setIsAwarding,
    ] = useState(false)

    const [
        pageError,
        setPageError,
    ] =
        useState<string | null>(
            tenderId === null
                ? 'A valid tender ID is required.'
                : null
        )

    const [
        successMessage,
        setSuccessMessage,
    ] =
        useState<string | null>(
            null
        )

    const loadWorkspace =
        useCallback(
            async (
                manualRefresh:
                boolean = false,
            ): Promise<void> => {
                if (tenderId === null) {
                    setIsLoading(false)
                    setIsRefreshing(false)

                    setPageError(
                        'A valid tender ID '
                        + 'is required.'
                    )

                    return
                }

                if (manualRefresh) {
                    setIsRefreshing(true)
                } else {
                    setIsLoading(true)
                }

                setPageError(null)

                try {
                    const [
                        tenderResponse,
                        assignmentResponse,
                    ] =
                        await Promise.all([
                            getProcurementTender(
                                tenderId
                            ),

                            getMyAdjudicatorAssignmentQueue(),
                        ])

                    const matchingAssignment =
                        findAdjudicatorAssignment(
                            assignmentResponse,
                            tenderId
                        )

                    setTender(
                        tenderResponse
                    )

                    setAssignment(
                        matchingAssignment
                    )

                    if (
                        matchingAssignment
                        === null
                    ) {
                        setRankings([])

                        setPageError(
                            'The authenticated user '
                            + 'does not have an '
                            + 'active Adjudicator '
                            + 'assignment for this '
                            + 'tender.'
                        )

                        return
                    }

                    if (
                        tenderResponse.status
                        !== 'ADJUDICATION'
                        && tenderResponse.status
                        !== 'AWARDED'
                    ) {
                        setRankings([])

                        setPageError(
                            'The adjudication summary '
                            + 'is available only '
                            + 'when the tender is '
                            + 'in Adjudication or '
                            + 'Awarded status.'
                        )

                        return
                    }

                    const rankingResponse =
                        await getAdjudicationSummary(
                            tenderId
                        )

                    setRankings(
                        rankingResponse
                    )

                    if (
                        tenderResponse.status
                        === 'AWARDED'
                    ) {
                        return
                    }

                    setForm(
                        (currentForm) => {
                            const selectedBidId =
                                Number(
                                    currentForm
                                        .selectedBidId
                                )

                            const currentSelectionExists =
                                rankingResponse.some(
                                    (ranking) =>
                                        ranking.bidId
                                        === selectedBidId
                                )

                            if (
                                currentSelectionExists
                            ) {
                                return currentForm
                            }

                            const highestRankedBid =
                                getHighestRankedBid(
                                    rankingResponse
                                )

                            return {
                                ...currentForm,

                                selectedBidId:
                                    highestRankedBid
                                    === null
                                        ? ''
                                        : highestRankedBid
                                            .bidId
                                            .toString(),
                            }
                        }
                    )
                } catch (error) {
                    setPageError(
                        getWorkspaceErrorMessage(
                            error
                        )
                    )
                } finally {
                    setIsLoading(false)
                    setIsRefreshing(false)
                }
            },
            [
                tenderId,
            ]
        )

        useEffect(
            () => {
                const dialog =
                    awardConfirmationDialogRef.current

                if (dialog === null) {
                    return
                }

                if (
                    isAwardConfirmationOpen
                    && !dialog.open
                ) {
                    dialog.showModal()
                } else if (
                    !isAwardConfirmationOpen
                    && dialog.open
                ) {
                    dialog.close()
                }
            },
            [
                isAwardConfirmationOpen,
            ]
        )

    useEffect(
        () => {
            queueMicrotask(
                () => {
                    void loadWorkspace()
                }
            )
        },
        [
            loadWorkspace,
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
                    6000
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

    const selectedBid =
        useMemo(
            () => {
                const selectedBidId =
                    Number(
                        form.selectedBidId
                    )

                if (
                    !Number.isInteger(
                        selectedBidId
                    )
                    || selectedBidId <= 0
                ) {
                    return null
                }

                return findRankingByBidId(
                    rankings,
                    selectedBidId
                )
            },
            [
                form.selectedBidId,
                rankings,
            ]
        )

    const highestRankedBid =
        useMemo(
            () =>
                getHighestRankedBid(
                    rankings
                ),
            [
                rankings,
            ]
        )

    const isReadOnly =
        tender === null
        || assignment === null
        || tender.status !== 'ADJUDICATION'
        || awardResult !== null

    function handleBidSelect(
        bidId: number,
    ): void {
        if (
            isReadOnly
            || isAwarding
        ) {
            return
        }

        setForm(
            (currentForm) => ({
                ...currentForm,

                selectedBidId:
                    bidId.toString(),
            })
        )

        setFormErrors(
            (currentErrors) => ({
                ...currentErrors,

                selectedBidId:
                undefined,

                form:
                undefined,
            })
        )
    }

    function handleAwardReasonChange(
        event:
        ChangeEvent<HTMLTextAreaElement>,
    ): void {
        setForm(
            (currentForm) => ({
                ...currentForm,

                awardReason:
                event.target.value,
            })
        )

        setFormErrors(
            (currentErrors) => ({
                ...currentErrors,

                awardReason:
                undefined,

                form:
                undefined,
            })
        )
    }

    async function handleAwardSubmit(
        event:
        FormEvent<HTMLFormElement>,
    ): Promise<void> {
        event.preventDefault()

        if (
            tenderId === null
            || tender === null
            || isReadOnly
            || isAwarding
        ) {
            return
        }

        const validationErrors =
            validateAdjudicationAwardForm(
                form,
                rankings
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

        const selected =
            findRankingByBidId(
                rankings,
                Number(
                    form.selectedBidId
                )
            )

        if (selected === null) {
            setFormErrors({
                selectedBidId:
                    'Select a valid ranked bid.',
            })

            return
        }

        setPendingAwardBid(selected)
        setIsAwardConfirmationOpen(true)
    }

    async function confirmAward(): Promise<void> {
        if (
            tenderId === null
            || tender === null
            || pendingAwardBid === null
            || isReadOnly
            || isAwarding
        ) {
            return
        }

        setIsAwarding(true)
        setFormErrors({})
        setPageError(null)
        setSuccessMessage(null)

        try {
            const request =
                buildAwardTenderRequest(
                    form,
                    rankings
                )

            const response =
                await awardTender(
                    tenderId,
                    request
                )

            setIsAwardConfirmationOpen(false)
            setPendingAwardBid(null)

            setAwardResult(
                response
            )

            setTender(
                (currentTender) => {
                    if (
                        currentTender === null
                    ) {
                        return currentTender
                    }

                    return {
                        ...currentTender,

                        status:
                            'AWARDED',
                    }
                }
            )

            setSuccessMessage(
                'Tender '
                + response.tenderReference
                + ' was awarded to '
                + response
                    .winningBidderAlias
                + '.'
            )
        } catch (error) {
            setFormErrors({
                form:
                    getAdjudicationApiErrorMessage(
                        error,
                        'Unable to record the '
                        + 'tender award.'
                    ),
            })
        } finally {
            setIsAwarding(false)
        }
    }

    function cancelAwardConfirmation(): void {
        if (isAwarding) {
            return
        }

        setIsAwardConfirmationOpen(false)
        setPendingAwardBid(null)
    }

    return (
        <ProcurementLayout>
            <nav
                className="back-navigation"
                aria-label={
                    'Adjudication navigation'
                }
            >
                <Link
                    to={
                        '/procurement/'
                        + 'adjudications'
                    }
                >
                    Back to my adjudications
                </Link>
            </nav>

            {pageError !== null && (
                <p
                    className={
                        'form-error-message '
                        + 'page-message'
                    }
                    role="alert"
                >
                    {pageError}
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

            {isLoading && tender === null ? (
                <AdjudicationLoadingState />
            ) : tender === null ? (
                <AdjudicationUnavailableState />
            ) : (
                <>
                    <AdjudicationWorkspaceHeader
                        tender={tender}
                        isRefreshing={
                            isRefreshing
                        }
                        onRefresh={
                            () => {
                                void loadWorkspace(
                                    true
                                )
                            }
                        }
                    />

                    <AdjudicationTenderSummary
                        tender={tender}
                        assignment={
                            assignment
                        }
                        rankingCount={
                            rankings.length
                        }
                    />

                    {assignment === null ? (
                        <AdjudicatorAssignmentNotice />
                    ) : tender.status
                    !== 'ADJUDICATION'
                    && tender.status
                    !== 'AWARDED' ? (
                        <AdjudicationStateNotice
                            tender={tender}
                        />
                    ) : rankings.length === 0 ? (
                        <NoAdjudicationRankingsState />
                    ) : (
                        <>
                            <section
                                className={
                                    'content-card '
                                    + 'adjudication-'
                                    + 'summary-introduction'
                                }
                                aria-labelledby={
                                    'adjudication-ranking-heading'
                                }
                            >
                                <div>
                                    <span
                                        className="eyebrow"
                                    >
                                        Evaluation
                                        recommendation
                                    </span>

                                    <h2
                                        id={
                                            'adjudication-ranking-heading'
                                        }
                                    >
                                        Ranked bids
                                    </h2>

                                    <p>
                                        Review the
                                        anonymised
                                        evaluation
                                        results before
                                        recording the
                                        final award
                                        decision.
                                    </p>
                                </div>

                                {highestRankedBid
                                    !== null && (
                                        <div
                                            className={
                                                'adjudication-'
                                                + 'leader-summary'
                                            }
                                        >
                                        <span>
                                            Highest
                                            evaluated
                                            bid
                                        </span>

                                            <strong>
                                                {
                                                    highestRankedBid
                                                        .bidderAlias
                                                }
                                            </strong>

                                            <small>
                                                {
                                                    highestRankedBid
                                                        .averageTotalScore
                                                        .toFixed(
                                                            2
                                                        )
                                                }
                                                {' / 100'}
                                            </small>
                                        </div>
                                    )}
                            </section>

                            <section
                                className={
                                    'adjudication-ranking-list'
                                }
                                aria-label={
                                    'Ranked adjudication bids'
                                }
                            >
                                {rankings.map(
                                    (ranking) => (
                                        <AdjudicationRankingCard
                                            key={
                                                ranking
                                                    .bidId
                                            }
                                            ranking={
                                                ranking
                                            }
                                            selected={
                                                selectedBid
                                                    ?.bidId
                                                === ranking
                                                    .bidId
                                            }
                                            disabled={
                                                isReadOnly
                                                || isAwarding
                                            }
                                            onSelect={
                                                handleBidSelect
                                            }
                                        />
                                    )
                                )}
                            </section>

                            {awardResult !== null ? (
                                <AwardResultPanel
                                    result={
                                        awardResult
                                    }
                                />
                            ) : (
                                <AwardDecisionPanel
                                    tender={tender}
                                    form={form}
                                    errors={
                                        formErrors
                                    }
                                    selectedBid={
                                        selectedBid
                                    }
                                    isAwarding={
                                        isAwarding
                                    }
                                    isReadOnly={
                                        isReadOnly
                                    }
                                    onAwardReasonChange={
                                        handleAwardReasonChange
                                    }
                                    onSubmit={
                                        handleAwardSubmit
                                    }
                                />
                            )}
                        </>
                    )}

                    <AdjudicationGovernanceNotice />
                </>
            )}

            <dialog
                ref={awardConfirmationDialogRef}
                className={
                    'confirmation-dialog '
                    + 'adjudication-confirmation-dialog'
                }
                aria-labelledby={
                    'adjudication-confirmation-title'
                }
                aria-describedby={
                    'adjudication-confirmation-description'
                }
                onClose={
                    () => {
                        setIsAwardConfirmationOpen(false)
                        setPendingAwardBid(null)
                    }
                }
                onCancel={
                    (event) => {
                        if (isAwarding) {
                            event.preventDefault()
                            return
                        }

                        setPendingAwardBid(null)
                    }
                }
            >
                <div
                    className={
                        'confirmation-dialog-content'
                    }
                >
                    <div
                        className={
                            'adjudication-confirmation-icon'
                        }
                        aria-hidden="true"
                    >
                        !
                    </div>

                    <span className="eyebrow">
                        Final decision
                    </span>

                    <h2
                        id={
                            'adjudication-confirmation-title'
                        }
                    >
                        Award selected bid?
                    </h2>

                    <p
                        id={
                            'adjudication-confirmation-description'
                        }
                    >
                        Awarding{' '}
                        <strong>
                            {pendingAwardBid?.bidderAlias}
                        </strong>
                        {' '}for{' '}
                        <strong>
                            {tender?.tenderReference}
                        </strong>
                        {' '}will complete this tender.
                    </p>

                    <div
                        className={
                            'adjudication-confirmation-warning'
                        }
                    >
                        <strong>
                            This action cannot be undone
                        </strong>

                        <span>
                            Other eligible bids will be rejected
                            and the selected bid will be recorded
                            as awarded.
                        </span>
                    </div>

                    <div className="form-actions">
                        <button
                            className="secondary-button"
                            type="button"
                            disabled={isAwarding}
                            onClick={
                                cancelAwardConfirmation
                            }
                        >
                            Cancel
                        </button>

                        <button
                            className={
                                'primary-button '
                                + 'adjudication-confirm-award-button'
                            }
                            type="button"
                            disabled={isAwarding}
                            onClick={
                                () => {
                                    void confirmAward()
                                }
                            }
                        >
                            {isAwarding
                                ? 'Awarding...'
                                : 'Confirm award'}
                        </button>
                    </div>
                </div>
            </dialog>
        </ProcurementLayout>
    )
}

interface AdjudicationWorkspaceHeaderProps {
    tender: ProcurementTender
    isRefreshing: boolean
    onRefresh: () => void
}

function AdjudicationWorkspaceHeader({
                                         tender,
                                         isRefreshing,
                                         onRefresh,
                                     }: AdjudicationWorkspaceHeaderProps) {
    return (
        <section className="page-heading">
            <div>
                <span className="eyebrow">
                    Bid Adjudication Committee
                </span>

                <h1>
                    Tender adjudication
                </h1>

                <p>
                    Review ranked evaluation
                    evidence and record the final
                    tender award as an assigned
                    Adjudicator.
                </p>
            </div>

            <div
                className={
                    'adjudication-heading-actions'
                }
            >
                <ProcurementTenderStatusBadge
                    status={tender.status}
                />

                <button
                    className="secondary-button"
                    type="button"
                    onClick={onRefresh}
                    disabled={isRefreshing}
                >
                    {isRefreshing
                        ? 'Refreshing...'
                        : 'Refresh workspace'}
                </button>
            </div>
        </section>
    )
}

interface AdjudicationTenderSummaryProps {
    tender: ProcurementTender
    assignment:
        CurrentUserCommitteeAssignment
        | null
    rankingCount: number
}

function AdjudicationTenderSummary({
                                       tender,
                                       assignment,
                                       rankingCount,
                                   }: AdjudicationTenderSummaryProps) {
    return (
        <section
            className={
                'content-card '
                + 'adjudication-tender-summary'
            }
            aria-labelledby={
                'adjudication-tender-heading'
            }
        >
            <div
                className={
                    'adjudication-tender-title'
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

                    <h2
                        id={
                            'adjudication-tender-heading'
                        }
                    >
                        {tender.title}
                    </h2>
                </div>

                <span
                    className={
                        'adjudication-evidence-badge'
                    }
                >
                    {rankingCount}
                    {' ranked '}
                    {rankingCount === 1
                        ? 'bid'
                        : 'bids'}
                </span>
            </div>

            <dl
                className={
                    'adjudication-tender-details'
                }
            >
                <AdjudicationDetailItem
                    label="Buyer organisation"
                    value={
                        tender.buyerOrgId
                    }
                />

                <AdjudicationDetailItem
                    label="Budget"
                    value={
                        formatCurrency(
                            tender.budgetAmount
                        )
                    }
                />

                <AdjudicationDetailItem
                    label="Service area"
                    value={
                        tender.serviceArea
                    }
                />

                <AdjudicationDetailItem
                    label="Business type"
                    value={
                        tender
                            .requiredBusinessType
                    }
                />

                <AdjudicationDetailItem
                    label="Bidding closed"
                    value={
                        tender.closedAt === null
                            ? 'Not recorded'
                            : formatDateTime(
                                tender.closedAt
                            )
                    }
                />

                <AdjudicationDetailItem
                    label="Assignment date"
                    value={
                        assignment === null
                            ? 'No active assignment'
                            : formatDateTime(
                                assignment
                                    .assignedAt
                            )
                    }
                />
            </dl>

            {assignment !== null && (
                <div
                    className={
                        'adjudication-assignment-reason'
                    }
                >
                    <span>
                        Adjudicator assignment
                    </span>

                    <p>
                        {
                            assignment
                                .assignmentReason
                            ?? (
                                'No assignment '
                                + 'reason was '
                                + 'recorded.'
                            )
                        }
                    </p>
                </div>
            )}
        </section>
    )
}

interface AdjudicationDetailItemProps {
    label: string
    value: string
}

function AdjudicationDetailItem({
                                    label,
                                    value,
                                }: AdjudicationDetailItemProps) {
    return (
        <div>
            <dt>{label}</dt>
            <dd>{value}</dd>
        </div>
    )
}

interface AwardDecisionPanelProps {
    tender: ProcurementTender
    form:
        AdjudicationAwardFormState
    errors:
        AdjudicationAwardFormErrors
    selectedBid:
        BidRanking | null
    isAwarding: boolean
    isReadOnly: boolean
    onAwardReasonChange:
        (
            event:
            ChangeEvent<
                HTMLTextAreaElement
            >,
        ) => void
    onSubmit:
        (
            event:
            FormEvent<HTMLFormElement>,
        ) => Promise<void>
}

function AwardDecisionPanel({
                                tender,
                                form,
                                errors,
                                selectedBid,
                                isAwarding,
                                isReadOnly,
                                onAwardReasonChange,
                                onSubmit,
                            }: AwardDecisionPanelProps) {
    return (
        <section
            className={
                'content-card '
                + 'adjudication-award-panel'
            }
            aria-labelledby={
                'adjudication-award-heading'
            }
        >
            <div
                className={
                    'adjudication-award-heading'
                }
            >
                <div>
                    <span className="eyebrow">
                        Final decision
                    </span>

                    <h2
                        id={
                            'adjudication-award-heading'
                        }
                    >
                        Record tender award
                    </h2>

                    <p>
                        Confirm the selected ranked
                        bid and provide an
                        evidence-based award reason.
                    </p>
                </div>

                {selectedBid !== null && (
                    <div
                        className={
                            'adjudication-selected-'
                            + 'bid-summary'
                        }
                    >
                        <span>
                            Selected bid
                        </span>

                        <strong>
                            {
                                selectedBid
                                    .bidderAlias
                            }
                        </strong>

                        <small>
                            Rank
                            {' '}
                            {selectedBid.rank}
                            {' • '}
                            {
                                selectedBid
                                    .averageTotalScore
                                    .toFixed(2)
                            }
                            {' / 100'}
                        </small>
                    </div>
                )}
            </div>

            <form
                className={
                    'adjudication-award-form'
                }
                onSubmit={
                    (event) => {
                        void onSubmit(event)
                    }
                }
                noValidate
            >
                {errors.selectedBidId && (
                    <p
                        className={
                            'field-error'
                        }
                        role="alert"
                    >
                        {
                            errors
                                .selectedBidId
                        }
                    </p>
                )}

                <div className="form-field">
                    <label
                        htmlFor={
                            'adjudication-award-reason'
                        }
                    >
                        Award reason
                    </label>

                    <textarea
                        id={
                            'adjudication-award-reason'
                        }
                        rows={6}
                        minLength={10}
                        maxLength={1000}
                        value={
                            form.awardReason
                        }
                        onChange={
                            onAwardReasonChange
                        }
                        disabled={
                            isReadOnly
                            || isAwarding
                        }
                        aria-invalid={
                            errors.awardReason
                            !== undefined
                        }
                        aria-describedby={
                            errors.awardReason
                                ? (
                                    'adjudication-award-'
                                    + 'reason-error'
                                )
                                : (
                                    'adjudication-award-'
                                    + 'reason-help'
                                )
                        }
                        placeholder={
                            'Explain how the '
                            + 'selected bid satisfies '
                            + 'the published criteria '
                            + 'and supports the final '
                            + 'award decision.'
                        }
                    />

                    <div
                        className={
                            'adjudication-award-'
                            + 'field-help'
                        }
                    >
                        <small
                            id={
                                'adjudication-award-'
                                + 'reason-help'
                            }
                        >
                            Minimum 10 characters.
                            The reason becomes part
                            of the permanent award
                            record.
                        </small>

                        <small>
                            {
                                form.awardReason
                                    .length
                            }
                            {' / 1000'}
                        </small>
                    </div>

                    {errors.awardReason && (
                        <p
                            id={
                                'adjudication-award-'
                                + 'reason-error'
                            }
                            className={
                                'field-error'
                            }
                            role="alert"
                        >
                            {
                                errors
                                    .awardReason
                            }
                        </p>
                    )}
                </div>

                {errors.form && (
                    <p
                        className={
                            'form-error-message'
                        }
                        role="alert"
                    >
                        {errors.form}
                    </p>
                )}

                <div
                    className={
                        'adjudication-consequential-'
                        + 'notice'
                    }
                >
                    <strong>
                        Consequential action
                    </strong>

                    <p>
                        Awarding
                        {' '}
                        {tender.tenderReference}
                        {' '}
                        marks the selected bid as
                        awarded, rejects other
                        eligible bids and completes
                        the tender. This action
                        cannot be repeated.
                    </p>
                </div>

                <div
                    className="form-actions"
                >
                    <button
                        className="primary-button"
                        type="submit"
                        disabled={
                            isReadOnly
                            || isAwarding
                            || selectedBid === null
                        }
                    >
                        {isAwarding
                            ? 'Recording award...'
                            : 'Award selected bid'}
                    </button>
                </div>
            </form>
        </section>
    )
}

interface AwardResultPanelProps {
    result: AwardTenderResponse
}

function AwardResultPanel({
                              result,
                          }: AwardResultPanelProps) {
    return (
        <section
            className={
                'content-card '
                + 'adjudication-award-result'
            }
            aria-labelledby={
                'adjudication-award-result-heading'
            }
        >
            <div>
                <span className="eyebrow">
                    Award recorded
                </span>

                <h2
                    id={
                        'adjudication-award-result-heading'
                    }
                >
                    Tender awarded successfully
                </h2>

                <p>
                    The final decision has been
                    persisted and the winning
                    Trader notification has been
                    queued.
                </p>
            </div>

            <dl
                className={
                    'adjudication-award-result-grid'
                }
            >
                <AdjudicationDetailItem
                    label="Tender reference"
                    value={
                        result.tenderReference
                    }
                />

                <AdjudicationDetailItem
                    label="Winning bid"
                    value={
                        result
                            .winningBidderAlias
                    }
                />

                <AdjudicationDetailItem
                    label="Winning bid reference"
                    value={
                        result
                            .winningBidReference
                    }
                />

                <AdjudicationDetailItem
                    label="Tender status"
                    value={
                        formatStatus(
                            result.tenderStatus
                        )
                    }
                />

                <AdjudicationDetailItem
                    label="Adjudicator user ID"
                    value={
                        result
                            .adjudicatorUserId
                            .toString()
                    }
                />

                <AdjudicationDetailItem
                    label="Awarded"
                    value={
                        formatDateTime(
                            result.awardedAt
                        )
                    }
                />
            </dl>

            <div
                className={
                    'adjudication-award-reason'
                }
            >
                <span>
                    Award reason
                </span>

                <p>
                    {result.awardReason}
                </p>
            </div>
        </section>
    )
}

function AdjudicationLoadingState() {
    return (
        <section
            className={
                'content-card '
                + 'adjudication-loading-state'
            }
            aria-busy="true"
        >
            <div className="empty-state">
                <h1>
                    Loading adjudication
                    workspace
                </h1>

                <p>
                    Verifying the active
                    Adjudicator assignment and
                    retrieving ranked evaluation
                    evidence.
                </p>
            </div>
        </section>
    )
}

function AdjudicationUnavailableState() {
    return (
        <section className="content-card">
            <div className="empty-state">
                <h1>
                    Adjudication unavailable
                </h1>

                <p>
                    The tender or adjudication
                    workspace could not be loaded.
                </p>

                <Link
                    className={
                        'primary-link-button'
                    }
                    to={
                        '/procurement/'
                        + 'adjudications'
                    }
                >
                    Return to adjudications
                </Link>
            </div>
        </section>
    )
}

function AdjudicatorAssignmentNotice() {
    return (
        <section
            className={
                'content-card '
                + 'evaluation-access-notice '
                + 'is-restricted'
            }
            role="alert"
        >
            <span className="eyebrow">
                Assignment required
            </span>

            <h2>
                Adjudication access denied
            </h2>

            <p>
                The current user does not have an
                active Adjudicator committee
                assignment for this tender.
            </p>
        </section>
    )
}

interface AdjudicationStateNoticeProps {
    tender: ProcurementTender
}

function AdjudicationStateNotice({
                                     tender,
                                 }: AdjudicationStateNoticeProps) {
    return (
        <section
            className={
                'content-card '
                + 'evaluation-access-notice'
            }
        >
            <span className="eyebrow">
                Workflow state
            </span>

            <h2>
                Adjudication is not available
            </h2>

            <p>
                Tender
                {' '}
                <strong>
                    {tender.tenderReference}
                </strong>
                {' '}
                is currently in
                {' '}
                <strong>
                    {
                        formatStatus(
                            tender.status
                        )
                    }
                </strong>
                {' '}
                status.
            </p>
        </section>
    )
}

function NoAdjudicationRankingsState() {
    return (
        <section className="content-card">
            <div className="empty-state">
                <h2>
                    No ranked bids available
                </h2>

                <p>
                    No completed evaluation
                    results are available for this
                    tender.
                </p>
            </div>
        </section>
    )
}

function AdjudicationGovernanceNotice() {
    return (
        <section
            className={
                'content-card '
                + 'adjudication-governance-notice'
            }
            aria-labelledby={
                'adjudication-governance-heading'
            }
        >
            <div>
                <span className="eyebrow">
                    Adjudication governance
                </span>

                <h2
                    id={
                        'adjudication-governance-heading'
                    }
                >
                    Required Adjudicator conduct
                </h2>
            </div>

            <ul>
                <li>
                    Review only tenders assigned
                    to the authenticated
                    Adjudicator.
                </li>

                <li>
                    Use the published criteria and
                    completed evaluation evidence.
                </li>

                <li>
                    Do not alter Evaluator scores.
                </li>

                <li>
                    Do not attempt to identify the
                    Trader behind an anonymised
                    bidder alias.
                </li>

                <li>
                    Record a clear, evidence-based
                    reason for the final award.
                </li>
            </ul>
        </section>
    )
}

function findAdjudicatorAssignment(
    assignments:
    CurrentUserCommitteeAssignment[],
    tenderId: number,
): CurrentUserCommitteeAssignment
    | null {
    return assignments.find(
        (currentAssignment) =>
            currentAssignment.active
            && currentAssignment.tenderId
            === tenderId
            && currentAssignment
                .committeeRole
            === 'ADJUDICATOR'
    ) ?? null
}

function getWorkspaceErrorMessage(
    error: unknown,
): string {
    const adjudicationMessage =
        getAdjudicationApiErrorMessage(
            error,
            ''
        )

    if (
        adjudicationMessage.length > 0
    ) {
        return adjudicationMessage
    }

    const committeeMessage =
        getCommitteeAssignmentApiErrorMessage(
            error,
            ''
        )

    if (
        committeeMessage.length > 0
    ) {
        return committeeMessage
    }

    return getProcurementTenderApiErrorMessage(
        error,
        'Unable to load the '
        + 'adjudication workspace.'
    )
}

function parseTenderId(
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