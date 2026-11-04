import type {
    ReactNode,
} from 'react'

import {
    PortalLayout,
} from './PortalLayout'

import type {
    NavigationItem,
} from './PortalLayout'

const administrationNavigation:
    NavigationItem[] = [
    {
        label: 'Administration',
        path: '/admin',
        end: true,
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
        label: 'Unassigned support tickets',
        path:
            '/procurement/support-tickets/unassigned',
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Users and roles',
        path: '/admin/users',
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Committee assignments',
        path: '/admin/committees',
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Audit activity',
        path: '/admin/audit',
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'System notifications',
        path: '/admin/notifications',
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
]

interface AdministrationLayoutProps {
    children: ReactNode
}

export function AdministrationLayout({
                                         children,
                                     }: AdministrationLayoutProps) {
    return (
        <PortalLayout
            portalName={
                'Administration Dashboard'
            }
            navigationItems={
                administrationNavigation
            }
        >
            {children}
        </PortalLayout>
    )
}