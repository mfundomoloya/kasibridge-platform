import axios from 'axios'

import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    AdjudicationApiErrorResponse,
    AwardTenderRequest,
    AwardTenderResponse,
    BidRanking,
} from '../types/adjudication'

export async function getAdjudicationSummary(
    tenderId: number,
): Promise<BidRanking[]> {
    const validTenderId =
        requirePositiveIdentifier(
            tenderId,
            'Tender ID'
        )

    const response =
        await procurementApiClient
            .get<BidRanking[]>(
                '/api/v1/tenders/'
                + validTenderId
                + '/adjudication/summary'
            )

    return response.data
}

export async function awardTender(
    tenderId: number,
    request: AwardTenderRequest,
): Promise<AwardTenderResponse> {
    const validTenderId =
        requirePositiveIdentifier(
            tenderId,
            'Tender ID'
        )

    const validRequest =
        normalizeAwardTenderRequest(
            request
        )

    const response =
        await procurementApiClient
            .post<AwardTenderResponse>(
                '/api/v1/tenders/'
                + validTenderId
                + '/adjudication/award',
                validRequest
            )

    return response.data
}

export function getAdjudicationApiErrorMessage(
    error: unknown,
    fallbackMessage:
    string = (
        'Unable to complete the '
        + 'adjudication request.'
    ),
): string {
    if (
        error instanceof Error
        && !axios.isAxiosError(error)
    ) {
        const localMessage =
            error.message.trim()

        if (localMessage.length > 0) {
            return localMessage
        }

        return fallbackMessage
    }

    if (
        !axios.isAxiosError<
            AdjudicationApiErrorResponse
        >(error)
    ) {
        return fallbackMessage
    }

    const responseMessage =
        error.response?.data?.message

    if (
        typeof responseMessage === 'string'
        && responseMessage
            .trim()
            .length > 0
    ) {
        return responseMessage.trim()
    }

    const fieldErrorMessage =
        getFieldErrorMessage(
            error.response
                ?.data
                ?.fieldErrors
        )

    if (
        fieldErrorMessage !== null
    ) {
        return fieldErrorMessage
    }

    if (
        error.code === 'ECONNABORTED'
    ) {
        return (
            'The adjudication request '
            + 'timed out. Check whether '
            + 'the award was recorded '
            + 'before trying again.'
        )
    }

    if (!error.response) {
        return (
            'The procurement service is '
            + 'unavailable. Confirm that '
            + 'the service is running and '
            + 'try again.'
        )
    }

    switch (error.response.status) {
        case 400:
            return (
                'The adjudication request '
                + 'contains invalid '
                + 'information. Confirm the '
                + 'selected bid and award '
                + 'reason.'
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
                + 'for this tender.'
            )

        case 404:
            return (
                'The selected tender or ranked '
                + 'bid could not be found.'
            )

        case 409:
            return (
                'The adjudication action '
                + 'conflicts with the current '
                + 'tender state. Refresh the '
                + 'workspace before retrying.'
            )

        case 422:
            return (
                'The award decision could not '
                + 'be processed. Confirm the '
                + 'selected bid and supporting '
                + 'reason.'
            )

        default:
            return fallbackMessage
    }
}

export function isAdjudicationForbidden(
    error: unknown,
): boolean {
    return (
        axios.isAxiosError(error)
        && error.response?.status === 403
    )
}

export function isAdjudicationConflict(
    error: unknown,
): boolean {
    return (
        axios.isAxiosError(error)
        && error.response?.status === 409
    )
}

export function isAdjudicationNotFound(
    error: unknown,
): boolean {
    return (
        axios.isAxiosError(error)
        && error.response?.status === 404
    )
}

function normalizeAwardTenderRequest(
    request: AwardTenderRequest,
): AwardTenderRequest {
    if (
        request === null
        || request === undefined
    ) {
        throw new Error(
            'An award request is required.'
        )
    }

    const winningBidId =
        requirePositiveIdentifier(
            request.winningBidId,
            'Winning bid ID'
        )

    const awardReason =
        request.awardReason.trim()

    if (awardReason.length < 10) {
        throw new Error(
            'The award reason must contain '
            + 'at least 10 characters.'
        )
    }

    if (awardReason.length > 1000) {
        throw new Error(
            'The award reason cannot exceed '
            + '1000 characters.'
        )
    }

    return {
        winningBidId,
        awardReason,
    }
}

function getFieldErrorMessage(
    fieldErrors:
    AdjudicationApiErrorResponse[
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
                            fieldError
                                .field
                                .trim()

                        const message =
                            fieldError
                                .message
                                .trim()

                        if (
                            message.length === 0
                        ) {
                            return ''
                        }

                        if (
                            field.length === 0
                        ) {
                            return message
                        }

                        return (
                            field
                            + ': '
                            + message
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

    const messages =
        Object.entries(
            fieldErrors
        )
            .map(
                ([
                     field,
                     message,
                 ]) => {
                    const cleanField =
                        field.trim()

                    const cleanMessage =
                        message.trim()

                    if (
                        cleanMessage.length
                        === 0
                    ) {
                        return ''
                    }

                    if (
                        cleanField.length
                        === 0
                    ) {
                        return cleanMessage
                    }

                    return (
                        cleanField
                        + ': '
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

function requirePositiveIdentifier(
    value: number,
    fieldName: string,
): number {
    if (
        !Number.isInteger(value)
        || value <= 0
    ) {
        throw new Error(
            fieldName
            + ' must be a positive integer.'
        )
    }

    return value
}