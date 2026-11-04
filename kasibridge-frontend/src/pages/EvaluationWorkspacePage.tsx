import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from 'react'

import {
    Link,
    useParams,
} from 'react-router-dom'

import {
    getBidEvaluationApiErrorMessage,
    getBlindBidsForEvaluation,
    scoreBlindBid,
} from '../api/bidEvaluationApi'

import {
    getCommitteeAssignmentApiErrorMessage,
    getMyEvaluatorAssignmentQueue,
} from '../api/committeeAssignmentApi'

import {
    getProcurementTender,
    getProcurementTenderApiErrorMessage,
} from '../api/procurementTenderApi'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import {
    BlindBidEvaluationCard,
} from '../components/procurement/BlindBidEvaluationCard'

import {
    ProcurementTenderStatusBadge,
} from '../components/procurement/ProcurementTenderStatusBadge'

import type {
    BidEvaluation,
    EvaluationBidState,
    EvaluateBidRequest,
} from '../types/bidEvaluation'

import {
    calculateEvaluationStatistics,
    createEvaluationBidStates,
    formatTechnicalScore,
    updateEvaluationBidState,
} from '../types/bidEvaluation'

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

export function EvaluationWorkspacePage() {
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

    const [
        assignment,
        setAssignment,
    ] =
        useState<
            CurrentUserCommitteeAssignment
            | null
        >(null)

    const [
        bidStates,
        setBidStates,
    ] =
        useState<EvaluationBidState[]>(
            []
        )

    const [
        isLoading,
        setIsLoading,
    ] = useState(true)

    const [
        isRefreshing,
        setIsRefreshing,
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
                isManualRefresh:
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

                if (isManualRefresh) {
                    setIsRefreshing(true)
                } else {
                    setIsLoading(true)
                }

                setPageError(null)

                try {
                    const [
                        assignmentResponse,
                        tenderResponse,
                    ] =
                        await Promise.all([
                            getMyEvaluatorAssignmentQueue(),
                            getProcurementTender(
                                tenderId
                            ),
                        ])

                    const matchingAssignment =
                        findMatchingAssignment(
                            assignmentResponse,
                            tenderId
                        )

                    if (
                        matchingAssignment === null
                    ) {
                        setAssignment(null)
                        setTender(
                            tenderResponse
                        )

                        setBidStates([])

                        setPageError(
                            'The authenticated '
                            + 'evaluator does not '
                            + 'have an active '
                            + 'Evaluation Committee '
                            + 'assignment for this '
                            + 'tender.'
                        )

                        return
                    }

                    setAssignment(
                        matchingAssignment
                    )

                    setTender(
                        tenderResponse
                    )

                    if (
                        tenderResponse.status
                        !== 'EVALUATION'
                    ) {
                        setBidStates([])

                        return
                    }

                    const bids =
                        await getBlindBidsForEvaluation(
                            tenderId
                        )

                    setBidStates(
                        (currentStates) =>
                            reconcileBidStates(
                                currentStates,
                                createEvaluationBidStates(
                                    bids
                                )
                            )
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

    const statistics =
        useMemo(
            () =>
                calculateEvaluationStatistics(
                    bidStates
                ),
            [
                bidStates,
            ]
        )

    const isWorkspaceReadOnly =
        tender === null
        || assignment === null
        || !assignment.active
        || tender.status !== 'EVALUATION'

    async function handleSubmitEvaluation(
        bidId: number,
        request: EvaluateBidRequest,
    ): Promise<void> {
        if (
            tenderId === null
            || isWorkspaceReadOnly
        ) {
            return
        }

        setSuccessMessage(null)

        setBidStates(
            (currentStates) =>
                updateEvaluationBidState(
                    currentStates,
                    bidId,
                    {
                        status:
                            'SUBMITTING',

                        errorMessage:
                            null,
                    }
                )
        )

        try {
            const evaluation =
                await scoreBlindBid(
                    tenderId,
                    bidId,
                    request
                )

            updateCompletedEvaluation(
                evaluation
            )

            setSuccessMessage(
                'Evaluation submitted for '
                + 'blind bid '
                + bidId
                + '.'
            )
        } catch (error) {
            setBidStates(
                (currentStates) =>
                    updateEvaluationBidState(
                        currentStates,
                        bidId,
                        {
                            status:
                                'FAILED',

                            errorMessage:
                                getBidEvaluationApiErrorMessage(
                                    error,
                                    'Unable to submit '
                                    + 'the bid '
                                    + 'evaluation.'
                                ),
                        }
                    )
            )
        }
    }

    function updateCompletedEvaluation(
        evaluation: BidEvaluation,
    ): void {
        setBidStates(
            (currentStates) =>
                updateEvaluationBidState(
                    currentStates,
                    evaluation.bidId,
                    {
                        status:
                            'SCORED',

                        evaluation,

                        errorMessage:
                            null,
                    }
                )
        )
    }

    return (
        <ProcurementLayout>
            <nav
                className="back-navigation"
                aria-label={
                    'Evaluation navigation'
                }
            >
                <Link
                    to={
                        '/procurement/'
                        + 'evaluations'
                    }
                >
                    Back to my evaluations
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
                <EvaluationLoadingState />
            ) : tender === null ? (
                <EvaluationUnavailableState />
            ) : (
                <>
                    <EvaluationWorkspaceHeader
                        tender={tender}
                        assignment={
                            assignment
                        }
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

                    <EvaluationTenderSummary
                        tender={tender}
                        assignment={
                            assignment
                        }
                    />

                    {assignment !== null && (
                        <EvaluationCriteriaPanel
                            tender={tender}
                        />
                    )}

                    <section
                        className={
                            'statistics-grid '
                            + 'evaluation-statistics-grid'
                        }
                        aria-label={
                            'Evaluation workspace statistics'
                        }
                    >
                        <EvaluationStatistic
                            label="Blind bids"
                            value={
                                statistics
                                    .totalBids
                            }
                            description={
                                'Anonymised bids '
                                + 'available to the '
                                + 'assigned evaluator.'
                            }
                            tone="info"
                        />

                        <EvaluationStatistic
                            label="Submitted scores"
                            value={
                                statistics
                                    .scoredBids
                            }
                            description={
                                'Evaluations submitted '
                                + 'during this workspace '
                                + 'session.'
                            }
                            tone="positive"
                        />

                        <EvaluationStatistic
                            label="Remaining bids"
                            value={
                                statistics
                                    .remainingBids
                            }
                            description={
                                'Blind bids still '
                                + 'requiring a technical '
                                + 'evaluation.'
                            }
                            tone={
                                statistics
                                    .remainingBids > 0
                                    ? 'warning'
                                    : 'positive'
                            }
                        />

                        <EvaluationStatistic
                            label={
                                'Average technical '
                                + 'score'
                            }
                            value={
                                statistics
                                    .averageTechnicalScore
                                === null
                                    ? 'Not available'
                                    : formatTechnicalScore(
                                        statistics
                                            .averageTechnicalScore
                                    )
                            }
                            description={
                                'Average of scores '
                                + 'submitted during the '
                                + 'current session.'
                            }
                            tone="info"
                        />
                    </section>

                    {assignment === null ? (
                        <AssignmentRequiredNotice />
                    ) : tender.status
                    !== 'EVALUATION' ? (
                        <EvaluationStateNotice
                            tender={tender}
                        />
                    ) : bidStates.length === 0 ? (
                        <NoBlindBidsState />
                    ) : (
                        <section
                            className={
                                'blind-bid-evaluation-list'
                            }
                            aria-label={
                                'Anonymised bids '
                                + 'for evaluation'
                            }
                        >
                            {bidStates.map(
                                (bidState) => (
                                    <BlindBidEvaluationCard
                                        key={
                                            bidState
                                                .bid
                                                .bidId
                                        }
                                        bidState={
                                            bidState
                                        }
                                        isWorkspaceReadOnly={
                                            isWorkspaceReadOnly
                                        }
                                        onSubmit={
                                            handleSubmitEvaluation
                                        }
                                    />
                                )
                            )}
                        </section>
                    )}

                    <EvaluationGovernanceNotice />
                </>
            )}
        </ProcurementLayout>
    )
}

interface EvaluationWorkspaceHeaderProps {
    tender: ProcurementTender
    assignment:
        CurrentUserCommitteeAssignment
        | null
    isRefreshing: boolean
    onRefresh: () => void
}

function EvaluationWorkspaceHeader({
                                       tender,
                                       assignment,
                                       isRefreshing,
                                       onRefresh,
                                   }: EvaluationWorkspaceHeaderProps) {
    return (
        <section className="page-heading">
            <div>
                <span className="eyebrow">
                    Bid Evaluation Committee
                </span>

                <h1>
                    Blind bid evaluation
                </h1>

                <p>
                    Evaluate anonymised technical
                    proposals against the published
                    criteria. Bidder identities are
                    intentionally hidden from this
                    workspace.
                </p>
            </div>

            <div
                className={
                    'evaluation-heading-actions'
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

            {assignment === null && (
                <span className="sr-only">
                    No active evaluator
                    assignment exists.
                </span>
            )}
        </section>
    )
}

interface EvaluationTenderSummaryProps {
    tender: ProcurementTender
    assignment:
        CurrentUserCommitteeAssignment
        | null
}

function EvaluationTenderSummary({
                                     tender,
                                     assignment,
                                 }: EvaluationTenderSummaryProps) {
    return (
        <section
            className={
                'content-card '
                + 'evaluation-tender-summary'
            }
            aria-labelledby={
                'evaluation-tender-heading'
            }
        >
            <div
                className={
                    'evaluation-tender-heading'
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
                            'evaluation-tender-heading'
                        }
                    >
                        {tender.title}
                    </h2>
                </div>

                <span
                    className={
                        'evaluation-anonymity-badge'
                    }
                >
                    Double-blind workspace
                </span>
            </div>

            <dl
                className={
                    'evaluation-tender-details'
                }
            >
                <EvaluationDetailItem
                    label="Buyer organisation"
                    value={tender.buyerOrgId}
                />

                <EvaluationDetailItem
                    label="Budget"
                    value={
                        formatCurrency(
                            tender.budgetAmount
                        )
                    }
                />

                <EvaluationDetailItem
                    label="Service area"
                    value={tender.serviceArea}
                />

                <EvaluationDetailItem
                    label="Required business type"
                    value={
                        tender
                            .requiredBusinessType
                    }
                />

                <EvaluationDetailItem
                    label="Bidding closed"
                    value={
                        tender.closedAt === null
                            ? 'Not recorded'
                            : formatDateTime(
                                tender.closedAt
                            )
                    }
                />

                <EvaluationDetailItem
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
                        'evaluation-assignment-reason'
                    }
                >
                    <span>
                        Assignment reason
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

interface EvaluationCriteriaPanelProps {
    tender: ProcurementTender
}

function EvaluationCriteriaPanel({
                                     tender,
                                 }: EvaluationCriteriaPanelProps) {
    return (
        <section
            className={
                'content-card '
                + 'evaluation-criteria-panel'
            }
            aria-labelledby={
                'evaluation-criteria-heading'
            }
        >
            <div>
                <span className="eyebrow">
                    Locked scoring basis
                </span>

                <h2
                    id={
                        'evaluation-criteria-heading'
                    }
                >
                    Published evaluation criteria
                </h2>

                <p>
                    Apply only the criteria published
                    before bidding opened. Do not add
                    or change criteria during
                    evaluation.
                </p>
            </div>

            <div
                className={
                    'evaluation-criteria-content'
                }
            >
                {tender.evaluationCriteria}
            </div>
        </section>
    )
}

interface EvaluationStatisticProps {
    label: string
    value: number | string
    description: string
    tone:
        | 'default'
        | 'positive'
        | 'warning'
        | 'info'
}

function EvaluationStatistic({
                                 label,
                                 value,
                                 description,
                                 tone,
                             }: EvaluationStatisticProps) {
    return (
        <article
            className={
                'statistic-card '
                + 'procurement-dashboard-stat '
                + 'procurement-stat-'
                + tone
            }
        >
            <span>{label}</span>

            <strong>{value}</strong>

            <p>{description}</p>
        </article>
    )
}

interface EvaluationDetailItemProps {
    label: string
    value: string
}

function EvaluationDetailItem({
                                  label,
                                  value,
                              }: EvaluationDetailItemProps) {
    return (
        <div>
            <dt>{label}</dt>
            <dd>{value}</dd>
        </div>
    )
}

function EvaluationLoadingState() {
    return (
        <section
            className={
                'content-card '
                + 'evaluation-loading-state'
            }
            aria-busy="true"
        >
            <div className="empty-state">
                <h1>
                    Loading evaluation workspace
                </h1>

                <p>
                    Verifying the authenticated
                    Evaluator’s assignment and
                    retrieving anonymised bids.
                </p>
            </div>
        </section>
    )
}

function EvaluationUnavailableState() {
    return (
        <section className="content-card">
            <div className="empty-state">
                <h1>
                    Evaluation unavailable
                </h1>

                <p>
                    The tender or evaluation
                    workspace could not be loaded.
                    Return to the assignment queue
                    and select another tender.
                </p>

                <Link
                    className={
                        'primary-link-button'
                    }
                    to={
                        '/procurement/'
                        + 'evaluations'
                    }
                >
                    Return to evaluations
                </Link>
            </div>
        </section>
    )
}

function AssignmentRequiredNotice() {
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
                Evaluation access denied
            </h2>

            <p>
                The current user does not have an
                active Evaluator committee
                assignment for this tender.
            </p>
        </section>
    )
}

interface EvaluationStateNoticeProps {
    tender: ProcurementTender
}

function EvaluationStateNotice({
                                   tender,
                               }: EvaluationStateNoticeProps) {
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
                Scoring is not currently available
            </h2>

            <p>
                Tender
                {' '}
                <strong>
                    {tender.tenderReference}
                </strong>
                {' '}
                is currently in the
                {' '}
                <strong>
                    {
                        formatTenderStatus(
                            tender.status
                        )
                    }
                </strong>
                {' '}
                state. Blind bid scoring is
                available only while the tender is
                in the Evaluation stage.
            </p>
        </section>
    )
}

function NoBlindBidsState() {
    return (
        <section className="content-card">
            <div className="empty-state">
                <h2>
                    No blind bids available
                </h2>

                <p>
                    The tender is in the Evaluation
                    stage, but no anonymised bids are
                    currently available for scoring.
                </p>
            </div>
        </section>
    )
}

function EvaluationGovernanceNotice() {
    return (
        <section
            className={
                'content-card '
                + 'evaluation-governance-notice'
            }
            aria-labelledby={
                'evaluation-governance-heading'
            }
        >
            <div>
                <span className="eyebrow">
                    Evaluation governance
                </span>

                <h2
                    id={
                        'evaluation-governance-heading'
                    }
                >
                    Required evaluator conduct
                </h2>
            </div>

            <ul>
                <li>
                    Evaluate only tenders assigned
                    to the authenticated evaluator.
                </li>

                <li>
                    Use only the published criteria
                    shown in this workspace.
                </li>

                <li>
                    Do not attempt to identify the
                    trader behind a bidder alias.
                </li>

                <li>
                    Provide evidence-based comments
                    supporting every technical score.
                </li>

                <li>
                    Do not alter specifications,
                    adjudicate recommendations or
                    record tender awards.
                </li>
            </ul>
        </section>
    )
}

function findMatchingAssignment(
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
            === 'EVALUATOR'
    ) ?? null
}

function reconcileBidStates(
    currentStates:
    EvaluationBidState[],
    loadedStates:
    EvaluationBidState[],
): EvaluationBidState[] {
    if (currentStates.length === 0) {
        return loadedStates
    }

    return loadedStates.map(
        (loadedState) => {
            const currentState =
                currentStates.find(
                    (candidate) =>
                        candidate.bid.bidId
                        === loadedState.bid.bidId
                )

            if (currentState === undefined) {
                return loadedState
            }

            if (
                currentState.evaluation
                !== null
            ) {
                return currentState
            }

            return {
                ...loadedState,

                status:
                    currentState.status
                    === 'SUBMITTING'
                        ? 'NOT_SCORED'
                        : currentState.status,

                errorMessage:
                currentState
                    .errorMessage,
            }
        }
    )
}

function getWorkspaceErrorMessage(
    error: unknown,
): string {
    const committeeMessage =
        getCommitteeAssignmentApiErrorMessage(
            error,
            ''
        )

    if (committeeMessage.length > 0) {
        return committeeMessage
    }

    const tenderMessage =
        getProcurementTenderApiErrorMessage(
            error,
            ''
        )

    if (tenderMessage.length > 0) {
        return tenderMessage
    }

    return getBidEvaluationApiErrorMessage(
        error,
        'Unable to load the evaluation '
        + 'workspace.'
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
        !Number.isInteger(parsedValue)
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

function formatTenderStatus(
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