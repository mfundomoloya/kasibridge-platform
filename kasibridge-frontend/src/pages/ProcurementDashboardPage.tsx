import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from 'react'

import {
    Link,
} from 'react-router-dom'

import {
    getProcurementTenderApiErrorMessage,
    getProcurementTenders,
} from '../api/procurementTenderApi'

import {
    useAuth,
} from '../auth/useAuth'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

import type {
    ProcurementTender,
} from '../types/procurementTender'

import {
    calculateTenderStatistics,
} from '../types/procurementTender'

import './SpecificationTenderPage.css'

const dashboardTenderLimit = 100

export function ProcurementDashboardPage() {
    const {
        user,
        hasRole,
    } = useAuth()

    const isSpecificationOfficer =
        hasRole(
            'ROLE_SPECIFICATION_OFFICER'
        )

    const isEvaluator =
        hasRole(
            'ROLE_EVALUATOR'
        )

    const isAdjudicator =
        hasRole(
            'ROLE_ADJUDICATOR'
        )

    const isAnalyst =
        hasRole(
            'ROLE_ANALYST'
        )

    if (isSpecificationOfficer) {
        return (
            <SpecificationOfficerDashboard />
        )
    }

    if (isEvaluator) {
        return (
            <EvaluatorDashboard
                username={
                    user?.username
                    ?? 'Evaluator'
                }
            />
        )
    }

    if (isAdjudicator) {
        return (
            <AdjudicatorDashboard
                username={
                    user?.username
                    ?? 'Adjudicator'
                }
            />
        )
    }

    if (isAnalyst) {
        return (
            <IntegrityAnalystDashboard />
        )
    }

    return (
        <ProcurementLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Controlled procurement workflow
                    </span>

                    <h1>
                        Procurement overview
                    </h1>

                    <p>
                        No procurement workflow has
                        been assigned to the current
                        account.
                    </p>
                </div>
            </section>

            <section className="content-card">
                <div className="empty-state">
                    <h2>
                        No procurement role available
                    </h2>

                    <p>
                        Contact a platform
                        administrator if this account
                        requires access to a
                        procurement workflow.
                    </p>
                </div>
            </section>
        </ProcurementLayout>
    )
}

function SpecificationOfficerDashboard() {
    const [
        tenders,
        setTenders,
    ] =
        useState<ProcurementTender[]>(
            []
        )

    const [
        isLoading,
        setIsLoading,
    ] = useState(true)

    const [
        errorMessage,
        setErrorMessage,
    ] =
        useState<string | null>(
            null
        )

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
                            dashboardTenderLimit,
                            sort:
                                'updatedAt,desc',
                        })

                    setTenders(
                        response.content
                    )
                } catch (error) {
                    setErrorMessage(
                        getProcurementTenderApiErrorMessage(
                            error,
                            'Unable to load the '
                            + 'Specification '
                            + 'Officer dashboard.'
                        )
                    )
                } finally {
                    setIsLoading(false)
                }
            },
            []
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

    const recentTenders =
        useMemo(
            () =>
                tenders.slice(
                    0,
                    5
                ),
            [
                tenders,
            ]
        )

    const tendersRequiringAction =
        useMemo(
            () =>
                tenders.filter(
                    (tender) =>
                        tender.status === 'DRAFT'
                        || tender.status
                        === 'BIDDING_CLOSED'
                )
                    .slice(
                        0,
                        5
                    ),
            [
                tenders,
            ]
        )

    return (
        <ProcurementLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Specification Officer workflow
                    </span>

                    <h1>
                        Procurement overview
                    </h1>

                    <p>
                        Create and publish tender
                        specifications, protect the
                        published scoring basis, close
                        bidding, and hand completed
                        tenders to the Evaluation
                        Committee.
                    </p>
                </div>

                <button
                    className="secondary-button"
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
                        : 'Refresh dashboard'}
                </button>
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

            <section
                className={
                    'statistics-grid '
                    + 'specification-statistics-grid'
                }
                aria-label={
                    'Specification Officer dashboard statistics'
                }
            >
                <DashboardStatistic
                    label="Total tenders"
                    value={
                        isLoading
                            ? '...'
                            : statistics.total
                    }
                    description={
                        'Internal procurement '
                        + 'tenders across all states.'
                    }
                />

                <DashboardStatistic
                    label="Draft specifications"
                    value={
                        isLoading
                            ? '...'
                            : statistics.draft
                    }
                    description={
                        'Specifications awaiting '
                        + 'review and publication.'
                    }
                    tone={
                        statistics.draft > 0
                            ? 'warning'
                            : 'default'
                    }
                />

                <DashboardStatistic
                    label="Published tenders"
                    value={
                        isLoading
                            ? '...'
                            : statistics.published
                    }
                    description={
                        'Opportunities currently '
                        + 'open to trader bids.'
                    }
                    tone="positive"
                />

                <DashboardStatistic
                    label="Bidding closed"
                    value={
                        isLoading
                            ? '...'
                            : statistics
                                .biddingClosed
                    }
                    description={
                        'Tenders ready to enter '
                        + 'blind evaluation.'
                    }
                    tone={
                        statistics
                            .biddingClosed > 0
                            ? 'warning'
                            : 'default'
                    }
                />

                <DashboardStatistic
                    label="In evaluation"
                    value={
                        isLoading
                            ? '...'
                            : statistics.evaluation
                    }
                    description={
                        'Tenders transferred to '
                        + 'the Evaluation Committee.'
                    }
                    tone="info"
                />

                <DashboardStatistic
                    label="In adjudication"
                    value={
                        isLoading
                            ? '...'
                            : statistics.adjudication
                    }
                    description={
                        'Evaluation outcomes under '
                        + 'adjudication review.'
                    }
                    tone="info"
                />

                <DashboardStatistic
                    label="Awarded"
                    value={
                        isLoading
                            ? '...'
                            : statistics.awarded
                    }
                    description={
                        'Completed procurement '
                        + 'award decisions.'
                    }
                    tone="positive"
                />

                <DashboardStatistic
                    label="Cancelled"
                    value={
                        isLoading
                            ? '...'
                            : statistics.cancelled
                    }
                    description={
                        'Tenders removed from the '
                        + 'active workflow.'
                    }
                    tone={
                        statistics.cancelled > 0
                            ? 'danger'
                            : 'default'
                    }
                />
            </section>

            <section className="dashboard-grid">
                <article className="content-card">
                    <div className="card-heading">
                        <div>
                            <span className="eyebrow">
                                Workflow actions
                            </span>

                            <h2>
                                Specification management
                            </h2>
                        </div>
                    </div>

                    <p className="tender-detail-text">
                        Create a new draft or open the
                        tender queue to publish,
                        integrity-check, close, and
                        transfer specifications into
                        evaluation.
                    </p>

                    <div
                        className={
                            'specification-dashboard-actions'
                        }
                    >
                        <Link
                            className={
                                'primary-link-button'
                            }
                            to={
                                '/procurement/'
                                + 'specifications/new'
                            }
                        >
                            Create specification
                        </Link>

                        <Link
                            className={
                                'secondary-link-button'
                            }
                            to={
                                '/procurement/'
                                + 'specifications'
                            }
                        >
                            Open tender queue
                        </Link>
                    </div>
                </article>

                <article className="content-card">
                    <div className="card-heading">
                        <div>
                            <span className="eyebrow">
                                Support and clarification
                            </span>

                            <h2>
                                Procurement support
                            </h2>
                        </div>
                    </div>

                    <p className="tender-detail-text">
                        Review assigned trader support
                        requests and AI-assisted
                        response assessments without
                        exposing internal committee
                        information.
                    </p>

                    <div
                        className={
                            'specification-dashboard-actions'
                        }
                    >
                        <Link
                            className={
                                'primary-link-button'
                            }
                            to={
                                '/procurement/'
                                + 'support-tickets'
                            }
                        >
                            Open support queue
                        </Link>

                        <Link
                            className={
                                'secondary-link-button'
                            }
                            to={
                                '/procurement/'
                                + 'ai-assessments'
                            }
                        >
                            Review AI assessments
                        </Link>
                    </div>
                </article>
            </section>

            <section className="dashboard-grid">
                <article className="content-card">
                    <div className="card-heading">
                        <div>
                            <span className="eyebrow">
                                Action required
                            </span>

                            <h2>
                                Tender workflow queue
                            </h2>
                        </div>

                        <Link
                            className="text-button"
                            to={
                                '/procurement/'
                                + 'specifications'
                            }
                        >
                            View all
                        </Link>
                    </div>

                    <TenderDashboardList
                        tenders={
                            tendersRequiringAction
                        }
                        isLoading={isLoading}
                        emptyTitle={
                            'No immediate actions'
                        }
                        emptyDescription={
                            'There are no draft '
                            + 'specifications or '
                            + 'closed tenders awaiting '
                            + 'evaluation handover.'
                        }
                    />
                </article>

                <article className="content-card">
                    <div className="card-heading">
                        <div>
                            <span className="eyebrow">
                                Recent activity
                            </span>

                            <h2>
                                Recently updated tenders
                            </h2>
                        </div>

                        <Link
                            className="text-button"
                            to={
                                '/procurement/'
                                + 'specifications'
                            }
                        >
                            Open queue
                        </Link>
                    </div>

                    <TenderDashboardList
                        tenders={recentTenders}
                        isLoading={isLoading}
                        emptyTitle={
                            'No tenders created'
                        }
                        emptyDescription={
                            'Create the first tender '
                            + 'specification to begin '
                            + 'the controlled workflow.'
                        }
                    />
                </article>
            </section>
        </ProcurementLayout>
    )
}

interface EvaluatorDashboardProps {
    username: string
}

function EvaluatorDashboard({
                                username,
                            }: EvaluatorDashboardProps) {
    return (
        <ProcurementLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Bid Evaluation Committee
                    </span>

                    <h1>
                        Evaluation dashboard
                    </h1>

                    <p>
                        Signed in as {username}. Review
                        only assigned tenders, preserve
                        bidder anonymity, and score
                        bids against the published
                        evaluation criteria.
                    </p>
                </div>
            </section>

            <section className="dashboard-grid">
                <article className="content-card">
        <span className="eyebrow">
            Assignment control
        </span>

                    <h2>
                        Assigned evaluations
                    </h2>

                    <p className="tender-detail-text">
                        Open the authenticated evaluator’s
                        committee-assignment queue. Evaluation
                        workspaces become available only when an
                        assigned tender is in the Evaluation
                        stage.
                    </p>

                    <div className="form-actions">
                        <Link
                            className="primary-link-button"
                            to="/procurement/evaluations"
                        >
                            Open evaluation queue
                        </Link>
                    </div>
                </article>

                <article className="content-card">
        <span className="eyebrow">
            Segregation of duties
        </span>

                    <h2>
                        Evaluation responsibilities
                    </h2>

                    <ul className="procurement-role-list">
                        <li>
                            Review only assigned tenders.
                        </li>

                        <li>
                            Review anonymized bids without
                            trader identities.
                        </li>

                        <li>
                            Apply the published evaluation
                            criteria.
                        </li>

                        <li>
                            Record evidence-based technical
                            scores and comments.
                        </li>

                        <li>
                            Do not publish specifications,
                            adjudicate recommendations, or
                            award tenders.
                        </li>
                    </ul>
                </article>
            </section>
        </ProcurementLayout>
    )
}

interface AdjudicatorDashboardProps {
    username: string
}

function AdjudicatorDashboard({
                                  username,
                              }: AdjudicatorDashboardProps) {
    return (
        <ProcurementLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Bid Adjudication Committee
                    </span>

                    <h1>
                        Adjudication dashboard
                    </h1>

                    <p>
                        Signed in as {username}. Review
                        evaluation rankings, supporting
                        records, integrity alerts, and
                        award reasons before recording
                        an adjudication decision.
                    </p>
                </div>
            </section>

            <section className="dashboard-grid">
                <article className="content-card">
                    <span className="eyebrow">
                        Decision queue
                    </span>

                    <h2>
                        Assigned adjudications
                    </h2>

                    <p className="tender-detail-text">
                        This queue will display tenders
                        assigned to the signed-in
                        Adjudication Committee member.
                    </p>

                    <div className="empty-state">
                        <h3>
                            Assignment queue required
                        </h3>

                        <p>
                            Ranking and award APIs are
                            available, but the backend
                            does not yet expose the
                            current adjudicator’s
                            committee assignments.
                        </p>
                    </div>
                </article>

                <article className="content-card">
                    <span className="eyebrow">
                        Support workflow
                    </span>

                    <h2>
                        Assigned support tickets
                    </h2>

                    <p className="tender-detail-text">
                        Review assigned procurement
                        support activities without
                        changing evaluator scoring or
                        tender specifications.
                    </p>

                    <div className="form-actions">
                        <Link
                            className={
                                'primary-link-button'
                            }
                            to={
                                '/procurement/'
                                + 'support-tickets'
                            }
                        >
                            Open support queue
                        </Link>
                    </div>
                </article>
            </section>
        </ProcurementLayout>
    )
}

function IntegrityAnalystDashboard() {
    return (
        <ProcurementLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Procurement integrity
                    </span>

                    <h1>
                        Integrity analyst dashboard
                    </h1>

                    <p>
                        Review explainable invoice-risk
                        assessments and record
                        accountable fraud-review
                        decisions.
                    </p>
                </div>
            </section>

            <section className="dashboard-grid">
                <article className="content-card">
                    <span className="eyebrow">
                        Fraud assessment workflow
                    </span>

                    <h2>
                        Invoice-risk assessment queue
                    </h2>

                    <p className="tender-detail-text">
                        Inspect model results, evidence
                        hashes, risk probabilities,
                        review status, and immutable
                        decision history.
                    </p>

                    <div className="form-actions">
                        <Link
                            className={
                                'primary-link-button'
                            }
                            to={
                                '/procurement/'
                                + 'fraud-assessments'
                            }
                        >
                            Open fraud assessments
                        </Link>
                    </div>
                </article>

                <article className="content-card">
                    <span className="eyebrow">
                        Role boundary
                    </span>

                    <h2>
                        Integrity review only
                    </h2>

                    <p className="tender-detail-text">
                        Analysts can review fraud
                        assessments but cannot create
                        specifications, score bids, or
                        adjudicate tender awards.
                    </p>
                </article>
            </section>
        </ProcurementLayout>
    )
}

interface DashboardStatisticProps {
    label: string
    value: number | string
    description: string
    tone?:
        | 'default'
        | 'positive'
        | 'warning'
        | 'danger'
        | 'info'
}

function DashboardStatistic({
                                label,
                                value,
                                description,
                                tone = 'default',
                            }: DashboardStatisticProps) {
    return (
        <article
            className={
                'statistic-card '
                + 'procurement-dashboard-stat '
                + `procurement-stat-${tone}`
            }
        >
            <span>{label}</span>

            <strong>{value}</strong>

            <p>{description}</p>
        </article>
    )
}

interface TenderDashboardListProps {
    tenders: ProcurementTender[]
    isLoading: boolean
    emptyTitle: string
    emptyDescription: string
}

function TenderDashboardList({
                                 tenders,
                                 isLoading,
                                 emptyTitle,
                                 emptyDescription,
                             }: TenderDashboardListProps) {
    if (isLoading) {
        return (
            <div
                className="empty-state"
                aria-busy="true"
            >
                <h3>
                    Loading tenders
                </h3>

                <p>
                    Retrieving current procurement
                    workflow records.
                </p>
            </div>
        )
    }

    if (tenders.length === 0) {
        return (
            <div className="empty-state">
                <h3>{emptyTitle}</h3>
                <p>{emptyDescription}</p>
            </div>
        )
    }

    return (
        <div className="dashboard-list">
            {tenders.map(
                (tender) => (
                    <article
                        className={
                            'dashboard-list-item'
                        }
                        key={tender.id}
                    >
                        <div>
                            <span className="eyebrow">
                                {
                                    tender
                                        .tenderReference
                                }
                            </span>

                            <h3>
                                {tender.title}
                            </h3>

                            <p>
                                {
                                    formatTenderStatus(
                                        tender.status
                                    )
                                }
                                {' · '}
                                {
                                    formatDateTime(
                                        tender.updatedAt
                                    )
                                }
                            </p>
                        </div>

                        <Link
                            className={
                                'secondary-link-button'
                            }
                            to={
                                '/procurement/'
                                + 'specifications/'
                                + tender.id
                            }
                        >
                            Open tender
                        </Link>
                    </article>
                )
            )}
        </div>
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
        return 'Date unavailable'
    }

    return new Intl.DateTimeFormat(
        'en-ZA',
        {
            dateStyle: 'medium',
            timeStyle: 'short',
        }
    ).format(parsedDate)
}