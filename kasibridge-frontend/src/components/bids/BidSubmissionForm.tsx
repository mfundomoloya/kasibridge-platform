import {
    type FormEvent,
    useState,
} from "react";

import {
    getBidApiError,
    submitBid,
} from "../../api/bidApi";
import type { Bid } from "../../types/bid";

interface BidSubmissionFormProps {
    tenderId: number;
    onBidSubmitted?: (bid: Bid) => void;
    onCancel?: () => void;
}

interface BidFormState {
    technicalProposal: string;
    priceAmount: string;
}

interface BidFormErrors {
    technicalProposal?: string;
    priceAmount?: string;
    general?: string;
}

const initialFormState: BidFormState = {
    technicalProposal: "",
    priceAmount: "",
};

export default function BidSubmissionForm({
                                              tenderId,
                                              onBidSubmitted,
                                              onCancel,
                                          }: BidSubmissionFormProps) {
    const [form, setForm] =
        useState<BidFormState>(initialFormState);

    const [errors, setErrors] =
        useState<BidFormErrors>({});

    const [isSubmitting, setIsSubmitting] =
        useState(false);

    const [submittedBid, setSubmittedBid] =
        useState<Bid | null>(null);

    function updateField(
        field: keyof BidFormState,
        value: string,
    ) {
        setForm((currentForm) => ({
            ...currentForm,
            [field]: value,
        }));

        setErrors((currentErrors) => ({
            ...currentErrors,
            [field]: undefined,
            general: undefined,
    }));
    }

    function validateForm(): BidFormErrors {
        const validationErrors: BidFormErrors = {};
        const proposal =
            form.technicalProposal.trim();
        const amount = Number(form.priceAmount);

        if (!proposal) {
            validationErrors.technicalProposal =
                "Technical proposal is required.";
        }

        if (!form.priceAmount.trim()) {
            validationErrors.priceAmount =
                "Bid price is required.";
        } else if (!Number.isFinite(amount)) {
            validationErrors.priceAmount =
                "Enter a valid bid price.";
        } else if (amount <= 0) {
            validationErrors.priceAmount =
                "Bid price must be greater than zero.";
        }

        return validationErrors;
    }

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>,
    ) {
        event.preventDefault();

        const validationErrors = validateForm();

        if (
            Object.keys(validationErrors).length > 0
        ) {
            setErrors(validationErrors);
            return;
        }

        setIsSubmitting(true);
        setErrors({});

        try {
            const bid = await submitBid(
                tenderId,
                {
                    technicalProposal:
                        form.technicalProposal.trim(),
                    priceAmount:
                        Number(form.priceAmount),
                },
            );

            setSubmittedBid(bid);
            setForm(initialFormState);
            onBidSubmitted?.(bid);
        } catch (error) {
            const apiError =
                getBidApiError(error);

            setErrors({
                general: apiError.message,
                technicalProposal:
                apiError.fieldErrors
                    ?.technicalProposal,
                priceAmount:
                apiError.fieldErrors
                    ?.priceAmount,
            });
        } finally {
            setIsSubmitting(false);
        }
    }

    if (submittedBid) {
        return (
            <section
                className="bid-submission-success"
                aria-live="polite"
            >
                <h2>
                    Bid submitted successfully
                </h2>

                <p>
                    Your bid reference is{" "}
                    <strong>
                        {submittedBid.bidReference}
                    </strong>
                    .
                </p>

                <p>
                    Status:{" "}
                    <strong>
                        {submittedBid.status}
                    </strong>
                </p>
            </section>
        );
    }

    return (
        <section className="bid-submission-form">
            <h2>Submit a bid</h2>

            <p>
                Provide your technical proposal and
                bid price. Your submission will be recorded
                against this tender.
            </p>

            {errors.general && (
                <div
                    className="form-error-message"
                    role="alert"
                >
                    {errors.general}
                </div>
            )}

            <form
                onSubmit={handleSubmit}
                noValidate
            >
                <div className="form-group">
                    <label htmlFor="technicalProposal">
                        Technical proposal
                    </label>

                    <textarea
                        id="technicalProposal"
                        name="technicalProposal"
                        value={
                            form.technicalProposal
                        }
                        onChange={(event) =>
                            updateField(
                                "technicalProposal",
                                event.target.value,
                            )
                        }
                        rows={10}
                        disabled={isSubmitting}
                        aria-invalid={Boolean(
                            errors.technicalProposal,
                        )}
                        aria-describedby={
                            errors.technicalProposal
                                ? "technicalProposal-error"
                                : undefined
                        }
                    />

                    {errors.technicalProposal && (
                        <p
                            id="technicalProposal-error"
                            className="field-error"
                            role="alert"
                        >
                            {
                                errors.technicalProposal
                            }
                        </p>
                    )}
                </div>

                <div className="form-group">
                    <label htmlFor="priceAmount">
                        Bid price
                    </label>

                    <input
                        id="priceAmount"
                        name="priceAmount"
                        type="number"
                        min="0.01"
                        step="0.01"
                        inputMode="decimal"
                        value={form.priceAmount}
                        onChange={(event) =>
                            updateField(
                                "priceAmount",
                                event.target.value,
                            )
                        }
                        disabled={isSubmitting}
                        aria-invalid={Boolean(
                            errors.priceAmount,
                        )}
                        aria-describedby={
                            errors.priceAmount
                                ? "priceAmount-error"
                                : undefined
                        }
                    />

                    {errors.priceAmount && (
                        <p
                            id="priceAmount-error"
                            className="field-error"
                            role="alert"
                        >
                            {errors.priceAmount}
                        </p>
                    )}
                </div>

                <div className="form-actions">
                    <button
                        className="primary-button"
                        type="submit"
                        disabled={isSubmitting}
                    >
                        {isSubmitting
                            ? "Submitting bid..."
                            : "Submit bid"}
                    </button>

                    {onCancel && (
                        <button
                            className="secondary-button"
                            type="button"
                            onClick={onCancel}
                            disabled={isSubmitting}
                        >
                            Cancel
                        </button>
                    )}
                </div>
            </form>
        </section>
    );
}