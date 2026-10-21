import {
    useEffect,
    useRef,
    useState,
} from 'react'

import {
    useSearchParams,
} from 'react-router-dom'

import {
    getInAppNotifications,
    getNotificationApiError,
    markNotificationRead,
} from '../api/notificationApi'

import type {
    NotificationReadFilter,
} from '../api/notificationApi'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import {
    NotificationCard,
} from '../components/notifications/NotificationCard'

import type {
    Notification,
} from '../types/notification'

import {
    notifyNotificationReadStateChanged,
} from '../utils/notificationEvents'

const notificationFilters: Array<{
    label: string
    value: NotificationReadFilter
}> = [
    {
        label: 'All',
        value: 'all',
    },
    {
        label: 'Unread',
        value: 'unread',
    },
    {
        label: 'Read',
        value: 'read',
    },
]

export function NotificationsPage() {
    const [notifications, setNotifications] =
        useState<Notification[]>([])

    const [selectedFilter, setSelectedFilter] =
        useState<NotificationReadFilter>('all')

    const [currentPage, setCurrentPage] =
        useState(0)

    const [totalPages, setTotalPages] =
        useState(0)

    const [totalElements, setTotalElements] =
        useState(0)

    const [markingReadId, setMarkingReadId] =
        useState<number | null>(null)

    const [isLoading, setIsLoading] =
        useState(true)

    const [errorMessage, setErrorMessage] =
        useState('')

    const [searchParams] =
        useSearchParams()

    const selectedNotificationIdValue =
        searchParams.get('notificationId')

    const selectedNotificationId =
        selectedNotificationIdValue !== null
            ? Number(selectedNotificationIdValue)
            : null

    const validSelectedNotificationId =
        selectedNotificationId !== null
        && Number.isSafeInteger(
            selectedNotificationId
        )
        && selectedNotificationId > 0
            ? selectedNotificationId
            : null

    const lastScrolledNotificationId =
        useRef<number | null>(null)

    useEffect(() => {
        let isActive = true

        async function loadNotifications() {
            setIsLoading(true)
            setErrorMessage('')

            try {
                const notificationPage =
                    await getInAppNotifications({
                        page: currentPage,
                        size: 10,
                        filter: selectedFilter,
                    })

                if (!isActive) {
                    return
                }

                setNotifications(
                    notificationPage.content
                )

                setTotalPages(
                    notificationPage.page.totalPages
                )

                setTotalElements(
                    notificationPage.page.totalElements
                )
            } catch (error) {
                if (!isActive) {
                    return
                }

                const apiError =
                    getNotificationApiError(error)

                setNotifications([])
                setTotalPages(0)
                setTotalElements(0)
                setErrorMessage(
                    apiError.message
                )
            } finally {
                if (isActive) {
                    setIsLoading(false)
                }
            }
        }

        void loadNotifications()

        return () => {
            isActive = false
        }
    }, [
        currentPage,
        selectedFilter,
    ])

    useEffect(() => {
        if (
            isLoading
            || validSelectedNotificationId === null
            || lastScrolledNotificationId.current
                === validSelectedNotificationId
        ) {
            return
        }

        const selectedNotificationExists =
            notifications.some(
                (notification) =>
                    notification.id
                    === validSelectedNotificationId
            )

        if (!selectedNotificationExists) {
            return
        }

        const animationFrame =
            window.requestAnimationFrame(() => {
                const notificationElement =
                    document.getElementById(
                        `notification-${validSelectedNotificationId}`
                    )

                if (!notificationElement) {
                    return
                }

                notificationElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'center',
                })

                lastScrolledNotificationId.current =
                    validSelectedNotificationId
            })

        return () => {
            window.cancelAnimationFrame(
                animationFrame
            )
        }
    }, [
        isLoading,
        notifications,
        validSelectedNotificationId,
    ])

    function handleFilterChange(
        filter: NotificationReadFilter
    ) {
        setSelectedFilter(filter)
        setCurrentPage(0)
        setErrorMessage('')
    }

    async function handleMarkRead(
        notification: Notification
    ) {
        if (
            notification.read
            || markingReadId !== null
        ) {
            return
        }

        setMarkingReadId(notification.id)
        setErrorMessage('')

        try {
            const updatedNotification =
                await markNotificationRead(
                    notification.id
                )

            notifyNotificationReadStateChanged()

            if (selectedFilter === 'unread') {
                setNotifications(
                    (currentNotifications) =>
                        currentNotifications.filter(
                            (currentNotification) =>
                                currentNotification.id
                                !== updatedNotification.id
                        )
                )

                setTotalElements(
                    (currentTotal) =>
                        Math.max(
                            0,
                            currentTotal - 1
                        )
                )
            } else {
                setNotifications(
                    (currentNotifications) =>
                        currentNotifications.map(
                            (currentNotification) =>
                                currentNotification.id
                                === updatedNotification.id
                                    ? updatedNotification
                                    : currentNotification
                        )
                )
            }
        } catch (error) {
            const apiError =
                getNotificationApiError(error)

            setErrorMessage(apiError.message)
        } finally {
            setMarkingReadId(null)
        }
    }

    function goToPreviousPage() {
        setCurrentPage((page) =>
            Math.max(0, page - 1)
        )
    }

    function goToNextPage() {
        setCurrentPage((page) =>
            Math.min(
                totalPages - 1,
                page + 1
            )
        )
    }

    return (
        <AppLayout>
            <section className="page-header">
                <span className="eyebrow">
                    Trader portal
                </span>

                <h1>Notifications</h1>

                <p>
                    Review procurement updates,
                    support-ticket responses, bid
                    decisions, and official
                    clarification notices.
                </p>
            </section>

            <section
                className="notification-toolbar"
                aria-label="Notification filters"
            >
                <div
                    className="notification-filter-group"
                    role="group"
                    aria-label="Filter notifications"
                >
                    {notificationFilters.map(
                        (filter) => (
                            <button
                                key={filter.value}
                                className={
                                    'notification-filter-button '
                                    + (
                                        selectedFilter
                                        === filter.value
                                            ? 'is-active'
                                            : ''
                                    )
                                }
                                type="button"
                                aria-pressed={
                                    selectedFilter
                                    === filter.value
                                }
                                disabled={isLoading}
                                onClick={() => {
                                    handleFilterChange(
                                        filter.value
                                    )
                                }}
                            >
                                {filter.label}
                            </button>
                        )
                    )}
                </div>

                <span className="notification-count">
                    {totalElements}{' '}
                    {totalElements === 1
                        ? 'notification'
                        : 'notifications'}
                </span>
            </section>

            {errorMessage && (
                <div
                    className="error-message"
                    role="alert"
                >
                    {errorMessage}
                </div>
            )}

            {isLoading ? (
                <section className="content-card">
                    <p>
                        Loading notifications...
                    </p>
                </section>
            ) : notifications.length === 0 ? (
                <section className="content-card">
                    <span className="eyebrow">
                        Notification activity
                    </span>

                    <h2>
                        {selectedFilter === 'unread'
                            ? 'No unread notifications'
                            : selectedFilter === 'read'
                                ? 'No read notifications'
                                : 'No notifications'}
                    </h2>

                    <p>
                        {selectedFilter === 'unread'
                            ? (
                                'You have reviewed all '
                                + 'your notifications.'
                            )
                            : selectedFilter === 'read'
                                ? (
                                    'You have not marked '
                                    + 'any notifications '
                                    + 'as read.'
                                )
                                : (
                                    'No in-app notifications '
                                    + 'are available yet.'
                                )}
                    </p>
                </section>
            ) : (
                <>
                    <section
                        className="notification-list"
                        aria-label="In-app notifications"
                    >
                        {notifications.map(
                            (notification) => (
                                <NotificationCard
                                    key={notification.id}
                                    notification={
                                        notification
                                    }
                                    isMarkingRead={
                                        markingReadId
                                        === notification.id
                                    }
                                    isHighlighted={
                                        validSelectedNotificationId
                                        === notification.id
                                    }
                                    onMarkRead={
                                        handleMarkRead
                                    }
                                />
                            )
                        )}
                    </section>

                    {totalPages > 1 && (
                        <nav
                            className="pagination-controls"
                            aria-label={
                                'Notification pagination'
                            }
                        >
                            <button
                                className="secondary-button"
                                type="button"
                                disabled={
                                    currentPage === 0
                                    || isLoading
                                }
                                onClick={
                                    goToPreviousPage
                                }
                            >
                                Previous
                            </button>

                            <span>
                                Page {currentPage + 1}
                                {' '}of {totalPages}
                            </span>

                            <button
                                className="secondary-button"
                                type="button"
                                disabled={
                                    currentPage
                                    >= totalPages - 1
                                    || isLoading
                                }
                                onClick={
                                    goToNextPage
                                }
                            >
                                Next
                            </button>
                        </nav>
                    )}
                </>
            )}
        </AppLayout>
    )
}