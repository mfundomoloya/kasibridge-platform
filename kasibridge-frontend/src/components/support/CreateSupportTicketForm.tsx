import {
    useState,
} from 'react'

import type {
    FormEventHandler,
} from 'react'

import {
    createSupportTicket,
    getSupportTicketApiError,
} from '../../api/supportTicketApi'

import {
    SUPPORT_TICKET_TYPE_LABELS,
} from '../../types/supportTicket'

import type {
    CreateSupportTicketRequest,
    SupportTicket,
    SupportTicketType,
} from '../../types/supportTicket'

interface CreateSupportTicketFormProps {
    tenderId: number
    bidId?: number | null
    onCreated: (ticket: SupportTicket) => void
    onCancel?: () => void
}

interface SupportTicketFormState {
    ticketType: SupportTicketType | ''
    subject: string
    description: string
}

interface SupportTicketFormErrors {
    ticketType?: string
    subject?: string
    description?: string
    general?: string
}

const initialFormState: SupportTicketFormState = {
    ticketType: '',
    subject: '',
    description: '',
}

const supportTicketTypes: SupportTicketType[] = [
    'CLARIFICATION_REQUEST',
    'COMPLIANCE_APPEAL',
    'UPLOAD_ISSUE',
    'GENERAL_SUPPORT',
]

function isSupportTicketType(
    value: string
): value is SupportTicketType {
    return supportTicketTypes.some(
        (ticketType) => ticketType === value
    )
}

export function CreateSupportTicketForm({
                                            tenderId,
                                            bidId = null,
                                            onCreated,
                                            onCancel,
                                        }: CreateSupportTicketFormProps) {
    const [form, setForm] =
        useState<SupportTicketFormState>(
            initialFormState
        )

    const [errors, setErrors] =
        useState<SupportTicketFormErrors>({})

    const [isSubmitting, setIsSubmitting] =
        useState(false)

    function updateTicketType(
        value: string
    ) {
        const ticketType:
            SupportTicketType | '' =
            isSupportTicketType(value)
                ? value
                : ''

        setForm((currentForm) => ({
            ...currentForm,
            ticketType,
        }))

        setErrors((currentErrors) => ({
            ...currentErrors,
            ticketType: undefined,
            general: undefined,
        }))
    }

    function updateSubject(
        subject: string
    ) {
        setForm((currentForm) => ({
            ...currentForm,
            subject,
        }))

        setErrors((currentErrors) => ({
            ...currentErrors,
            subject: undefined,
            general: undefined,
        }))
    }

    function updateDescription(
        description: string
    ) {
        setForm((currentForm) => ({
            ...currentForm,
            description,
        }))

        setErrors((currentErrors) => ({
            ...currentErrors,
            description: undefined,
            general: undefined,
        }))
    }

    function validateForm():
        SupportTicketFormErrors {
        const validationErrors:
            SupportTicketFormErrors = {}

        const subject =
            form.subject.trim()

        const description =
            form.description.trim()

        if (!form.ticketType) {
            validationErrors.ticketType =
                'Ticket type is required.'
        }

        if (!subject) {
            validationErrors.subject =
                'Subject is required.'
        } else if (subject.length > 200) {
            validationErrors.subject =
                'Subject cannot exceed 200 characters.'
        }

        if (!description) {
            validationErrors.description =
                'Description is required.'
        } else if (
            description.length > 3000
        ) {
            validationErrors.description =
                'Description cannot exceed 3000 characters.'
        }

        return validationErrors
    }

    const handleSubmit:
        FormEventHandler<HTMLFormElement> =
        async (event) => {
            event.preventDefault()

            const validationErrors =
                validateForm()

            if (
                Object.keys(validationErrors)
                    .length > 0
            ) {
                setErrors(validationErrors)
                return
            }

            if (!form.ticketType) {
                return
            }

            setIsSubmitting(true)
            setErrors({})

            const request:
                CreateSupportTicketRequest = {
                ticketType:
                form.ticketType,
                subject:
                    form.subject.trim(),
                description:
                    form.description.trim(),
            }

            if (bidId !== null) {
                request.bidId = bidId
            }

            try {
                const createdTicket =
                    await createSupportTicket(
                        tenderId,
                        request
                    )

                setForm(initialFormState)
                onCreated(createdTicket)
            } catch (error) {
                const apiError =
                    getSupportTicketApiError(
                        error
                    )

                setErrors({
                    general:
                    apiError.message,
                    ticketType:
                    apiError.fieldErrors
                        ?.ticketType,
                    subject:
                    apiError.fieldErrors
                        ?.subject,
                    description:
                    apiError.fieldErrors
                        ?.description,
                })
            } finally {
                setIsSubmitting(false)
            }
        }

    return (
        <section className="support-ticket-form">
            <span className="eyebrow">
                New support request
            </span>

            <h1>Create a support ticket</h1>

            <p>
                Provide the issue details for
                tender{' '}
                <strong>{tenderId}</strong>.
            </p>

            {bidId !== null && (
                <p>
                    This request will be linked
                    to bid{' '}
                    <strong>{bidId}</strong>.
                </p>
            )}

            {errors.general && (
                <div
                    className="error-message"
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
                    <label htmlFor="ticketType">
                        Ticket type
                    </label>

                    <select
                        id="ticketType"
                        name="ticketType"
                        value={form.ticketType}
                        disabled={isSubmitting}
                        aria-invalid={Boolean(
                            errors.ticketType
                        )}
                        aria-describedby={
                            errors.ticketType
                                ? 'ticketType-error'
                                : undefined
                        }
                        onChange={(event) => {
                            updateTicketType(
                                event.target.value
                            )
                        }}
                    >
                        <option value="">
                            Select a ticket type
                        </option>

                        {supportTicketTypes.map(
                            (ticketType) => (
                                <option
                                    key={ticketType}
                                    value={ticketType}
                                >
                                    {
                                        SUPPORT_TICKET_TYPE_LABELS[
                                            ticketType
                                            ]
                                    }
                                </option>
                            )
                        )}
                    </select>

                    {errors.ticketType && (
                        <p
                            id="ticketType-error"
                            className="field-error"
                            role="alert"
                        >
                            {errors.ticketType}
                        </p>
                    )}
                </div>

                <div className="form-group">
                    <label htmlFor="ticketSubject">
                        Subject
                    </label>

                    <input
                        id="ticketSubject"
                        name="subject"
                        type="text"
                        maxLength={200}
                        value={form.subject}
                        disabled={isSubmitting}
                        aria-invalid={Boolean(
                            errors.subject
                        )}
                        aria-describedby={
                            errors.subject
                                ? 'ticketSubject-error'
                                : 'ticketSubject-help'
                        }
                        onChange={(event) => {
                            updateSubject(
                                event.target.value
                            )
                        }}
                    />

                    <p
                        id="ticketSubject-help"
                        className="field-help"
                    >
                        {form.subject.length}/200
                        {' '}characters
                    </p>

                    {errors.subject && (
                        <p
                            id="ticketSubject-error"
                            className="field-error"
                            role="alert"
                        >
                            {errors.subject}
                        </p>
                    )}
                </div>

                <div className="form-group">
                    <label
                        htmlFor="ticketDescription"
                    >
                        Description
                    </label>

                    <textarea
                        id="ticketDescription"
                        name="description"
                        rows={10}
                        maxLength={3000}
                        value={form.description}
                        disabled={isSubmitting}
                        aria-invalid={Boolean(
                            errors.description
                        )}
                        aria-describedby={
                            errors.description
                                ? 'ticketDescription-error'
                                : 'ticketDescription-help'
                        }
                        onChange={(event) => {
                            updateDescription(
                                event.target.value
                            )
                        }}
                    />

                    <p
                        id="ticketDescription-help"
                        className="field-help"
                    >
                        {form.description.length}/3000
                        {' '}characters
                    </p>

                    {errors.description && (
                        <p
                            id="ticketDescription-error"
                            className="field-error"
                            role="alert"
                        >
                            {errors.description}
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
                            ? 'Creating ticket...'
                            : 'Create support ticket'}
                    </button>

                    {onCancel && (
                        <button
                            className="secondary-button"
                            type="button"
                            disabled={isSubmitting}
                            onClick={onCancel}
                        >
                            Cancel
                        </button>
                    )}
                </div>
            </form>
        </section>
    )
}