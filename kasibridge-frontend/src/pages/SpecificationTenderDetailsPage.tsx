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

type TenderConfirmation =
    | TenderDetailsAction
    | null

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
        isLoading,
        setIsLoading,
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
        successMessage,
        setSuccessMessage,
    ] =
        useState<string | null>(
            getNavigationSuccessMessage(
                location.state
            )
        )

    const confirmationDialogRef =
        useRef<HTMLDialogElement>(null)

    const [
        confirmationAction,
        setConfirmationAction,
    ] =
        useState<TenderConfirmation>(null)

    const loadTender =
        useCallback(
            async (): Promise<void> => {
                if (tenderId === null) {
                    setIsLoading(false)
                    setErrorMessage(
                        'A valid tender ID is required.'
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

    useEffect(
        () => {
            queueMicrotask(
                () => {
                    void loadTender()
                }
            )
        },
        [
            loadTender,
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

    useEffect(
        () => {
            const dialog =
                confirmationDialogRef.current

            if (dialog === null) {
                return
            }

            if (
                confirmationAction !== null
                && !dialog.open
            ) {
                dialog.showModal()
            }

            if (
                confirmationAction === null
                && dialog.open
            ) {
                dialog.close()
            }
        },
        [
            confirmationAction,
        ]
    )

    function requestConfirmation(
        action: TenderDetailsAction,
    ): void {
        if (tender === null) {
            return
        }

        setConfirmationAction(action)
    }

    async function confirmTenderAction(): Promise<void> {
        if (
            tender === null
            || confirmationAction === null
        ) {
            return
        }

        const action =
            confirmationAction

        setConfirmationAction(null)

        if (action === 'PUBLISH') {
            await executeTenderTransition(
                action,
                () =>
                    publishProcurementTender(
                        tender.id
                    ),
                'Tender published successfully.'
            )

            return
        }

        if (action === 'CLOSE_BIDDING') {
            await executeTenderTransition(
                action,
                () =>
                    closeProcurementTenderBidding(
                        tender.id
                    ),
                'Tender bidding was closed.'
            )

            return
        }

        await executeTenderTransition(
            action,
            () =>
                startProcurementTenderEvaluation(
                    tender.id
                ),
            'Tender moved to evaluation.'
        )
    }

    function getConfirmationCopy():
        {
            title: string
            description: string
        } {
        if (confirmationAction === 'PUBLISH') {
            return {
                title: 'Publish this tender?',
                description:
                    'Publish '
                    + (tender?.tenderReference ?? 'this tender')
                    + '? The specification will be locked '
                    + 'with a SHA-256 evidence hash.',
            }
        }

        if (confirmationAction === 'CLOSE_BIDDING') {
            return {
                title: 'Close bidding?',
                description:
                    'Close bidding for '
                    + (tender?.tenderReference ?? 'this tender')
                    + '? Traders will no longer be able to '
                    + 'submit bids.',
            }
        }

        return {
            title: 'Start evaluation?',
            description:
                'Move '
                + (tender?.tenderReference ?? 'this tender')
                + ' into evaluation? Bid scoring must be '
                + 'completed by authorised evaluators.',
        }
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
        } catch (error) {
            setErrorMessage(
                getProcurementTenderApiErrorMessage(
                    error,
                    'Unable to complete the '
                    + 'tender workflow action.'
                )
            )

            await loadTender()
        } finally {
            setPendingAction(null)
        }
    }

    const isActionPending =
        pendingAction !== null

    return (
        <ProcurementLayout>
            <dialog
                ref={confirmationDialogRef}
                className="confirmation-dialog"
                aria-labelledby="tender-confirmation-title"
                aria-describedby="tender-confirmation-description"
                onClose={() => {
                    setConfirmationAction(null)
                }}
            >
                <div className="confirmation-dialog-content">
                    <span className="eyebrow">
                        Confirm workflow action
                    </span>

                    <h2 id="tender-confirmation-title">
                        {getConfirmationCopy().title}
                    </h2>

                    <p id="tender-confirmation-description">
                        {getConfirmationCopy().description}
                    </p>

                    <div className="form-actions">
                        <button
                            className="secondary-button"
                            type="button"
                            onClick={() => {
                                setConfirmationAction(null)
                            }}
                        >
                            Cancel
                        </button>

                        <button
                            className="primary-button"
                            type="button"
                            onClick={() => {
                                void confirmTenderAction()
                            }}
                        >
                            Confirm
                        </button>
                    </div>
                </div>
            </dialog>

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
                            not be loaded. Return to the
                            specification queue and
                            select another tender.
                        </p>
                    </div>
                </section>
            ) : (
                <>
                    <TenderDetailHeader
                        tender={tender}
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
                        pendingAction={
                            pendingAction
                        }
                        isActionPending={
                            isActionPending
                        }
                        onPublish={
                            () => {
                                requestConfirmation(
                                    'PUBLISH'
                                )
                            }
                        }
                        onVerify={
                            () => {
                                void handleVerifyIntegrity()
                            }
                        }
                        onCloseBidding={
                            () => {
                                requestConfirmation(
                                    'CLOSE_BIDDING'
                                )
                            }
                        }
                        onStartEvaluation={
                            () => {
                                requestConfirmation(
                                    'START_EVALUATION'
                                )
                            }
                        }
                        onRefresh={
                            () => {
                                void loadTender()
                            }
                        }
                    />
                </>
            )}
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

interface TenderWorkflowActionsProps {
    tender: ProcurementTender
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
                                   pendingAction,
                                   isActionPending,
                                   onPublish,
                                   onVerify,
                                   onCloseBidding,
                                   onStartEvaluation,
                                   onRefresh,
                               }: TenderWorkflowActionsProps) {
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
                    Evaluation and adjudication
                    decisions remain separated from
                    specification responsibilities.
                </p>
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
                        disabled={isActionPending}
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
                        disabled={isActionPending}
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