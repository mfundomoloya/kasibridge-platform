import {
    PortalLayout,
} from '../components/layout/PortalLayout'

const procurementNavigation = [
    {
        label: 'Procurement overview',
        path: '/procurement',
    },
    {
        label: 'Tender specifications',
        path: '/procurement/specifications',
        roles: [
            'ROLE_SPECIFICATION_OFFICER',
        ] as const,
    },
    {
        label: 'Bid evaluations',
        path: '/procurement/evaluations',
        roles: [
            'ROLE_EVALUATOR',
        ] as const,
    },
    {
        label: 'Adjudication',
        path: '/procurement/adjudication',
        roles: [
            'ROLE_ADJUDICATOR',
        ] as const,
    },
    {
        label: 'Fraud alerts',
        path: '/procurement/alerts',
        roles: [
            'ROLE_ANALYST',
        ] as const,
    },
]

export function ProcurementDashboardPage() {
    return (
        <PortalLayout
            portalName="Procurement Portal"
            navigationItems={
                procurementNavigation.map((item) => ({
                    ...item,
                    roles: item.roles
                        ? [...item.roles]
                        : undefined,
                }))
            }
        >
            <section className="page-heading">
                <div>
          <span className="eyebrow">
            Controlled procurement workflow
          </span>

                    <h1>Procurement Overview</h1>

                    <p>
                        Access only the workflow functions
                        authorised for your assigned role.
                    </p>
                </div>
            </section>

            <section className="statistics-grid">
                <article className="statistic-card">
                    <span>Assigned activities</span>
                    <strong>0</strong>
                    <p>Tasks allocated to your role</p>
                </article>

                <article className="statistic-card">
                    <span>Open tenders</span>
                    <strong>0</strong>
                    <p>Active procurement processes</p>
                </article>

                <article className="statistic-card">
                    <span>Pending reviews</span>
                    <strong>0</strong>
                    <p>Items awaiting authorised action</p>
                </article>

                <article className="statistic-card">
                    <span>Alerts</span>
                    <strong>0</strong>
                    <p>Explainable integrity alerts</p>
                </article>
            </section>
        </PortalLayout>
    )
}