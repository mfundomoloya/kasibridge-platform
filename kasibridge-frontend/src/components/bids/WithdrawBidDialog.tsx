import {
    useEffect,
    useRef,
    useState,
} from 'react'

import {
    getBidApiError,
    withdrawBid,
} from '../../api/bidApi'

import type {
    Bid,
} from '../../types/bid'

interface WithdrawBidDialogProps {
    bid: Bid
    isOpen: boolean
    onClose: () => void
    onWithdrawn: (bid: Bid) => void
}

export function WithdrawBidDialog({
                                      bid,
                                      isOpen,
                                      onClose,
                                      onWithdrawn,
                                  }: WithdrawBidDialogProps) {
    const dialogRef =
        useRef<HTMLDialogElement>(null)

    const [isWithdrawing, setIsWithdrawing] =
        useState(false)

    const [errorMessage, setErrorMessage] =
        useState('')

    useEffect(() => {
        const dialog = dialogRef.current

        if (!dialog) {
            return
        }

        if (isOpen && !dialog.open) {
            setErrorMessage('')
            dialog.showModal()
            return
        }

        if (!isOpen && dialog.open) {
            dialog.close()
        }
    }, [isOpen])

    function handleDialogClose() {
        if (!isWithdrawing) {
            onClose()
        }
    }

    function handleCancel() {
        if (isWithdrawing) {
            return
        }

        setErrorMessage('')

        const dialog = dialogRef.current

        if (dialog?.open) {
            dialog.close()
        } else {
            onClose()
        }
    }

    async function handleWithdraw() {
        if (isWithdrawing) {
            return
        }

        setIsWithdrawing(true)
        setErrorMessage('')

        try {
            const withdrawnBid =
                await withdrawBid(bid.id)

            onWithdrawn(withdrawnBid)

            const dialog = dialogRef.current

            if (dialog?.open) {
                dialog.close()
            } else {
                onClose()
            }
        } catch (error) {
            const apiError =
                getBidApiError(error)

            setErrorMessage(apiError.message)
        } finally {
            setIsWithdrawing(false)
        }
    }

    return (
        <dialog
            ref={dialogRef}
            className="confirmation-dialog"
            aria-labelledby="withdraw-bid-title"
            aria-describedby="withdraw-bid-description"
            onClose={handleDialogClose}
            onCancel={(event) => {
                if (isWithdrawing) {
                    event.preventDefault()
                    return
                }

                setErrorMessage('')
            }}
        >
            <div className="confirmation-dialog-content">
                <span className="eyebrow">
                    Bid withdrawal
                </span>

                <h2 id="withdraw-bid-title">
                    Withdraw this bid?
                </h2>

                <p id="withdraw-bid-description">
                    You are about to withdraw bid{' '}
                    <strong>
                        {bid.bidReference}
                    </strong>
                    . This action cannot be reversed.
                </p>

                {errorMessage && (
                    <div
                        className="error-message"
                        role="alert"
                    >
                        {errorMessage}
                    </div>
                )}

                <div className="form-actions">
                    <button
                        className="secondary-button"
                        type="button"
                        disabled={isWithdrawing}
                        onClick={handleCancel}
                    >
                        Keep bid
                    </button>

                    <button
                        className="danger-button"
                        type="button"
                        disabled={isWithdrawing}
                        onClick={handleWithdraw}
                    >
                        {isWithdrawing
                            ? 'Withdrawing bid...'
                            : 'Withdraw bid'}
                    </button>
                </div>
            </div>
        </dialog>
    )
}