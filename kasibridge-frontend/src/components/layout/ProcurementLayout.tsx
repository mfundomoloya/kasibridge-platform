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
        end: true,
    },
    {
        label: 'Procurement overview',
        path: '/procurement',
        end: true,
        roles: [
            'ROLE_ANALYST',
            'ROLE_SPECIFICATION_OFFICER',
            'ROLE_EVALUATOR',
            'ROLE_ADJUDICATOR',
        ],
    },
    {
        label: 'Tender specifications',
        path:
            '/procurement/specifications',
        end: true,
        roles: [
            'ROLE_SPECIFICATION_OFFICER',
        ],
    },
    {
        label: 'Create specification',
        path:
            '/procurement/specifications/new',
        end: true,
        roles: [
            'ROLE_SPECIFICATION_OFFICER',
        ],
    },
    {
        label: 'My evaluations',
        path:
            '/procurement/evaluations',
        end: true,
        roles: [
            'ROLE_EVALUATOR',
        ],
    },
    {
        label: 'Fraud assessments',
        path:
            '/procurement/fraud-assessments',
        end: true,
        roles: [
            'ROLE_ANALYST',
        ],
    },
    {
        label: 'AI assessments',
        path:
            '/procurement/ai-assessments',
        end: true,
        roles: [
            'ROLE_SPECIFICATION_OFFICER',
        ],
    },
    {
        label: 'Support queue',
        path:
            '/procurement/support-tickets',
        end: true,
        roles: [
            'ROLE_SPECIFICATION_OFFICER',
            'ROLE_ADJUDICATOR',
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