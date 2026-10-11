import {
    useState,
} from 'react'

import {
    Link,
    Navigate,
    useNavigate,
    useSearchParams,
} from 'react-router-dom'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import {
    CreateSupportTicketForm,
} from '../components/support/CreateSupportTicketForm'

import type {
    SupportTicket,
} from '../types/supportTicket'

function parsePositiveInteger(
    value: string | null
): number | null {
    if (!value) {
        return null
    }

    const parsedValue = Number(value)

    if (
        !Number.isInteger(parsedValue)
        || parsedValue <= 0
    ) {
        return null
    }

    return parsedValue
}

export function CreateSupportTicketPage() {
    const navigate = useNavigate()

    const [searchParams] =
        useSearchParams()

    const [createdTicket, setCreatedTicket] =
        useState<SupportTicket | null>(null)

    const tenderId =
        parsePositiveInteger(
            searchParams.get('tenderId')
        )

    const bidId =
        parsePositiveInteger(
            searchParams.get('bidId')
        )

    function handleCreated(
        ticket: SupportTicket
    ) {
        setCreatedTicket(ticket)
    }

    function handleCancel() {
        navigate('/support-tickets')
    }

    if (!tenderId) {
        return (
            <AppLayout>
                <div className="back-navigation">
                    <Link to="/support-tickets">
                        Back to support tickets
                    </Link>
                </div>

                <section className="content-card">
                    <div
                        className="error-message"
                        role="alert"
                    >
                        A valid tender must be
                        selected before creating a
                        support ticket.
                    </div>

                    <div className="form-actions">
                        <Link
                            className="primary-link-button"
                            to="/tenders"
                        >
                            View open tenders
                        </Link>

                        <Link
                            className="secondary-link-button"
                            to="/support-tickets"
                        >
                            Return to support tickets
                        </Link>
                    </div>
                </section>
            </AppLayout>
        )
    }

    if (createdTicket) {
        return (
            <Navigate
                to={
                    `/support-tickets/`
                    + createdTicket.id
                }
                replace
            />
        )
    }

    return (
        <AppLayout>
            <div className="back-navigation">
                <Link to="/support-tickets">
                    Back to support tickets
                </Link>
            </div>

            <section className="content-card">
                <CreateSupportTicketForm
                    tenderId={tenderId}
                    bidId={bidId}
                    onCreated={handleCreated}
                    onCancel={handleCancel}
                />
            </section>
        </AppLayout>
    )
}