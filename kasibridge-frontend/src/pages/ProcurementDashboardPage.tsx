import {
    Link,
} from 'react-router-dom'

import {
    ProcurementLayout,
} from '../components/layout/ProcurementLayout'

export function ProcurementDashboardPage() {
    return (
        <ProcurementLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Controlled procurement workflow
                    </span>

                    <h1>Procurement overview</h1>

                    <p>
                        Access only the workflow
                        functions authorised for your
                        assigned role.
                    </p>
                </div>
            </section>

            <section className="statistics-grid">
                <Link
                    className={
                        'statistic-card '
                        + 'dashboard-statistic-link'
                    }
                    to="/procurement/support-tickets"
                >
                    <span>Assigned support tickets</span>

                    <strong>View</strong>

                    <p>
                        Review support activities
                        assigned to your account.
                    </p>
                </Link>

                <article className="statistic-card">
                    <span>Open tenders</span>

                    <strong>0</strong>

                    <p>
                        Active procurement processes
                    </p>
                </article>

                <article className="statistic-card">
                    <span>Pending reviews</span>

                    <strong>0</strong>

                    <p>
                        Items awaiting authorised
                        action
                    </p>
                </article>

                <article className="statistic-card">
                    <span>Alerts</span>

                    <strong>0</strong>

                    <p>
                        Explainable integrity alerts
                    </p>
                </article>
            </section>

            <section className="dashboard-grid">
                <article className="content-card">
                    <span className="eyebrow">
                        Support workflow
                    </span>

                    <h2>Assigned ticket queue</h2>

                    <p className="tender-detail-text">
                        Review support requests assigned
                        to your account and perform only
                        the actions permitted by the
                        ticket’s current workflow state.
                    </p>

                    <div className="form-actions">
                        <Link
                            className="primary-link-button"
                            to="/procurement/support-tickets"
                        >
                            Open support queue
                        </Link>
                    </div>
                </article>

                <article className="content-card">
                    <span className="eyebrow">
                        Segregation of duties
                    </span>

                    <h2>Role-controlled access</h2>

                    <p className="tender-detail-text">
                        Procurement functions are
                        displayed according to the roles
                        included in the authenticated
                        user’s access token.
                    </p>
                </article>
            </section>
        </ProcurementLayout>
    )
}