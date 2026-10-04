import {
    PortalLayout,
} from '../components/layout/PortalLayout'

const adminNavigation = [
    {
        label: 'Administration',
        path: '/admin',
    },
    {
        label: 'Users and roles',
        path: '/admin/users',
    },
    {
        label: 'Committee assignments',
        path: '/admin/committees',
    },
    {
        label: 'Audit activity',
        path: '/admin/audit',
    },
    {
        label: 'System notifications',
        path: '/admin/notifications',
    },
]

export function AdminDashboardPage() {
    return (
        <PortalLayout
            portalName="Administration Dashboard"
            navigationItems={adminNavigation}
        >
            <section className="page-heading">
                <div>
          <span className="eyebrow">
            Platform administration
          </span>

                    <h1>Administration Dashboard</h1>

                    <p>
                        Manage platform access, role assignments,
                        committee controls and system oversight.
                    </p>
                </div>
            </section>

            <section className="statistics-grid">
                <article className="statistic-card">
                    <span>Active users</span>
                    <strong>0</strong>
                    <p>Authorised KasiBridge accounts</p>
                </article>

                <article className="statistic-card">
                    <span>Committee assignments</span>
                    <strong>0</strong>
                    <p>Active procurement responsibilities</p>
                </article>

                <article className="statistic-card">
                    <span>Open audit events</span>
                    <strong>0</strong>
                    <p>Items requiring administrative review</p>
                </article>

                <article className="statistic-card">
                    <span>System notifications</span>
                    <strong>0</strong>
                    <p>Operational updates</p>
                </article>
            </section>
        </PortalLayout>
    )
}