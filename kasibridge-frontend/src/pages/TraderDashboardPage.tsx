import { AppLayout } from '../components/layout/AppLayout'

const dashboardStatistics = [
    {
        label: 'Open tenders',
        value: '0',
        description: 'Available opportunities',
    },
    {
        label: 'Active bids',
        value: '0',
        description: 'Submitted bids in progress',
    },
    {
        label: 'Support tickets',
        value: '0',
        description: 'Open support requests',
    },
    {
        label: 'Unread notifications',
        value: '0',
        description: 'Updates requiring attention',
    },
]

export function TraderDashboardPage() {
    return (
        <AppLayout>
            <section className="page-heading">
                <div>
          <span className="eyebrow">
            Trader overview
          </span>

                    <h1>Dashboard</h1>

                    <p>
                        Track tenders, bids, clarification
                        notices and support activity from one
                        secure workspace.
                    </p>
                </div>

                <button
                    className="primary-button"
                    type="button"
                >
                    Browse open tenders
                </button>
            </section>

            <section className="statistics-grid">
                {dashboardStatistics.map((statistic) => (
                    <article
                        className="statistic-card"
                        key={statistic.label}
                    >
                        <span>{statistic.label}</span>

                        <strong>{statistic.value}</strong>

                        <p>{statistic.description}</p>
                    </article>
                ))}
            </section>

            <section className="dashboard-grid">
                <article className="content-card">
                    <div className="card-heading">
                        <div>
              <span className="eyebrow">
                Opportunities
              </span>

                            <h2>Recent open tenders</h2>
                        </div>

                        <button
                            className="text-button"
                            type="button"
                        >
                            View all
                        </button>
                    </div>

                    <div className="empty-state">
                        <h3>No tenders loaded yet</h3>

                        <p>
                            Open tender information will appear
                            here after the frontend is connected
                            to the Procurement Service.
                        </p>
                    </div>
                </article>

                <article className="content-card">
                    <div className="card-heading">
                        <div>
              <span className="eyebrow">
                Communications
              </span>

                            <h2>Latest notifications</h2>
                        </div>
                    </div>

                    <div className="empty-state">
                        <h3>No notifications loaded yet</h3>

                        <p>
                            Dashboard and WhatsApp updates will
                            remain aligned with official
                            KasiBridge records.
                        </p>
                    </div>
                </article>
            </section>
        </AppLayout>
    )
}