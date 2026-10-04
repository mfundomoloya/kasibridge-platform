import type { ReactNode } from 'react'

import {PortalLayout,} from './PortalLayout'

interface AppLayoutProps {
    children: ReactNode
}

const traderNavigation = [
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
        path: '/bids',
    },
    {
        label: 'Support tickets',
        path: '/support-tickets',
    },
    {
        label: 'Notifications',
        path: '/notifications',
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

export function AppLayout({
                              children,
                          }: AppLayoutProps) {
    return (
        <PortalLayout
            portalName="Trader Web Dashboard"
            navigationItems={traderNavigation}
        >
            {children}
        </PortalLayout>
    )
}