import {
    useCallback,
    useEffect,
    useState,
} from 'react'

import {
    approveTicketAiAssessment,
    escalateTicketAiAssessment,
    generateTicketAiAssessment,
    getTicketAiAssessment,
    publishTicketAiAssessmentResponse,
    rejectTicketAiAssessment,
} from '../../api/supportTicketAiAssessmentApi'

import {
    AI_ASSESSMENT_PRIORITY_LABELS,
    AI_RESOLUTION_MODE_LABELS,
    canApproveAiAssessment,
    canEscalateAiAssessment,
    canPublishAiAssessment,
    canRejectAiAssessment,
    formatAiDetectedCategory,
} from '../../types/supportTicketAiAssessment'

import type {
    SupportTicketAiAssessment,
} from '../../types/supportTicketAiAssessment'

import {
    TicketAiAssessmentStatusBadge,
} from './TicketAiAssessmentStatusBadge'

import './TicketAiAssessmentReviewPanel.css'

interface TicketAiAssessmentReviewPanelProps {
    ticketId: number
    currentUserId: number | null
    onAssessmentUpdated?: (
        assessment: SupportTicketAiAssessment,
    ) => void
}

type AssessmentAction =
    | 'load'
    | 'generate'
    | 'approve'
    | 'reject'
    | 'escalate'
    | 'publish'
    | null

interface ApiErrorResponse {
    message?: string
    error?: string
    fieldErrors?: Record<string, string>
}

function getErrorMessage(
    error: unknown,
    fallbackMessage: string,
): string {
    if (
        typeof error === 'object'
        && error !== null
        && 'response' in error
    ) {
        const response =
            (
                error as {
                    response?: {
                        data?: ApiErrorResponse
                    }
                }
            ).response

        const data = response?.data

        if (data?.message) {
            return data.message
        }

        if (data?.error) {
            return data.error
        }

        if (data?.fieldErrors) {
            const firstFieldError =
                Object.values(data.fieldErrors)[0]

            if (firstFieldError) {
                return firstFieldError
            }
        }
    }

    if (
        error instanceof Error
        && error.message.trim().length > 0
    ) {
        return error.message
    }

    return fallbackMessage
}

function formatDateTime(
    value: string | null,
): string {
    if (!value) {
        return 'Not recorded'
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return value
    }

    return date.toLocaleString()
}

function formatConfidenceScore(
    score: number | null,
): string {
    if (score === null) {
        return 'Not available'
    }

    const normalizedScore =
        score >= 0 && score <= 1
            ? score * 100
            : score

    return `${normalizedScore.toFixed(1)}%`
}

export function TicketAiAssessmentReviewPanel({
                                                  ticketId,
                                                  currentUserId,
                                                  onAssessmentUpdated,
                                              }: TicketAiAssessmentReviewPanelProps) {
    const [
        assessment,
        setAssessment,
    ] = useState<SupportTicketAiAssessment | null>(
        null
    )

    const [
        action,
        setAction,
    ] = useState<AssessmentAction>('load')

    const [
        errorMessage,
        setErrorMessage,
    ] = useState<string | null>(null)

    const [
        successMessage,
        setSuccessMessage,
    ] = useState<string | null>(null)

    const [
        approvedResponse,
        setApprovedResponse,
    ] = useState('')

    const [
        relevanceConfirmed,
        setRelevanceConfirmed,
    ] = useState(false)

    const [
        relevanceNotes,
        setRelevanceNotes,
    ] = useState('')

    const [
        rejectionReason,
        setRejectionReason,
    ] = useState('')

    const [
        escalationReason,
        setEscalationReason,
    ] = useState('')

    const updateAssessment = useCallback(
        (
            updatedAssessment:
            SupportTicketAiAssessment,
        ) => {
            setAssessment(updatedAssessment)

            setApprovedResponse(
                updatedAssessment.approvedResponse
                ?? updatedAssessment.suggestedResponse
                ?? ''
            )

            setRelevanceConfirmed(
                updatedAssessment
                    .responseRelevanceConfirmed
            )

            setRelevanceNotes(
                updatedAssessment
                    .responseRelevanceConfirmationNotes
                ?? ''
            )

            onAssessmentUpdated?.(
                updatedAssessment
            )
        },
        [onAssessmentUpdated]
    )

    const loadAssessment = useCallback(
        async () => {
            setAction('load')
            setErrorMessage(null)
            setSuccessMessage(null)

            try {
                const loadedAssessment =
                    await getTicketAiAssessment(
                        ticketId
                    )

                updateAssessment(loadedAssessment)
            } catch (error) {
                const status =
                    (
                        error as {
                            response?: {
                                status?: number
                            }
                        }
                    ).response?.status

                if (status === 404) {
                    setAssessment(null)
                    setApprovedResponse('')
                    setRelevanceConfirmed(false)
                    setRelevanceNotes('')
                } else {
                    setErrorMessage(
                        getErrorMessage(
                            error,
                            'Unable to load the AI assessment.'
                        )
                    )
                }
            } finally {
                setAction(null)
            }
        },
        [
            ticketId,
            updateAssessment,
        ]
    )

    useEffect(() => {
        let isActive = true

        getTicketAiAssessment(ticketId)
            .then((loadedAssessment) => {
                if (!isActive) {
                    return
                }

                updateAssessment(loadedAssessment)
            })
            .catch((error: unknown) => {
                if (!isActive) {
                    return
                }

                const status =
                    (
                        error as {
                            response?: {
                                status?: number
                            }
                        }
                    ).response?.status

                if (status === 404) {
                    setAssessment(null)
                    setApprovedResponse('')
                    setRelevanceConfirmed(false)
                    setRelevanceNotes('')
                } else {
                    setErrorMessage(
                        getErrorMessage(
                            error,
                            'Unable to load the AI assessment.'
                        )
                    )
                }
            })
            .finally(() => {
                if (isActive) {
                    setAction(null)
                }
            })

        return () => {
            isActive = false
        }
    }, [
        ticketId,
        updateAssessment,
    ])

    async function handleGenerateAssessment() {
        setAction('generate')
        setErrorMessage(null)
        setSuccessMessage(null)

        try {
            const generatedAssessment =
                await generateTicketAiAssessment(
                    ticketId
                )

            updateAssessment(generatedAssessment)

            setSuccessMessage(
                'The AI assessment was generated successfully.'
            )
        } catch (error) {
            setErrorMessage(
                getErrorMessage(
                    error,
                    'Unable to generate the AI assessment.'
                )
            )
        } finally {
            setAction(null)
        }
    }

    async function handleApproveAssessment() {
        if (!assessment) {
            return
        }

        const trimmedResponse =
            approvedResponse.trim()

        const trimmedNotes =
            relevanceNotes.trim()

        if (!relevanceConfirmed) {
            setErrorMessage(
                'Confirm that the response addresses '
                + 'the ticket subject and description.'
            )
            return
        }

        if (trimmedNotes.length === 0) {
            setErrorMessage(
                'Response relevance confirmation notes '
                + 'are required.'
            )
            return
        }

        if (trimmedNotes.length > 1000) {
            setErrorMessage(
                'Response relevance confirmation notes '
                + 'cannot exceed 1000 characters.'
            )
            return
        }

        if (trimmedResponse.length > 4000) {
            setErrorMessage(
                'The approved response cannot exceed '
                + '4000 characters.'
            )
            return
        }

        setAction('approve')
        setErrorMessage(null)
        setSuccessMessage(null)

        try {
            const approvedAssessment =
                await approveTicketAiAssessment(
                    assessment.id,
                    {
                        approvedResponse:
                            trimmedResponse.length > 0
                                ? trimmedResponse
                                : null,
                        responseRelevanceConfirmed:
                        relevanceConfirmed,
                        responseRelevanceConfirmationNotes:
                        trimmedNotes,
                    }
                )

            updateAssessment(approvedAssessment)

            setSuccessMessage(
                'The assessment was approved. '
                + 'A different authorised user must '
                + 'publish the response.'
            )
        } catch (error) {
            setErrorMessage(
                getErrorMessage(
                    error,
                    'Unable to approve the assessment.'
                )
            )
        } finally {
            setAction(null)
        }
    }

    async function handleRejectAssessment() {
        if (!assessment) {
            return
        }

        const trimmedReason =
            rejectionReason.trim()

        if (trimmedReason.length === 0) {
            setErrorMessage(
                'A rejection reason is required.'
            )
            return
        }

        if (trimmedReason.length > 1500) {
            setErrorMessage(
                'The rejection reason cannot exceed '
                + '1500 characters.'
            )
            return
        }

        setAction('reject')
        setErrorMessage(null)
        setSuccessMessage(null)

        try {
            const rejectedAssessment =
                await rejectTicketAiAssessment(
                    assessment.id,
                    {
                        rejectionReason:
                        trimmedReason,
                    }
                )

            updateAssessment(rejectedAssessment)
            setRejectionReason('')

            setSuccessMessage(
                'The assessment was rejected.'
            )
        } catch (error) {
            setErrorMessage(
                getErrorMessage(
                    error,
                    'Unable to reject the assessment.'
                )
            )
        } finally {
            setAction(null)
        }
    }

    async function handleEscalateAssessment() {
        if (!assessment) {
            return
        }

        const trimmedReason =
            escalationReason.trim()

        if (trimmedReason.length === 0) {
            setErrorMessage(
                'An escalation reason is required.'
            )
            return
        }

        if (trimmedReason.length > 1500) {
            setErrorMessage(
                'The escalation reason cannot exceed '
                + '1500 characters.'
            )
            return
        }

        setAction('escalate')
        setErrorMessage(null)
        setSuccessMessage(null)

        try {
            const escalatedAssessment =
                await escalateTicketAiAssessment(
                    assessment.id,
                    {
                        escalationReason:
                        trimmedReason,
                    }
                )

            updateAssessment(escalatedAssessment)
            setEscalationReason('')

            setSuccessMessage(
                'The assessment was escalated '
                + 'for human handling.'
            )
        } catch (error) {
            setErrorMessage(
                getErrorMessage(
                    error,
                    'Unable to escalate the assessment.'
                )
            )
        } finally {
            setAction(null)
        }
    }

    async function handlePublishResponse() {
        if (!assessment) {
            return
        }

        if (
            assessment.approvedByUserId
            === currentUserId
        ) {
            setErrorMessage(
                'The user who approved this response '
                + 'cannot publish it.'
            )
            return
        }

        setAction('publish')
        setErrorMessage(null)
        setSuccessMessage(null)

        try {
            const publishedAssessment =
                await publishTicketAiAssessmentResponse(
                    assessment.id
                )

            updateAssessment(publishedAssessment)

            setSuccessMessage(
                'The approved clarification response '
                + 'was published successfully.'
            )
        } catch (error) {
            setErrorMessage(
                getErrorMessage(
                    error,
                    'Unable to publish the approved response.'
                )
            )
        } finally {
            setAction(null)
        }
    }

    const isBusy = action !== null

    if (action === 'load') {
        return (
            <section
                className="ticket-ai-assessment-panel"
                aria-busy="true"
            >
                <h2>AI clarification assessment</h2>
                <p>Loading assessment...</p>
            </section>
        )
    }

    if (!assessment) {
        return (
            <section
                className="ticket-ai-assessment-panel"
            >
                <div
                    className={
                        'ticket-ai-assessment-panel__header'
                    }
                >
                    <div>
                        <h2>
                            AI clarification assessment
                        </h2>

                        <p>
                            Generate an assessment before
                            reviewing the proposed response.
                        </p>
                    </div>
                </div>

                {errorMessage && (
                    <div
                        className="form-error"
                        role="alert"
                    >
                        {errorMessage}
                    </div>
                )}

                <button
                    type="button"
                    className="primary-button"
                    disabled={isBusy}
                    onClick={
                        handleGenerateAssessment
                    }
                >
                    {action === 'generate'
                        ? 'Generating...'
                        : 'Generate AI assessment'}
                </button>
            </section>
        )
    }

    const canApprove =
        canApproveAiAssessment(assessment)

    const canReject =
        canRejectAiAssessment(assessment)

    const canEscalate =
        canEscalateAiAssessment(assessment)

    const isApprovedByCurrentUser =
        currentUserId !== null
        && assessment.approvedByUserId
        === currentUserId

    const canPublish =
        canPublishAiAssessment(
            assessment,
            currentUserId
        )

    const hasPublishableStatus =
        assessment.assessmentStatus
        === 'APPROVED'
        || assessment.assessmentStatus
        === 'EDITED_AND_APPROVED'

    return (
        <section
            className="ticket-ai-assessment-panel"
            aria-busy={isBusy}
        >
            <div
                className={
                    'ticket-ai-assessment-panel__header'
                }
            >
                <div>
                    <h2>
                        AI clarification assessment
                    </h2>

                    <p>
                        Reference:{' '}
                        <strong>
                            {
                                assessment
                                    .assessmentReference
                            }
                        </strong>
                    </p>
                </div>

                <TicketAiAssessmentStatusBadge
                    status={
                        assessment.assessmentStatus
                    }
                />
            </div>

            {errorMessage && (
                <div
                    className="form-error"
                    role="alert"
                >
                    {errorMessage}
                </div>
            )}

            {successMessage && (
                <div
                    className="form-success"
                    role="status"
                >
                    {successMessage}
                </div>
            )}

            <div
                className={
                    'ticket-ai-assessment-panel__metadata'
                }
            >
                <div>
                    <span>Category</span>
                    <strong>
                        {formatAiDetectedCategory(
                            assessment.detectedCategory
                        )}
                    </strong>
                </div>

                <div>
                    <span>Priority</span>
                    <strong>
                        {
                            AI_ASSESSMENT_PRIORITY_LABELS[
                                assessment.priority
                                ]
                        }
                    </strong>
                </div>

                <div>
                    <span>Resolution mode</span>
                    <strong>
                        {
                            AI_RESOLUTION_MODE_LABELS[
                                assessment
                                    .resolutionMode
                                ]
                        }
                    </strong>
                </div>

                <div>
                    <span>Confidence</span>
                    <strong>
                        {formatConfidenceScore(
                            assessment.confidenceScore
                        )}
                    </strong>
                </div>

                <div>
                    <span>Model version</span>
                    <strong>
                        {assessment.modelVersion
                            ?? 'Not available'}
                    </strong>
                </div>

                <div>
                    <span>Generated</span>
                    <strong>
                        {formatDateTime(
                            assessment.createdAt
                        )}
                    </strong>
                </div>
            </div>

            <div
                className={
                    'ticket-ai-assessment-panel__section'
                }
            >
                <h3>Assessment summary</h3>
                <p>
                    {assessment.summary}
                </p>
            </div>

            {assessment.escalationReason && (
                <div
                    className={
                        'ticket-ai-assessment-panel__section'
                    }
                >
                    <h3>Escalation reason</h3>
                    <p>
                        {assessment.escalationReason}
                    </p>
                </div>
            )}

            {assessment.rejectionReason && (
                <div
                    className={
                        'ticket-ai-assessment-panel__section'
                    }
                >
                    <h3>Rejection reason</h3>
                    <p>
                        {assessment.rejectionReason}
                    </p>
                </div>
            )}

            {canApprove && (
                <div
                    className={
                        'ticket-ai-assessment-panel__section'
                    }
                >
                    <h3>Review proposed response</h3>

                    <label
                        htmlFor={
                            'ai-assessment-approved-response'
                        }
                    >
                        Approved response
                    </label>

                    <textarea
                        id={
                            'ai-assessment-approved-response'
                        }
                        rows={10}
                        maxLength={4000}
                        value={approvedResponse}
                        onChange={(event) =>
                            setApprovedResponse(
                                event.target.value
                            )
                        }
                        disabled={isBusy}
                    />

                    <div className="form-help">
                        {approvedResponse.length}/4000
                    </div>

                    <label
                        className="checkbox-field"
                        htmlFor={
                            'ai-assessment-relevance-confirmed'
                        }
                    >
                        <input
                            id={
                                'ai-assessment-relevance-confirmed'
                            }
                            type="checkbox"
                            checked={
                                relevanceConfirmed
                            }
                            onChange={(event) =>
                                setRelevanceConfirmed(
                                    event.target.checked
                                )
                            }
                            disabled={isBusy}
                        />

                        <span>
                            I confirm that the approved
                            response addresses the ticket
                            subject and description.
                        </span>
                    </label>

                    <label
                        htmlFor={
                            'ai-assessment-relevance-notes'
                        }
                    >
                        Relevance confirmation notes
                    </label>

                    <textarea
                        id={
                            'ai-assessment-relevance-notes'
                        }
                        rows={4}
                        maxLength={1000}
                        value={relevanceNotes}
                        onChange={(event) =>
                            setRelevanceNotes(
                                event.target.value
                            )
                        }
                        disabled={isBusy}
                    />

                    <div className="form-help">
                        {relevanceNotes.length}/1000
                    </div>

                    <button
                        type="button"
                        className="primary-button"
                        disabled={isBusy}
                        onClick={
                            handleApproveAssessment
                        }
                    >
                        {action === 'approve'
                            ? 'Approving...'
                            : 'Approve response'}
                    </button>
                </div>
            )}

            {canReject && (
                <div
                    className={
                        'ticket-ai-assessment-panel__section'
                    }
                >
                    <h3>Reject assessment</h3>

                    <label
                        htmlFor={
                            'ai-assessment-rejection-reason'
                        }
                    >
                        Rejection reason
                    </label>

                    <textarea
                        id={
                            'ai-assessment-rejection-reason'
                        }
                        rows={4}
                        maxLength={1500}
                        value={rejectionReason}
                        onChange={(event) =>
                            setRejectionReason(
                                event.target.value
                            )
                        }
                        disabled={isBusy}
                    />

                    <div className="form-help">
                        {rejectionReason.length}/1500
                    </div>

                    <button
                        type="button"
                        className="danger-button"
                        disabled={isBusy}
                        onClick={
                            handleRejectAssessment
                        }
                    >
                        {action === 'reject'
                            ? 'Rejecting...'
                            : 'Reject assessment'}
                    </button>
                </div>
            )}

            {canEscalate && (
                <div
                    className={
                        'ticket-ai-assessment-panel__section'
                    }
                >
                    <h3>Escalate assessment</h3>

                    <label
                        htmlFor={
                            'ai-assessment-escalation-reason'
                        }
                    >
                        Escalation reason
                    </label>

                    <textarea
                        id={
                            'ai-assessment-escalation-reason'
                        }
                        rows={4}
                        maxLength={1500}
                        value={escalationReason}
                        onChange={(event) =>
                            setEscalationReason(
                                event.target.value
                            )
                        }
                        disabled={isBusy}
                    />

                    <div className="form-help">
                        {escalationReason.length}/1500
                    </div>

                    <button
                        type="button"
                        className="secondary-button"
                        disabled={isBusy}
                        onClick={
                            handleEscalateAssessment
                        }
                    >
                        {action === 'escalate'
                            ? 'Escalating...'
                            : 'Escalate for human handling'}
                    </button>
                </div>
            )}

            {hasPublishableStatus && (
                <div
                    className={
                        'ticket-ai-assessment-panel__section'
                    }
                >
                    <h3>Publish approved response</h3>

                    <p>
                        {assessment.approvedResponse}
                    </p>

                    <dl
                        className={
                            'ticket-ai-assessment-panel__audit'
                        }
                    >
                        <div>
                            <dt>Approved by user</dt>
                            <dd>
                                {
                                    assessment
                                        .approvedByUserId
                                    ?? 'Not recorded'
                                }
                            </dd>
                        </div>

                        <div>
                            <dt>Approved at</dt>
                            <dd>
                                {formatDateTime(
                                    assessment.approvedAt
                                )}
                            </dd>
                        </div>

                        <div>
                            <dt>
                                Relevance confirmation
                            </dt>
                            <dd>
                                {
                                    assessment
                                        .responseRelevanceConfirmationNotes
                                    ?? 'Not recorded'
                                }
                            </dd>
                        </div>
                    </dl>

                    {isApprovedByCurrentUser && (
                        <div
                            className="form-warning"
                            role="status"
                        >
                            You approved this response.
                            A different authorised user
                            must publish it.
                        </div>
                    )}

                    <button
                        type="button"
                        className="primary-button"
                        disabled={
                            isBusy || !canPublish
                        }
                        onClick={
                            handlePublishResponse
                        }
                    >
                        {action === 'publish'
                            ? 'Publishing...'
                            : 'Publish official clarification'}
                    </button>
                </div>
            )}

            {assessment.assessmentStatus
                === 'RESPONSE_PUBLISHED' && (
                    <div
                        className={
                            'ticket-ai-assessment-panel__section'
                        }
                    >
                        <h3>Publication record</h3>

                        <p>
                            Published by user{' '}
                            <strong>
                                {
                                    assessment
                                        .publishedByUserId
                                    ?? 'Not recorded'
                                }
                            </strong>
                            {' '}on{' '}
                            <strong>
                                {formatDateTime(
                                    assessment.publishedAt
                                )}
                            </strong>.
                        </p>
                    </div>
                )}

            <div
                className={
                    'ticket-ai-assessment-panel__footer'
                }
            >
                <button
                    type="button"
                    className="secondary-button"
                    disabled={isBusy}
                    onClick={() =>
                        void loadAssessment()
                    }
                >
                    Refresh assessment
                </button>
            </div>
        </section>
    )
}