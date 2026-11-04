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
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Fraud assessments',
        path:
            '/procurement/fraud-assessments',
        end: true,
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label:
            'Unassigned support tickets',
        path:
            '/procurement/'
            + 'support-tickets/'
            + 'unassigned',
        end: true,
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Users and roles',
        path: '/admin/users',
        end: true,
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Committee assignments',
        path:
            '/admin/'
            + 'committee-assignments',
        end: true,
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Audit activity',
        path: '/admin/audit',
        end: true,
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'System notifications',
        path:
            '/admin/notifications',
        end: true,
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