import {
    useEffect,
    useState,
} from 'react'

import type {
    ReactNode,
} from 'react'

import {
    getInAppNotifications,
} from '../../api/notificationApi'

import {
    NOTIFICATION_READ_STATE_CHANGED,
} from '../../utils/notificationEvents'

import {
    PortalLayout,
} from './PortalLayout'

import type {
    NavigationItem,
} from './PortalLayout'

interface AppLayoutProps {
    children: ReactNode
}

export function AppLayout({
                              children,
                          }: AppLayoutProps) {
    const [unreadNotificationCount,
        setUnreadNotificationCount] =
        useState(0)

    useEffect(() => {
        let isActive = true

        async function loadUnreadCount() {
            try {
                const notificationPage =
                    await getInAppNotifications({
                        page: 0,
                        size: 1,
                        filter: 'unread',
                    })

                if (!isActive) {
                    return
                }

                setUnreadNotificationCount(
                    notificationPage
                        .page
                        .totalElements
                )
            } catch {
                if (!isActive) {
                    return
                }

                /*
                 * Keep the last successful count if the
                 * procurement service is temporarily
                 * unavailable.
                 */
            }
        }

        function handleNotificationChange() {
            void loadUnreadCount()
        }

        void loadUnreadCount()

        window.addEventListener(
            NOTIFICATION_READ_STATE_CHANGED,
            handleNotificationChange
        )

        return () => {
            isActive = false

            window.removeEventListener(
                NOTIFICATION_READ_STATE_CHANGED,
                handleNotificationChange
            )
        }
    }, [])

    const traderNavigation:
        NavigationItem[] = [
        {
            label: 'Dashboard',
            path: '/dashboard',
        },
        {
            label: 'Open tenders',
            path: '/tenders',
        },
        {
            label: 'My bids',
            path: '/my-bids',
        },
        {
            label: 'Support tickets',
            path: '/support-tickets',
        },
        {
            label: 'Notifications',
            path: '/notifications',
            badge: unreadNotificationCount,
        },
        {
            label: 'Official clarifications',
            path: '/clarifications',
        },
        {
            label: 'Business profile',
            path: '/profile',
        },
    ]

    return (
        <PortalLayout
            portalName="Trader Web Dashboard"
            navigationItems={traderNavigation}
        >
            {children}
        </PortalLayout>
    )
}