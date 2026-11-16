import axios from 'axios'

import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    ProcurementApiErrorResponse,
} from '../types/procurementTender'

import type {
    ProcurementAnomalyPage,
    ProcurementAnomalyRecord,
    ReviewProcurementAnomalyRequest,
} from '../types/procurementAnomaly'

export async function getTenderAnomalyRecords(
    tenderId: number,
): Promise<ProcurementAnomalyPage> {
    const validTenderId =
        requirePositiveTenderId(
            tenderId
        )

    const response =
        await procurementApiClient
            .get<ProcurementAnomalyPage>(
                `/api/v1/tenders/${validTenderId}`
                + '/anomaly-records',
                {
                    params: {
                        page: 0,
                        size: 100,
                        sort: 'detectedAt,desc',
                    },
                }
            )

    return response.data
}

export async function markProcurementAnomalyReviewed(
    anomalyId: number,
    request: ReviewProcurementAnomalyRequest,
): Promise<ProcurementAnomalyRecord> {
    const validAnomalyId =
        requirePositiveAnomalyId(
            anomalyId
        )

    const validRequest =
        normalizeReviewRequest(
            request
        )

    const response =
        await procurementApiClient
            .patch<ProcurementAnomalyRecord>(
                '/api/v1/procurement/anomalies/'
                + `${validAnomalyId}/review`,
                validRequest
            )

    return response.data
}

export async function dismissProcurementAnomaly(
    anomalyId: number,
    request: ReviewProcurementAnomalyRequest,
): Promise<ProcurementAnomalyRecord> {
    const validAnomalyId =
        requirePositiveAnomalyId(
            anomalyId
        )

    const validRequest =
        normalizeReviewRequest(
            request
        )

    const response =
        await procurementApiClient
            .patch<ProcurementAnomalyRecord>(
                '/api/v1/procurement/anomalies/'
                + `${validAnomalyId}/dismiss`,
                validRequest
            )

    return response.data
}

export function getProcurementAnomalyApiErrorMessage(
    error: unknown,
    fallbackMessage:
    string = (
        'Unable to complete the procurement '
        + 'anomaly request.'
    ),
): string {
    if (
        error instanceof Error
        && !axios.isAxiosError(error)
    ) {
        const localMessage =
            error.message.trim()

        return localMessage.length > 0
            ? localMessage
            : fallbackMessage
    }

    if (
        !axios.isAxiosError<
            ProcurementApiErrorResponse
        >(error)
    ) {
        return fallbackMessage
    }

    const responseMessage =
        error.response?.data?.message

    if (
        typeof responseMessage === 'string'
        && responseMessage.trim().length > 0
    ) {
        return responseMessage.trim()
    }

    const fieldErrorMessage =
        getFieldErrorMessage(
            error.response
                ?.data
                ?.fieldErrors
        )

    if (fieldErrorMessage !== null) {
        return fieldErrorMessage
    }

    if (error.code === 'ECONNABORTED') {
        return (
            'The anomaly request timed out. '
            + 'Refresh the anomaly record before '
            + 'trying the action again.'
        )
    }

    if (!error.response) {
        return (
            'The procurement service is '
            + 'unavailable. Confirm that it '
            + 'is running and try again.'
        )
    }

    switch (error.response.status) {
        case 400:
            return (
                'The anomaly review request '
                + 'contains invalid information.'
            )

        case 401:
            return (
                'The current session is no '
                + 'longer authenticated. Sign '
                + 'in again before continuing.'
            )

        case 403:
            return (
                'The authenticated user is not '
                + 'assigned as an Adjudicator '
                + 'for this anomaly’s tender.'
            )

        case 404:
            return (
                'The requested procurement '
                + 'anomaly could not be found.'
            )

        case 409:
            return (
                'This anomaly has already been '
                + 'reviewed or dismissed. Refresh '
                + 'the workspace before continuing.'
            )

        default:
            return fallbackMessage
    }
}

function requirePositiveTenderId(
    tenderId: number,
): number {
    if (
        !Number.isInteger(tenderId)
        || tenderId <= 0
    ) {
        throw new Error(
            'A valid tender ID is required.'
        )
    }

    return tenderId
}

function requirePositiveAnomalyId(
    anomalyId: number,
): number {
    if (
        !Number.isInteger(anomalyId)
        || anomalyId <= 0
    ) {
        throw new Error(
            'A valid procurement anomaly ID '
            + 'is required.'
        )
    }

    return anomalyId
}

function normalizeReviewRequest(
    request: ReviewProcurementAnomalyRequest,
): ReviewProcurementAnomalyRequest {
    if (
        request === null
        || request === undefined
    ) {
        throw new Error(
            'An anomaly review request '
            + 'is required.'
        )
    }

    const reviewNotes =
        request.reviewNotes?.trim()
        ?? ''

    if (reviewNotes.length === 0) {
        throw new Error(
            'Review notes are required.'
        )
    }

    if (reviewNotes.length > 1000) {
        throw new Error(
            'Review notes cannot exceed '
            + '1000 characters.'
        )
    }

    return {
        reviewNotes,
    }
}

function getFieldErrorMessage(
    fieldErrors:
    ProcurementApiErrorResponse[
        'fieldErrors'
        ],
): string | null {
    if (
        fieldErrors === null
        || fieldErrors === undefined
    ) {
        return null
    }

    if (Array.isArray(fieldErrors)) {
        const messages =
            fieldErrors
                .map(
                    (fieldError) => {
                        const field =
                            fieldError.field.trim()

                        const message =
                            fieldError.message.trim()

                        if (message.length === 0) {
                            return ''
                        }

                        return field.length === 0
                            ? message
                            : `${field}: ${message}`
                    }
                )
                .filter(
                    (message) =>
                        message.length > 0
                )

        return messages.length === 0
            ? null
            : messages.join(' ')
    }

    const messages =
        Object.entries(fieldErrors)
            .map(
                ([field, message]) => {
                    const cleanField =
                        field.trim()

                    const cleanMessage =
                        message.trim()

                    if (
                        cleanMessage.length === 0
                    ) {
                        return ''
                    }

                    return cleanField.length === 0
                        ? cleanMessage
                        : (
                            `${cleanField}: `
                            + cleanMessage
                        )
                }
            )
            .filter(
                (message) =>
                    message.length > 0
            )

    return messages.length === 0
        ? null
        : messages.join(' ')
}