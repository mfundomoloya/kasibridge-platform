import {
    useEffect,
    useState,
} from 'react'

import {
    getBidApiError,
    getMyBids,
} from '../api/bidApi'

import {
    BidCard,
} from '../components/bids/BidCard'

import {
    WithdrawBidDialog,
} from '../components/bids/WithdrawBidDialog'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import type {
    Bid,
} from '../types/bid'

export function MyBidsPage() {
    const [bids, setBids] =
        useState<Bid[]>([])

    const [selectedBid, setSelectedBid] =
        useState<Bid | null>(null)

    const [isLoading, setIsLoading] =
        useState(true)

    const [errorMessage, setErrorMessage] =
        useState('')

    useEffect(() => {
        let isActive = true

        async function loadBids() {
            setIsLoading(true)
            setErrorMessage('')

            try {
                const loadedBids =
                    await getMyBids()

                if (!isActive) {
                    return
                }

                setBids(loadedBids)
            } catch (error) {
                if (!isActive) {
                    return
                }

                const apiError =
                    getBidApiError(error)

                setBids([])
                setErrorMessage(
                    apiError.message
                )
            } finally {
                if (isActive) {
                    setIsLoading(false)
                }
            }
        }

        void loadBids()

        return () => {
            isActive = false
        }
    }, [])

    function handleWithdrawRequest(
        bid: Bid,
    ) {
        setSelectedBid(bid)
    }

    function handleDialogClose() {
        setSelectedBid(null)
    }

    function handleBidWithdrawn(
        withdrawnBid: Bid,
    ) {
        setBids((currentBids) =>
            currentBids.map((bid) =>
                bid.id === withdrawnBid.id
                    ? withdrawnBid
                    : bid
            )
        )

        setSelectedBid(null)
    }

    return (
        <AppLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Trader portal
                    </span>

                    <h1>My bids</h1>

                    <p>
                        Review your submitted bids,
                        compliance results, and current
                        bid statuses.
                    </p>
                </div>
            </section>

            {isLoading ? (
                <section className="content-card">
                    <p>Loading your bids...</p>
                </section>
            ) : errorMessage ? (
                <section className="content-card">
                    <div
                        className="error-message"
                        role="alert"
                    >
                        {errorMessage}
                    </div>
                </section>
            ) : bids.length === 0 ? (
                <section className="content-card">
                    <span className="eyebrow">
                        Bid activity
                    </span>

                    <h2>No bids submitted</h2>

                    <p>
                        You have not submitted any
                        bids yet.
                    </p>
                </section>
            ) : (
                <section
                    className="bid-list"
                    aria-label="Submitted bids"
                >
                    {bids.map((bid) => (
                        <BidCard
                            key={bid.id}
                            bid={bid}
                            isWithdrawing={
                                selectedBid?.id
                                === bid.id
                            }
                            onWithdraw={
                                handleWithdrawRequest
                            }
                        />
                    ))}
                </section>
            )}

            {selectedBid && (
                <WithdrawBidDialog
                    bid={selectedBid}
                    isOpen={true}
                    onClose={handleDialogClose}
                    onWithdrawn={
                        handleBidWithdrawn
                    }
                />
            )}
        </AppLayout>
    )
}