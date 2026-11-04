import {
    useCallback,
    useEffect,
    useRef,
    useState,
} from 'react'

import {
    Link,
    useLocation,
    useParams,
} from 'react-router-dom'

import {
    getBidEvaluationApiErrorMessage,
    getTenderBidActivity,
} from '../api/bidEvaluationApi'

import {
    closeProcurementTenderBidding,
    getProcurementTender,
    getProcurementTenderApiErrorMessage,
    publishProcurementTender,
    startProcurementTenderEvaluation,
    verifyProcurementTenderSpecification,
} from '../api/procurementTenderApi'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import {
    ProcurementTenderStatusBadge,
} from '../components/procurement/ProcurementTenderStatusBadge'

import type {
    TenderBidActivity,
} from '../types/bidEvaluation'

import type {
    ProcurementTender,
    SpecificationIntegrity,
} from '../types/procurementTender'

import {
    canCloseTenderBidding,
    canPublishTender,
    canStartTenderEvaluation,
    canVerifyTenderSpecification,
} from '../types/procurementTender'

import './SpecificationTenderPage.css'

type TenderDetailsAction =
    | 'PUBLISH'
    | 'VERIFY_INTEGRITY'
    | 'CLOSE_BIDDING'
    | 'START_EVALUATION'

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

export function SpecificationTenderDetailsPage() {
    const routeParameters =
        useParams()

    const location =
        useLocation()

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
        bidActivity,
        setBidActivity,
    ] =
        useState<TenderBidActivity | null>(
            null
        )

    const [
        integrityResult,
        setIntegrityResult,
    ] =
        useState<
            SpecificationIntegrity | null
        >(null)

    const [
        pendingAction,
        setPendingAction,
    ] =
        useState<
            TenderDetailsAction | null
        >(null)

    const [
        isCloseBiddingDialogOpen,
        setIsCloseBiddingDialogOpen,
    ] = useState(false)

    const [
        isStartEvaluationDialogOpen,
        setIsStartEvaluationDialogOpen,
    ] = useState(false)

    const closeBiddingDialogRef =
        useRef<HTMLDialogElement>(null)

    const startEvaluationDialogRef =
        useRef<HTMLDialogElement>(null)

    const [
        isLoading,
        setIsLoading,
    ] = useState(true)

    const [
        isBidActivityLoading,
        setIsBidActivityLoading,
    ] = useState(true)

    const [
        errorMessage,
        setErrorMessage,
    ] =
        useState<string | null>(
            tenderId === null
                ? 'A valid tender ID is required.'
                : null
        )

    const [
        bidActivityError,
        setBidActivityError,
    ] =
        useState<string | null>(
            null
        )

    const [
        successMessage,
        setSuccessMessage,
    ] =
        useState<string | null>(
            getNavigationSuccessMessage(
                location.state
            )
        )

    const loadTender =
        useCallback(
            async (): Promise<void> => {
                if (tenderId === null) {
                    setIsLoading(false)

                    setErrorMessage(
                        'A valid tender ID '
                        + 'is required.'
                    )

                    return
                }

                setIsLoading(true)
                setErrorMessage(null)

                try {
                    const response =
                        await getProcurementTender(
                            tenderId
                        )

                    setTender(response)
                } catch (error) {
                    setErrorMessage(
                        getProcurementTenderApiErrorMessage(
                            error,
                            'Unable to load the '
                            + 'tender specification.'
                        )
                    )
                } finally {
                    setIsLoading(false)
                }
            },
            [
                tenderId,
            ]
        )

    const loadBidActivity =
        useCallback(
            async (): Promise<void> => {
                if (tenderId === null) {
                    setIsBidActivityLoading(
                        false
                    )

                    return
                }

                setIsBidActivityLoading(
                    true
                )

                setBidActivityError(
                    null
                )

                try {
                    const response =
                        await getTenderBidActivity(
                            tenderId
                        )

                    setBidActivity(
                        response
                    )
                } catch (error) {
                    setBidActivityError(
                        getBidEvaluationApiErrorMessage(
                            error,
                            'Unable to load tender '
                            + 'bid activity.'
                        )
                    )
                } finally {
                    setIsBidActivityLoading(
                        false
                    )
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
                    void loadTender()
                    void loadBidActivity()
                }
            )
        },
        [
            loadTender,
            loadBidActivity,
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

    useEffect(
        () => {
            const dialog =
                closeBiddingDialogRef.current

            if (dialog === null) {
                return
            }

            if (
                isCloseBiddingDialogOpen
                && !dialog.open
            ) {
                dialog.showModal()
            } else if (
                !isCloseBiddingDialogOpen
                && dialog.open
            ) {
                dialog.close()
            }
        },
        [
            isCloseBiddingDialogOpen,
        ]
    )

    useEffect(
        () => {
            const dialog =
                startEvaluationDialogRef.current

            if (dialog === null) {
                return
            }

            if (
                isStartEvaluationDialogOpen
                && !dialog.open
            ) {
                dialog.showModal()
            } else if (
                !isStartEvaluationDialogOpen
                && dialog.open
            ) {
                dialog.close()
            }
        },
        [
            isStartEvaluationDialogOpen,
        ]
    )

    async function handlePublish():
        Promise<void> {
        if (tender === null) {
            return
        }

        const confirmed =
            window.confirm(
                'Publish '
                + tender.tenderReference
                + '? The specification will '
                + 'be locked with a SHA-256 '
                + 'evidence hash.'
            )

        if (!confirmed) {
            return
        }

        await executeTenderTransition(
            'PUBLISH',
            () =>
                publishProcurementTender(
                    tender.id
                ),
            'Tender published successfully.'
        )
    }

    async function handleCloseBidding():
        Promise<void> {
        if (tender === null) {
            return
        }

        setIsCloseBiddingDialogOpen(true)
    }

    async function confirmCloseBidding():
        Promise<void> {
        if (
            tender === null
            || pendingAction !== null
        ) {
            return
        }

        setIsCloseBiddingDialogOpen(false)

        await executeTenderTransition(
            'CLOSE_BIDDING',
            () =>
                closeProcurementTenderBidding(
                    tender.id
                ),
            'Tender bidding was closed.'
        )
    }

    async function handleStartEvaluation():
        Promise<void> {
        if (tender === null) {
            return
        }

        setIsStartEvaluationDialogOpen(true)
    }

    async function confirmStartEvaluation():
        Promise<void> {
        if (
            tender === null
            || pendingAction !== null
        ) {
            return
        }

        setIsStartEvaluationDialogOpen(false)

        await executeTenderTransition(
            'START_EVALUATION',
            () =>
                startProcurementTenderEvaluation(
                    tender.id
                ),
            'Tender moved to evaluation.'
        )
    }

    async function handleVerifyIntegrity():
        Promise<void> {
        if (tender === null) {
            return
        }

        setPendingAction(
            'VERIFY_INTEGRITY'
        )

        setErrorMessage(null)
        setSuccessMessage(null)
        setIntegrityResult(null)

        try {
            const response =
                await verifyProcurementTenderSpecification(
                    tender.id
                )

            setIntegrityResult(response)

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
                    + 'published specification.'
                )
            )
        } finally {
            setPendingAction(null)
        }
    }

    async function executeTenderTransition(
        action: TenderDetailsAction,
        request:
        () => Promise<ProcurementTender>,
        successText: string,
    ): Promise<void> {
        setPendingAction(action)
        setErrorMessage(null)
        setSuccessMessage(null)
        setIntegrityResult(null)

        try {
            const updatedTender =
                await request()

            setTender(updatedTender)

            setSuccessMessage(
                successText
                + ' '
                + updatedTender
                    .tenderReference
            )

            await loadBidActivity()
        } catch (error) {
            setErrorMessage(
                getProcurementTenderApiErrorMessage(
                    error,
                    'Unable to complete the '
                    + 'tender workflow action.'
                )
            )

            await loadTender()
            await loadBidActivity()
        } finally {
            setPendingAction(null)
        }
    }

    const isActionPending =
        pendingAction !== null

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

            {isLoading && tender === null ? (
                <section
                    className={
                        'content-card '
                        + 'specification-loading-card'
                    }
                    aria-busy="true"
                >
                    <p>
                        Loading tender
                        specification...
                    </p>
                </section>
            ) : tender === null ? (
                <section className="content-card">
                    <div className="empty-state">
                        <h1>
                            Tender unavailable
                        </h1>

                        <p>
                            The requested tender could
                            not be loaded. Return to
                            the specification queue
                            and select another tender.
                        </p>
                    </div>
                </section>
            ) : (
                <>
                    <TenderDetailHeader
                        tender={tender}
                    />

                    <TenderBidActivityPanel
                        activity={
                            bidActivity
                        }
                        isLoading={
                            isBidActivityLoading
                        }
                        errorMessage={
                            bidActivityError
                        }
                        onRefresh={
                            () => {
                                void loadBidActivity()
                            }
                        }
                    />

                    {integrityResult !== null && (
                        <SpecificationIntegrityPanel
                            result={
                                integrityResult
                            }
                        />
                    )}

                    <div
                        className={
                            'specification-detail-grid'
                        }
                    >
                        <section
                            className={
                                'content-card '
                                + 'specification-detail-wide'
                            }
                            aria-labelledby={
                                'tender-description-heading'
                            }
                        >
                            <span className="eyebrow">
                                Procurement requirement
                            </span>

                            <h2
                                id={
                                    'tender-description-heading'
                                }
                            >
                                Tender description
                            </h2>

                            <p
                                className={
                                    'specification-detail-text'
                                }
                            >
                                {tender.description}
                            </p>
                        </section>

                        <section
                            className="content-card"
                            aria-labelledby={
                                'tender-commercial-heading'
                            }
                        >
                            <span className="eyebrow">
                                Commercial scope
                            </span>

                            <h2
                                id={
                                    'tender-commercial-heading'
                                }
                            >
                                Procurement details
                            </h2>

                            <dl
                                className={
                                    'specification-detail-list'
                                }
                            >
                                <DetailItem
                                    label="Budget"
                                    value={
                                        formatCurrency(
                                            tender
                                                .budgetAmount
                                        )
                                    }
                                />

                                <DetailItem
                                    label={
                                        'Buyer organisation'
                                    }
                                    value={
                                        tender.buyerOrgId
                                    }
                                />

                                <DetailItem
                                    label={
                                        'Required business type'
                                    }
                                    value={
                                        tender
                                            .requiredBusinessType
                                    }
                                />

                                <DetailItem
                                    label="Service area"
                                    value={
                                        tender.serviceArea
                                    }
                                />
                            </dl>
                        </section>

                        <section
                            className="content-card"
                            aria-labelledby={
                                'tender-lifecycle-heading'
                            }
                        >
                            <span className="eyebrow">
                                Workflow evidence
                            </span>

                            <h2
                                id={
                                    'tender-lifecycle-heading'
                                }
                            >
                                Lifecycle
                            </h2>

                            <dl
                                className={
                                    'specification-detail-list'
                                }
                            >
                                <DetailItem
                                    label="Created"
                                    value={
                                        formatDateTime(
                                            tender
                                                .createdAt
                                        )
                                    }
                                />

                                <DetailItem
                                    label="Updated"
                                    value={
                                        formatDateTime(
                                            tender
                                                .updatedAt
                                        )
                                    }
                                />

                                <DetailItem
                                    label="Published"
                                    value={
                                        tender
                                            .publishedAt
                                        === null
                                            ? 'Not published'
                                            : formatDateTime(
                                                tender
                                                    .publishedAt
                                            )
                                    }
                                />

                                <DetailItem
                                    label={
                                        'Bidding closed'
                                    }
                                    value={
                                        tender.closedAt
                                        === null
                                            ? 'Not closed'
                                            : formatDateTime(
                                                tender
                                                    .closedAt
                                            )
                                    }
                                />
                            </dl>
                        </section>

                        <section
                            className={
                                'content-card '
                                + 'specification-detail-wide'
                            }
                            aria-labelledby={
                                'tender-evaluation-heading'
                            }
                        >
                            <span className="eyebrow">
                                Published scoring basis
                            </span>

                            <h2
                                id={
                                    'tender-evaluation-heading'
                                }
                            >
                                Evaluation criteria
                            </h2>

                            <p
                                className={
                                    'specification-detail-text '
                                    + 'specification-criteria-text'
                                }
                            >
                                {
                                    tender
                                        .evaluationCriteria
                                }
                            </p>
                        </section>

                        <section
                            className={
                                'content-card '
                                + 'specification-detail-wide'
                            }
                            aria-labelledby={
                                'tender-evidence-heading'
                            }
                        >
                            <span className="eyebrow">
                                Integrity control
                            </span>

                            <h2
                                id={
                                    'tender-evidence-heading'
                                }
                            >
                                Specification evidence
                            </h2>

                            {tender.specificationHash
                            === null ? (
                                <p
                                    className={
                                        'specification-detail-muted'
                                    }
                                >
                                    The tender remains
                                    in draft and does
                                    not yet have a
                                    specification hash.
                                </p>
                            ) : (
                                <div
                                    className={
                                        'specification-hash-panel'
                                    }
                                >
                                    <span>
                                        Published
                                        specification
                                        SHA-256
                                    </span>

                                    <code>
                                        {
                                            tender
                                                .specificationHash
                                        }
                                    </code>
                                </div>
                            )}
                        </section>
                    </div>

                    <TenderWorkflowActions
                        tender={tender}
                        bidActivity={
                            bidActivity
                        }
                        pendingAction={
                            pendingAction
                        }
                        isActionPending={
                            isActionPending
                        }
                        onPublish={
                            () => {
                                void handlePublish()
                            }
                        }
                        onVerify={
                            () => {
                                void handleVerifyIntegrity()
                            }
                        }
                        onCloseBidding={
                            () => {
                                void handleCloseBidding()
                            }
                        }
                        onStartEvaluation={
                            () => {
                                void handleStartEvaluation()
                            }
                        }
                        onRefresh={
                            () => {
                                void loadTender()
                                void loadBidActivity()
                            }
                        }
                    />
                </>
            )}

            <dialog
                ref={closeBiddingDialogRef}
                className="confirmation-dialog"
                aria-labelledby={
                    'close-bidding-dialog-title'
                }
                aria-describedby={
                    'close-bidding-dialog-description'
                }
                onClose={
                    () => {
                        setIsCloseBiddingDialogOpen(
                            false
                        )
                    }
                }
            >
                <div
                    className={
                        'confirmation-dialog-content'
                    }
                >
                    <span className="eyebrow">
                        Controlled transition
                    </span>

                    <h2
                        id={
                            'close-bidding-dialog-title'
                        }
                    >
                        Close bidding?
                    </h2>

                    <p
                        id={
                            'close-bidding-dialog-description'
                        }
                    >
                        Close bidding for{' '}
                        <strong>
                            {tender?.tenderReference}
                        </strong>
                        ? Traders will no longer be
                        able to submit or replace bids.
                    </p>

                    <div className="form-actions">
                        <button
                            className="secondary-button"
                            type="button"
                            onClick={
                                () => {
                                    setIsCloseBiddingDialogOpen(
                                        false
                                    )
                                }
                            }
                        >
                            Cancel
                        </button>

                        <button
                            className={
                                'primary-button '
                                + 'specification-close-confirm-button'
                            }
                            type="button"
                            onClick={
                                () => {
                                    void confirmCloseBidding()
                                }
                            }
                        >
                            Close bidding
                        </button>
                    </div>
                </div>
            </dialog>

            <dialog
                ref={startEvaluationDialogRef}
                className="confirmation-dialog"
                aria-labelledby={
                    'start-evaluation-dialog-title'
                }
                aria-describedby={
                    'start-evaluation-dialog-description'
                }
                onClose={
                    () => {
                        setIsStartEvaluationDialogOpen(
                            false
                        )
                    }
                }
            >
                <div
                    className={
                        'confirmation-dialog-content'
                    }
                >
                    <span className="eyebrow">
                        Controlled transition
                    </span>

                    <h2
                        id={
                            'start-evaluation-dialog-title'
                        }
                    >
                        Start evaluation?
                    </h2>

                    <p
                        id={
                            'start-evaluation-dialog-description'
                        }
                    >
                        Move{' '}
                        <strong>
                            {tender?.tenderReference}
                        </strong>
                        {' '}into evaluation? Only
                        assigned evaluators should score
                        its anonymised bids.
                    </p>

                    <div className="form-actions">
                        <button
                            className="secondary-button"
                            type="button"
                            onClick={
                                () => {
                                    setIsStartEvaluationDialogOpen(
                                        false
                                    )
                                }
                            }
                        >
                            Cancel
                        </button>

                        <button
                            className={
                                'primary-button '
                                + 'specification-evaluation-confirm-button'
                            }
                            type="button"
                            onClick={
                                () => {
                                    void confirmStartEvaluation()
                                }
                            }
                        >
                            Start evaluation
                        </button>
                    </div>
                </div>
            </dialog>
        </ProcurementLayout>
    )
}

interface TenderDetailHeaderProps {
    tender: ProcurementTender
}

function TenderDetailHeader({
                                tender,
                            }: TenderDetailHeaderProps) {
    return (
        <section
            className={
                'specification-detail-header'
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

                <h1>{tender.title}</h1>

                <p>
                    Specification created by user
                    {' '}
                    <strong>
                        {tender.createdByUserId}
                    </strong>
                </p>
            </div>

            <ProcurementTenderStatusBadge
                status={tender.status}
            />
        </section>
    )
}

interface TenderBidActivityPanelProps {
    activity:
        TenderBidActivity | null
    isLoading: boolean
    errorMessage: string | null
    onRefresh: () => void
}

function TenderBidActivityPanel({
                                    activity,
                                    isLoading,
                                    errorMessage,
                                    onRefresh,
                                }: TenderBidActivityPanelProps) {
    return (
        <section
            className={
                'content-card '
                + 'tender-bid-activity-panel'
            }
            aria-labelledby={
                'tender-bid-activity-heading'
            }
        >
            <div
                className={
                    'tender-bid-activity-header'
                }
            >
                <div>
                    <span className="eyebrow">
                        Submission activity
                    </span>

                    <h2
                        id={
                            'tender-bid-activity-heading'
                        }
                    >
                        Bid activity
                    </h2>

                    <p>
                        Aggregate counts are shown
                        without exposing trader
                        identities, bid prices,
                        bidder aliases or technical
                        proposals.
                    </p>
                </div>

                <button
                    className="secondary-button"
                    type="button"
                    onClick={onRefresh}
                    disabled={isLoading}
                >
                    {isLoading
                        ? 'Refreshing...'
                        : 'Refresh activity'}
                </button>
            </div>

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

            {isLoading && activity === null ? (
                <p
                    className={
                        'tender-bid-activity-loading'
                    }
                    aria-busy="true"
                >
                    Loading bid activity...
                </p>
            ) : activity === null ? (
                <div className="empty-state">
                    <h3>
                        Bid activity unavailable
                    </h3>

                    <p>
                        Aggregate submission counts
                        could not be retrieved.
                    </p>
                </div>
            ) : (
                <>
                    <dl
                        className={
                            'tender-bid-activity-grid'
                        }
                    >
                        <BidActivityItem
                            label="Total submissions"
                            value={
                                activity.totalBids
                            }
                        />

                        <BidActivityItem
                            label="Active submissions"
                            value={
                                activity.activeBids
                            }
                        />

                        <BidActivityItem
                            label="Compliant"
                            value={
                                activity
                                    .compliantBids
                            }
                            tone="positive"
                        />

                        <BidActivityItem
                            label="Compliance failed"
                            value={
                                activity
                                    .complianceFailedBids
                            }
                            tone={
                                activity
                                    .complianceFailedBids
                                > 0
                                    ? 'danger'
                                    : 'default'
                            }
                        />

                        <BidActivityItem
                            label="Withdrawn"
                            value={
                                activity
                                    .withdrawnBids
                            }
                        />

                        <BidActivityItem
                            label="Under evaluation"
                            value={
                                activity
                                    .underEvaluationBids
                            }
                            tone="info"
                        />
                    </dl>

                    <div
                        className={
                            'tender-bid-last-activity'
                        }
                    >
                        <span>
                            Last bid submitted
                        </span>

                        <strong>
                            {
                                activity
                                    .lastBidSubmittedAt
                                === null
                                    ? 'No bids submitted'
                                    : formatDateTime(
                                        activity
                                            .lastBidSubmittedAt
                                    )
                            }
                        </strong>
                    </div>
                </>
            )}
        </section>
    )
}

interface BidActivityItemProps {
    label: string
    value: number
    tone?:
        | 'default'
        | 'positive'
        | 'danger'
        | 'info'
}

function BidActivityItem({
                             label,
                             value,
                             tone = 'default',
                         }: BidActivityItemProps) {
    return (
        <div
            className={
                'tender-bid-activity-item '
                + 'is-'
                + tone
            }
        >
            <dt>{label}</dt>
            <dd>{value}</dd>
        </div>
    )
}

interface TenderWorkflowActionsProps {
    tender: ProcurementTender
    bidActivity:
        TenderBidActivity | null
    pendingAction:
        TenderDetailsAction | null
    isActionPending: boolean
    onPublish: () => void
    onVerify: () => void
    onCloseBidding: () => void
    onStartEvaluation: () => void
    onRefresh: () => void
}

function TenderWorkflowActions({
                                   tender,
                                   bidActivity,
                                   pendingAction,
                                   isActionPending,
                                   onPublish,
                                   onVerify,
                                   onCloseBidding,
                                   onStartEvaluation,
                                   onRefresh,
                               }: TenderWorkflowActionsProps) {
    const hasActiveBids =
        bidActivity !== null
        && bidActivity.activeBids > 0

    const hasCompliantBids =
        bidActivity !== null
        && bidActivity.compliantBids > 0

    const closeBiddingDisabled =
        isActionPending
        || (
            bidActivity !== null
            && !hasActiveBids
        )

    const evaluationDisabled =
        isActionPending
        || (
            bidActivity !== null
            && !hasCompliantBids
        )

    return (
        <section
            className={
                'content-card '
                + 'specification-workflow-panel'
            }
            aria-labelledby={
                'tender-workflow-actions-heading'
            }
        >
            <div>
                <span className="eyebrow">
                    Controlled transition
                </span>

                <h2
                    id={
                        'tender-workflow-actions-heading'
                    }
                >
                    Available workflow actions
                </h2>

                <p>
                    Only actions permitted by the
                    tender’s current state are shown.
                    Aggregate bid activity is used
                    without disclosing protected
                    bid information.
                </p>

                {canCloseTenderBidding(
                        tender
                    )
                    && bidActivity !== null
                    && !hasActiveBids && (
                        <p
                            className={
                                'workflow-action-guidance'
                            }
                        >
                            At least one active bid is
                            required before bidding can
                            be closed.
                        </p>
                    )}

                {canStartTenderEvaluation(
                        tender
                    )
                    && bidActivity !== null
                    && !hasCompliantBids && (
                        <p
                            className={
                                'workflow-action-guidance'
                            }
                        >
                            At least one compliant bid is
                            required before evaluation can
                            begin.
                        </p>
                    )}
            </div>

            <div
                className={
                    'specification-workflow-actions'
                }
            >
                <button
                    className="secondary-button"
                    type="button"
                    onClick={onRefresh}
                    disabled={isActionPending}
                >
                    Refresh tender
                </button>

                {canVerifyTenderSpecification(
                    tender
                ) && (
                    <button
                        className={
                            'secondary-button '
                            + 'specification-integrity-button'
                        }
                        type="button"
                        onClick={onVerify}
                        disabled={isActionPending}
                    >
                        {pendingAction
                        === 'VERIFY_INTEGRITY'
                            ? 'Verifying...'
                            : 'Verify integrity'}
                    </button>
                )}

                {canPublishTender(tender) && (
                    <button
                        className="primary-button"
                        type="button"
                        onClick={onPublish}
                        disabled={isActionPending}
                    >
                        {pendingAction
                        === 'PUBLISH'
                            ? 'Publishing...'
                            : 'Publish tender'}
                    </button>
                )}

                {canCloseTenderBidding(
                    tender
                ) && (
                    <button
                        className={
                            'primary-button '
                            + 'specification-close-button'
                        }
                        type="button"
                        onClick={
                            onCloseBidding
                        }
                        disabled={
                            closeBiddingDisabled
                        }
                    >
                        {pendingAction
                        === 'CLOSE_BIDDING'
                            ? 'Closing bidding...'
                            : 'Close bidding'}
                    </button>
                )}

                {canStartTenderEvaluation(
                    tender
                ) && (
                    <button
                        className={
                            'primary-button '
                            + 'specification-evaluation-button'
                        }
                        type="button"
                        onClick={
                            onStartEvaluation
                        }
                        disabled={
                            evaluationDisabled
                        }
                    >
                        {pendingAction
                        === 'START_EVALUATION'
                            ? 'Starting evaluation...'
                            : 'Start evaluation'}
                    </button>
                )}
            </div>
        </section>
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

interface SpecificationIntegrityPanelProps {
    result: SpecificationIntegrity
}

function SpecificationIntegrityPanel({
                                         result,
                                     }: SpecificationIntegrityPanelProps) {
    const panelClassName =
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
            className={panelClassName}
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
                        {result.tenderReference}
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

function getNavigationSuccessMessage(
    state: unknown,
): string | null {
    if (
        typeof state !== 'object'
        || state === null
        || !(
            'successMessage' in state
        )
    ) {
        return null
    }

    const successMessage =
        Reflect.get(
            state,
            'successMessage'
        )

    if (
        typeof successMessage
        !== 'string'
        || successMessage
            .trim()
            .length === 0
    ) {
        return null
    }

    return successMessage.trim()
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