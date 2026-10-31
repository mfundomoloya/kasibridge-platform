import type {
    ReactNode,
} from 'react'


import type {
    NavigationItem,
} from './PortalLayout'

import {
    PortalLayout,
} from './PortalLayout'

interface ProcurementLayoutProps {
    children: ReactNode
}

const procurementNavigation:
    NavigationItem[] = [
    {
        label: 'Showcase',
        path: '/showcase',
    },
    {
        label: 'Procurement overview',
        path: '/procurement',
        roles: [
            'ROLE_ANALYST',
            'ROLE_SPECIFICATION_OFFICER',
            'ROLE_EVALUATOR',
            'ROLE_ADJUDICATOR',
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Fraud assessments',
        path:
            '/procurement/fraud-assessments',
        roles: [
            'ROLE_ANALYST',
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'AI assessments',
        path:
            '/procurement/ai-assessments',
        roles: [
            'ROLE_SPECIFICATION_OFFICER',
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Support queue',
        path:
            '/procurement/support-tickets',
        roles: [
            'ROLE_SPECIFICATION_OFFICER',
            'ROLE_ADJUDICATOR',
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'Unassigned tickets',
        path:
            '/procurement/support-tickets/unassigned',
        roles: [
            'ROLE_PLATFORM_ADMIN',
        ],
    },
]
export function ProcurementLayout({
                                      children,
                                  }: ProcurementLayoutProps) {
    return (
        <PortalLayout
            portalName="Procurement Portal"
            navigationItems={
                procurementNavigation
            }
        >
            {children}
        </PortalLayout>
    )
}