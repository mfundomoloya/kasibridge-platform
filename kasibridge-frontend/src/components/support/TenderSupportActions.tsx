import {
    Link,
} from 'react-router-dom'

interface TenderSupportActionsProps {
    tenderId: number
}

export function TenderSupportActions({
                                         tenderId,
                                     }: TenderSupportActionsProps) {
    return (
        <article
            className={
                'content-card '
                + 'tender-detail-wide '
                + 'tender-support-actions'
            }
        >
            <span className="eyebrow">
                Tender support
            </span>

            <h2>Need help with this tender?</h2>

            <p className="tender-detail-text">
                Create a support request for a
                clarification, compliance appeal,
                upload issue, or general assistance.
            </p>

            <div className="form-actions">
                <Link
                    className="primary-link-button"
                    to={
                        '/support-tickets/new'
                        + `?tenderId=${tenderId}`
                    }
                >
                    Create support ticket
                </Link>

                <Link
                    className="secondary-link-button"
                    to="/support-tickets"
                >
                    View my support tickets
                </Link>
            </div>
        </article>
    )
}