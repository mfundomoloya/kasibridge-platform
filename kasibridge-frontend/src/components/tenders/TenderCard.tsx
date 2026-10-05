import { Link } from 'react-router-dom'

import type {
    TraderTender,
} from '../../types/tender'

import {
    formatBuyerOrganisation,
    formatCurrency,
    formatDateTime,
} from '../../utils/tenderFormatters'

import {
    TenderStatusBadge,
} from './TenderStatusBadge'

interface TenderCardProps {
    tender: TraderTender
}

export function TenderCard({
                               tender,
                           }: TenderCardProps) {
    return (
        <article className="tender-card">
            <div className="tender-card-heading">
                <div>
          <span className="tender-reference">
            {tender.tenderReference}
          </span>

                    <h2>{tender.title}</h2>
                </div>

                <TenderStatusBadge
                    status={tender.status}
                />
            </div>

            <p className="tender-description">
                {tender.description}
            </p>

            <dl className="tender-summary">
                <div>
                    <dt>Buyer organisation</dt>

                    <dd>
                        {formatBuyerOrganisation(
                            tender.buyerOrgId
                        )}
                    </dd>
                </div>

                <div>
                    <dt>Budget</dt>

                    <dd>
                        {formatCurrency(
                            tender.budgetAmount
                        )}
                    </dd>
                </div>

                <div>
                    <dt>Published</dt>

                    <dd>
                        {formatDateTime(
                            tender.publishedAt
                        )}
                    </dd>
                </div>
            </dl>

            <div className="tender-card-actions">
                <Link
                    className="primary-link-button"
                    to={`/tenders/${tender.id}`}
                >
                    View tender details
                </Link>
            </div>
        </article>
    )
}