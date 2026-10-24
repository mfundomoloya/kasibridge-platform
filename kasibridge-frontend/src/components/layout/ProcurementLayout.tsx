import type {
    ReactNode,
} from 'react'

import type {
    UserRole,
} from '../../types/auth'

import {
    PortalLayout,
} from './PortalLayout'

interface ProcurementLayoutProps {
    children: ReactNode
}

interface ProcurementNavigationItem {
    label: string
    path: string
    roles?: UserRole[]
}

const procurementNavigation:
    ProcurementNavigationItem[] = [

    {
        label: 'Showcase',
        path: '/showcase',
    },
    {
        label: 'Procurement overview',
        path: '/procurement',
    },
    {
        label: 'Procurement overview',
        path: '/procurement',
    },
    {
        label: 'Support queue',
        path: '/procurement/support-tickets',
        roles: [
            'ROLE_SPECIFICATION_OFFICER',
            'ROLE_ADJUDICATOR',
            'ROLE_PLATFORM_ADMIN',
        ],
    },
    {
        label: 'AI assessment queue',
        path: '/procurement/ai-assessments',
        roles: [
            'ROLE_SPECIFICATION_OFFICER',
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
    {
        label: 'Tender specifications',
        path: '/procurement/specifications',
        roles: [
            'ROLE_SPECIFICATION_OFFICER',
        ],
    },
    {
        label: 'Bid evaluations',
        path: '/procurement/evaluations',
        roles: [
            'ROLE_EVALUATOR',
        ],
    },
    {
        label: 'Adjudication',
        path: '/procurement/adjudication',
        roles: [
            'ROLE_ADJUDICATOR',
        ],
    },
    {
        label: 'Fraud alerts',
        path: '/procurement/alerts',
        roles: [
            'ROLE_ANALYST',
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