import {
    useEffect,
    useRef,
    useState,
} from 'react'

import {
    Link,
} from 'react-router-dom'

import {
    getMyBids,
} from '../api/bidApi'

import {
    getInAppNotifications,
} from '../api/notificationApi'

import {
    getMySupportTickets,
} from '../api/supportTicketApi'

import {
    getOpenTenders,
} from '../api/tenderApi'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import {
    NOTIFICATION_TEMPLATE_LABELS,
} from '../types/notification'

import type {
    Notification,
} from '../types/notification'

import type {
    TraderTender,
} from '../types/tender'

import {
    formatCurrency,
    formatDateTime,
} from '../utils/tenderFormatters'

interface DashboardStatistics {
    openTenders: number
    activeBids: number
    openSupportTickets: number
    unreadNotifications: number
}

interface DashboardLoadErrors {
    tenders?: string
    bids?: string
    supportTickets?: string
    notifications?: string
}

const initialStatistics: DashboardStatistics = {
    openTenders: 0,
    activeBids: 0,
    openSupportTickets: 0,
    unreadNotifications: 0,
}

function getNotificationDestination(
    notification: Notification
): {
    path: string
    label: string
} {
    if (
        notification.relatedTicketId !== null
    ) {
        return {
            path:
                '/support-tickets/'
                + notification.relatedTicketId,
            label: 'View support ticket',
        }
    }

    if (
        notification.relatedTenderId !== null
    ) {
        return {
            path:
                '/tenders/'
                + notification.relatedTenderId,
            label: 'View tender',
        }
    }

    if (
        notification.relatedBidId !== null
    ) {
        return {
            path: '/my-bids',
            label: 'View my bids',
        }
    }

    return {
        path: '/notifications',
        label: 'View notification',
    }
}

export function TraderDashboardPage() {
    const [statistics, setStatistics] =
        useState<DashboardStatistics>(
            initialStatistics
        )

    const [recentTenders, setRecentTenders] =
        useState<TraderTender[]>([])

    const [
        recentNotifications,
        setRecentNotifications,
    ] = useState<Notification[]>([])

    const [loadErrors, setLoadErrors] =
        useState<DashboardLoadErrors>({})

    const [isLoading, setIsLoading] =
        useState(true)

    const [isRefreshing, setIsRefreshing] =
        useState(false)

    const [refreshVersion, setRefreshVersion] =
        useState(0)

    const hasLoadedRef =
        useRef(false)

    useEffect(() => {
        let isActive = true

        async function loadDashboard() {
            const isRefresh =
                hasLoadedRef.current

            if (isRefresh) {
                setIsRefreshing(true)
            } else {
                setIsLoading(true)
            }

            setLoadErrors({})

            const [
                tenderResult,
                bidResult,
                ticketResult,
                notificationResult,
            ] = await Promise.allSettled([
                getOpenTenders({
                    page: 0,
                    size: 3,
                }),
                getMyBids(),
                getMySupportTickets({
                    page: 0,
                    size: 3,
                    sort: 'createdAt,desc',
                }),
                getInAppNotifications({
                    page: 0,
                    size: 3,
                    filter: 'unread',
                }),
            ])

            if (!isActive) {
                return
            }

            const nextErrors:
                DashboardLoadErrors = {}

            if (
                tenderResult.status === 'fulfilled'
            ) {
                setRecentTenders(
                    tenderResult.value.content
                )
            } else {
                nextErrors.tenders =
                    isRefresh
                        ? (
                            'Open tenders could not '
                            + 'be refreshed.'
                        )
                        : (
                            'Open tenders could not '
                            + 'be loaded.'
                        )

                if (!isRefresh) {
                    setRecentTenders([])
                }
            }

            if (
                bidResult.status !== 'fulfilled'
            ) {
                nextErrors.bids =
                    isRefresh
                        ? (
                            'Bid activity could not '
                            + 'be refreshed.'
                        )
                        : (
                            'Bid activity could not '
                            + 'be loaded.'
                        )
            }

            if (
                ticketResult.status !== 'fulfilled'
            ) {
                nextErrors.supportTickets =
                    isRefresh
                        ? (
                            'Support activity could not '
                            + 'be refreshed.'
                        )
                        : (
                            'Support activity could not '
                            + 'be loaded.'
                        )
            }

            if (
                notificationResult.status
                === 'fulfilled'
            ) {
                setRecentNotifications(
                    notificationResult.value.content
                )
            } else {
                nextErrors.notifications =
                    isRefresh
                        ? (
                            'Notifications could not '
                            + 'be refreshed.'
                        )
                        : (
                            'Notifications could not '
                            + 'be loaded.'
                        )

                if (!isRefresh) {
                    setRecentNotifications([])
                }
            }

            setStatistics(
                (currentStatistics) => ({
                    openTenders:
                        tenderResult.status
                        === 'fulfilled'
                            ? (
                                tenderResult.value
                                    .page.totalElements
                            )
                            : (
                                currentStatistics
                                    .openTenders
                            ),

                    activeBids:
                        bidResult.status
                        === 'fulfilled'
                            ? (
                                bidResult.value.filter(
                                    (bid) =>
                                        bid.status
                                        !== 'WITHDRAWN'
                                        && bid.status
                                        !== 'REJECTED'
                                        && bid.status
                                        !== 'AWARDED'
                                ).length
                            )
                            : (
                                currentStatistics
                                    .activeBids
                            ),

                    openSupportTickets:
                        ticketResult.status
                        === 'fulfilled'
                            ? (
                                ticketResult.value
                                    .content
                                    .filter(
                                        (ticket) =>
                                            ticket.status
                                            === 'OPEN'
                                            || ticket.status
                                            === 'IN_REVIEW'
                                            || ticket.status
                                            === 'RESPONDED'
                                    )
                                    .length
                            )
                            : (
                                currentStatistics
                                    .openSupportTickets
                            ),

                    unreadNotifications:
                        notificationResult.status
                        === 'fulfilled'
                            ? (
                                notificationResult
                                    .value
                                    .page
                                    .totalElements
                            )
                            : (
                                currentStatistics
                                    .unreadNotifications
                            ),
                })
            )

            setLoadErrors(nextErrors)

            hasLoadedRef.current = true

            setIsLoading(false)
            setIsRefreshing(false)
        }

        void loadDashboard()

        return () => {
            isActive = false
        }
    }, [refreshVersion])

    function handleRefresh() {
        if (isLoading || isRefreshing) {
            return
        }

        setRefreshVersion(
            (currentVersion) =>
                currentVersion + 1
        )
    }

    const dashboardStatistics = [
        {
            label: 'Open tenders',
            value: statistics.openTenders,
            description:
                'Available opportunities',
            path: '/tenders',
            error: loadErrors.tenders,
        },
        {
            label: 'Active bids',
            value: statistics.activeBids,
            description:
                'Submitted bids in progress',
            path: '/my-bids',
            error: loadErrors.bids,
        },
        {
            label: 'Support tickets',
            value:
            statistics.openSupportTickets,
            description:
                'Active support requests',
            path: '/support-tickets',
            error: loadErrors.supportTickets,
        },
        {
            label: 'Unread notifications',
            value:
            statistics.unreadNotifications,
            description:
                'Updates requiring attention',
            path: '/notifications',
            error: loadErrors.notifications,
        },
    ]

    return (
        <AppLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Trader overview
                    </span>

                    <h1>Dashboard</h1>

                    <p>
                        Track tenders, bids,
                        clarification notices, and
                        support activity from one
                        secure workspace.
                    </p>
                </div>

                <div className="form-actions">
                    <button
                        className="secondary-button"
                        type="button"
                        disabled={
                            isLoading
                            || isRefreshing
                        }
                        onClick={handleRefresh}
                    >
                        {isRefreshing
                            ? 'Refreshing...'
                            : 'Refresh dashboard'}
                    </button>

                    <Link
                        className="primary-link-button"
                        to="/tenders"
                    >
                        Browse open tenders
                    </Link>
                </div>
            </section>

            <div
                className="visually-hidden"
                role="status"
                aria-live="polite"
            >
                {isRefreshing
                    ? (
                        'Refreshing dashboard '
                        + 'information.'
                    )
                    : ''}
            </div>

            <section
                className="statistics-grid"
                aria-label="Trader activity summary"
                aria-busy={
                    isLoading || isRefreshing
                }
            >
                {dashboardStatistics.map(
                    (statistic) => (
                        <Link
                            className={
                                'statistic-card '
                                + 'dashboard-statistic-link'
                            }
                            key={statistic.label}
                            to={statistic.path}
                        >
                            <span>
                                {statistic.label}
                            </span>

                            <strong>
                                {isLoading
                                    ? '...'
                                    : statistic.value}
                            </strong>

                            <p>
                                {statistic.error
                                    ? statistic.error
                                    : statistic.description}
                            </p>
                        </Link>
                    )
                )}
            </section>

            <section className="dashboard-grid">
                <article className="content-card">
                    <div className="card-heading">
                        <div>
                            <span className="eyebrow">
                                Opportunities
                            </span>

                            <h2>
                                Recent open tenders
                            </h2>
                        </div>

                        <Link
                            className="text-button"
                            to="/tenders"
                        >
                            View all
                        </Link>
                    </div>

                    {isLoading ? (
                        <div className="empty-state">
                            <p>
                                Loading open tenders...
                            </p>
                        </div>
                    ) : (
                        <>
                            {loadErrors.tenders && (
                                <div
                                    className="error-message"
                                    role="alert"
                                >
                                    {loadErrors.tenders}
                                </div>
                            )}

                            {recentTenders.length === 0 ? (
                                <div className="empty-state">
                                    <h3>
                                        No open tenders
                                    </h3>

                                    <p>
                                        There are currently
                                        no published
                                        opportunities
                                        available for
                                        bidding.
                                    </p>
                                </div>
                            ) : (
                                <div className="dashboard-list">
                                    {recentTenders.map(
                                        (tender) => (
                                            <article
                                                className={
                                                    'dashboard-list-item'
                                                }
                                                key={
                                                    tender.id
                                                }
                                            >
                                                <div>
                                                    <span
                                                        className={
                                                            'tender-reference'
                                                        }
                                                    >
                                                        {
                                                            tender
                                                                .tenderReference
                                                        }
                                                    </span>

                                                    <h3>
                                                        {
                                                            tender
                                                                .title
                                                        }
                                                    </h3>

                                                    <p>
                                                        {formatCurrency(
                                                            tender
                                                                .budgetAmount
                                                        )}
                                                    </p>

                                                    <p
                                                        className={
                                                            'field-help'
                                                        }
                                                    >
                                                        Published{' '}
                                                        {formatDateTime(
                                                            tender
                                                                .publishedAt
                                                        )}
                                                    </p>
                                                </div>

                                                <Link
                                                    className={
                                                        'secondary-link-button'
                                                    }
                                                    to={
                                                        `/tenders/${tender.id}`
                                                    }
                                                >
                                                    View tender
                                                </Link>
                                            </article>
                                        )
                                    )}
                                </div>
                            )}
                        </>
                    )}
                </article>

                <article className="content-card">
                    <div className="card-heading">
                        <div>
                            <span className="eyebrow">
                                Communications
                            </span>

                            <h2>
                                Latest unread
                                notifications
                            </h2>
                        </div>

                        <Link
                            className="text-button"
                            to="/notifications"
                        >
                            View all
                        </Link>
                    </div>

                    {isLoading ? (
                        <div className="empty-state">
                            <p>
                                Loading notifications...
                            </p>
                        </div>
                    ) : (
                        <>
                            {loadErrors.notifications && (
                                <div
                                    className="error-message"
                                    role="alert"
                                >
                                    {
                                        loadErrors
                                            .notifications
                                    }
                                </div>
                            )}

                            {recentNotifications.length
                            === 0 ? (
                                <div className="empty-state">
                                    <h3>
                                        No unread
                                        notifications
                                    </h3>

                                    <p>
                                        You have reviewed
                                        all current
                                        updates.
                                    </p>
                                </div>
                            ) : (
                                <div className="dashboard-list">
                                    {recentNotifications.map(
                                        (notification) => {
                                            const destination =
                                                getNotificationDestination(
                                                    notification
                                                )

                                            return (
                                                <article
                                                    className={
                                                        'dashboard-list-item '
                                                        + 'dashboard-notification'
                                                    }
                                                    key={notification.id}
                                                >
                                                    <div>
                    <span className="eyebrow">
                        {
                            NOTIFICATION_TEMPLATE_LABELS[
                                notification
                                    .templateType
                                ]
                        }
                    </span>

                                                        <p>
                                                            {notification.message}
                                                        </p>

                                                        <p className="field-help">
                                                            {formatDateTime(
                                                                notification.createdAt
                                                            )}
                                                        </p>
                                                    </div>

                                                    <Link
                                                        className={
                                                            'secondary-link-button'
                                                        }
                                                        to={destination.path}
                                                        aria-label={
                                                            destination.label
                                                            + ': '
                                                            + NOTIFICATION_TEMPLATE_LABELS[
                                                                notification.templateType
                                                                ]
                                                        }
                                                    >
                                                        {destination.label}
                                                    </Link>
                                                </article>
                                            )
                                        }
                                    )}
                                </div>
                            )}
                        </>
                    )}
                </article>
            </section>
        </AppLayout>
    )
}