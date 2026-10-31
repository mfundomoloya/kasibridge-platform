import {
    useCallback,
    useEffect,
    useState,
} from 'react'

import {
    Link,
} from 'react-router-dom'

import {
    getFraudApiErrorMessage,
    getFraudAssessmentDashboard,
} from '../api/fraudAssessmentApi'

import {
    FraudMetricCard,
} from '../components/fraud/FraudMetricCard'

import {
    PortalLayout,
} from '../components/layout/PortalLayout'

import type {
    NavigationItem,
} from '../components/layout/PortalLayout'

import type {
    FraudAssessmentDashboard,
} from '../types/fraudAssessment'

import './FraudAssessmentDashboardPage.css'

const adminNavigation:
    NavigationItem[] = [
    {
        label: 'Administration',
        path: '/admin',
    },
    {
        label: 'Fraud assessments',
        path:
            '/procurement/fraud-assessments',
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Users and roles',
        path: '/admin/users',
    },
    {
        label: 'Committee assignments',
        path: '/admin/committees',
    },
    {
        label: 'Audit activity',
        path: '/admin/audit',
    },
    {
        label: 'System notifications',
        path: '/admin/notifications',
    },
]

export function AdminDashboardPage() {
    const [
        fraudDashboard,
        setFraudDashboard,
    ] =
        useState<
            FraudAssessmentDashboard | null
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
            null
        )

    const loadDashboard =
        useCallback(
            async (): Promise<void> => {
                setIsLoading(true)
                setErrorMessage(null)

                try {
                    const dashboard =
                        await getFraudAssessmentDashboard()

                    setFraudDashboard(
                        dashboard
                    )
                } catch (error) {
                    setErrorMessage(
                        getFraudApiErrorMessage(
                            error,
                            'Unable to load '
                            + 'administrative '
                            + 'oversight metrics.'
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
                    void loadDashboard()
                }
            )
        },
        [
            loadDashboard,
        ]
    )

    return (
        <PortalLayout
            portalName={
                'Administration Dashboard'
            }
            navigationItems={
                adminNavigation
            }
        >
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Platform administration
                    </span>

                    <h1>
                        Administration Dashboard
                    </h1>

                    <p>
                        Monitor platform integrity,
                        fraud-review workload, access
                        controls, committee activity,
                        and operational oversight.
                    </p>
                </div>

                <button
                    className="secondary-button"
                    type="button"
                    onClick={
                        () => {
                            void loadDashboard()
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
                className="fraud-metric-grid"
                aria-label={
                    'Administrative fraud oversight'
                }
            >
                <FraudMetricCard
                    label="Total assessments"
                    value={
                        fraudDashboard
                            ?.totalAssessments
                        ?? 0
                    }
                    description={
                        'Persisted procurement '
                        + 'invoice assessments.'
                    }
                    tone="info"
                    isLoading={isLoading}
                    footer={
                        fraudDashboard
                            ?.latestAssessedAt
                            ? (
                                <span>
                                Latest assessment:
                                    {' '}
                                    {
                                        formatDateTime(
                                            fraudDashboard
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
                        fraudDashboard
                            ?.pendingReview
                        ?? 0
                    }
                    description={
                        'Assessments awaiting an '
                        + 'accountable analyst '
                        + 'decision.'
                    }
                    tone={
                        fraudDashboard
                        && fraudDashboard
                            .pendingReview > 0
                            ? 'warning'
                            : 'positive'
                    }
                    isLoading={isLoading}
                    footer={
                        getPendingAgeDescription(
                            fraudDashboard
                        )
                    }
                />

                <FraudMetricCard
                    label="Open high-risk work"
                    value={
                        fraudDashboard
                            ?.openHighOrCriticalRisk
                        ?? 0
                    }
                    description={
                        'High or critical cases '
                        + 'that remain pending or '
                        + 'escalated.'
                    }
                    tone={
                        fraudDashboard
                        && fraudDashboard
                            .openHighOrCriticalRisk
                        > 0
                            ? 'danger'
                            : 'positive'
                    }
                    isLoading={isLoading}
                />

                <FraudMetricCard
                    label="Escalated"
                    value={
                        fraudDashboard
                            ?.escalated
                        ?? 0
                    }
                    description={
                        'Cases awaiting senior '
                        + 'investigation or further '
                        + 'evidence.'
                    }
                    tone={
                        fraudDashboard
                        && fraudDashboard
                            .escalated > 0
                            ? 'warning'
                            : 'default'
                    }
                    isLoading={isLoading}
                />

                <FraudMetricCard
                    label="Confirmed fraud"
                    value={
                        fraudDashboard
                            ?.confirmedFraud
                        ?? 0
                    }
                    description={
                        'Assessments confirmed as '
                        + 'fraud after authorised '
                        + 'review.'
                    }
                    tone={
                        fraudDashboard
                        && fraudDashboard
                            .confirmedFraud > 0
                            ? 'danger'
                            : 'default'
                    }
                    isLoading={isLoading}
                />

                <FraudMetricCard
                    label="False positives"
                    value={
                        fraudDashboard
                            ?.falsePositive
                        ?? 0
                    }
                    description={
                        'Assessments cleared through '
                        + 'documented analyst review.'
                    }
                    tone="info"
                    isLoading={isLoading}
                />

                <FraudMetricCard
                    label="Critical risk"
                    value={
                        fraudDashboard
                            ?.criticalRisk
                        ?? 0
                    }
                    description={
                        'Assessments at the highest '
                        + 'operational risk level.'
                    }
                    tone={
                        fraudDashboard
                        && fraudDashboard
                            .criticalRisk > 0
                            ? 'danger'
                            : 'positive'
                    }
                    isLoading={isLoading}
                />

                <FraudMetricCard
                    label="High risk"
                    value={
                        fraudDashboard
                            ?.highRisk
                        ?? 0
                    }
                    description={
                        'Assessments classified as '
                        + 'high risk by the active '
                        + 'model.'
                    }
                    tone={
                        fraudDashboard
                        && fraudDashboard
                            .highRisk > 0
                            ? 'warning'
                            : 'default'
                    }
                    isLoading={isLoading}
                />
            </section>

            <section className="dashboard-grid">
                <article className="content-card">
                    <span className="eyebrow">
                        Integrity oversight
                    </span>

                    <h2>
                        Fraud assessment workflow
                    </h2>

                    <p className="tender-detail-text">
                        Review high-risk invoice
                        assessments, inspect immutable
                        evidence hashes, escalate cases,
                        and monitor analyst decisions.
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
                            Open fraud dashboard
                        </Link>
                    </div>
                </article>

                <article className="content-card">
                    <span className="eyebrow">
                        Segregation of duties
                    </span>

                    <h2>
                        Administrative escalation
                    </h2>

                    <p className="tender-detail-text">
                        Platform administrators can
                        escalate pending assessments
                        for senior investigation.
                        Final confirmation and
                        false-positive decisions remain
                        recorded against authenticated
                        users with optimistic-lock
                        protection.
                    </p>
                </article>
            </section>
        </PortalLayout>
    )
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