import {
    useEffect,
    useState,
} from 'react'

import axios from 'axios'

import {
    Link,
    useParams,
} from 'react-router-dom'

import {
    getOpenTenderById,
} from '../api/tenderApi'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import {
    TenderStatusBadge,
} from '../components/tenders/TenderStatusBadge'

import type {
    TraderTender,
} from '../types/tender'

import {
    formatBuyerOrganisation,
    formatCurrency,
    formatDateTime,
} from '../utils/tenderFormatters'

function getTenderDetailsError(
    error: unknown
): string {
    if (!axios.isAxiosError(error)) {
        return (
            'An unexpected tender error occurred.'
        )
    }

    if (error.response?.status === 404) {
        return (
            'This tender is not available '
            + 'for trader bidding.'
        )
    }

    if (error.response?.status === 403) {
        return (
            'The signed-in account is not authorised '
            + 'to view this tender.'
        )
    }

    if (!error.response) {
        return (
            'The Procurement Service could not '
            + 'be reached.'
        )
    }

    return (
        'The tender details could not be loaded.'
    )
}

export function TenderDetailsPage() {
    const { tenderId } = useParams()

    const [tender, setTender] =
        useState<TraderTender | null>(null)

    const [isLoading, setIsLoading] =
        useState(true)

    const [errorMessage, setErrorMessage] =
        useState('')

    useEffect(() => {
        let isActive = true

        const parsedTenderId =
            Number(tenderId)

        if (
            !Number.isInteger(parsedTenderId)
            || parsedTenderId <= 0
        ) {
            Promise.resolve().then(() => {
                if (isActive) {
                    setErrorMessage(
                        'The tender identifier is invalid.'
                    )
                    setIsLoading(false)
                }
            })

            return () => {
                isActive = false
            }
        }

        getOpenTenderById(parsedTenderId)
            .then((loadedTender) => {
                if (!isActive) {
                    return
                }

                setTender(loadedTender)
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (!isActive) {
                    return
                }

                setTender(null)

                setErrorMessage(
                    getTenderDetailsError(error)
                )
            })
            .finally(() => {
                if (isActive) {
                    setIsLoading(false)
                }
            })

        return () => {
            isActive = false
        }
    }, [tenderId])

    return (
        <AppLayout>
            <div className="back-navigation">
                <Link to="/tenders">
                    Back to open tenders
                </Link>
            </div>

            {isLoading ? (
                <section className="content-card">
                    <p>Loading tender details...</p>
                </section>
            ) : errorMessage ? (
                <section className="content-card">
                    <div
                        className="error-message"
                        role="alert"
                    >
                        {errorMessage}
                    </div>

                    <div className="form-actions">
                        <Link
                            className="secondary-link-button"
                            to="/tenders"
                        >
                            Return to open tenders
                        </Link>
                    </div>
                </section>
            ) : tender ? (
                <>
                    <section className="tender-detail-hero">
                        <div>
              <span className="tender-reference">
                {tender.tenderReference}
              </span>

                            <h1>{tender.title}</h1>

                            <p>
                                {formatBuyerOrganisation(
                                    tender.buyerOrgId
                                )}
                            </p>
                        </div>

                        <TenderStatusBadge
                            status={tender.status}
                        />
                    </section>

                    <section className="tender-detail-grid">
                        <article className="content-card">
              <span className="eyebrow">
                Opportunity details
              </span>

                            <h2>Tender description</h2>

                            <p className="tender-detail-text">
                                {tender.description}
                            </p>
                        </article>

                        <article className="content-card">
              <span className="eyebrow">
                Summary
              </span>

                            <dl className="profile-details">
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

                                <div>
                                    <dt>Buyer organisation</dt>

                                    <dd>
                                        {formatBuyerOrganisation(
                                            tender.buyerOrgId
                                        )}
                                    </dd>
                                </div>

                                <div>
                                    <dt>Status</dt>

                                    <dd>Open for bidding</dd>
                                </div>
                            </dl>
                        </article>

                        <article className="content-card tender-detail-wide">
              <span className="eyebrow">
                Evaluation
              </span>

                            <h2>Evaluation criteria</h2>

                            <p className="tender-detail-text">
                                {tender.evaluationCriteria}
                            </p>
                        </article>

                        <article className="content-card tender-detail-wide">
              <span className="eyebrow">
                Bid submission
              </span>

                            <h2>
                                Interested in this tender?
                            </h2>

                            <p className="tender-detail-text">
                                Bid submission will be enabled
                                after the deadline and compliance
                                workflow is connected to the Trader
                                Dashboard.
                            </p>

                            <button
                                className="primary-button"
                                type="button"
                                disabled
                            >
                                Bid submission coming next
                            </button>
                        </article>
                    </section>
                </>
            ) : null}
        </AppLayout>
    )
}