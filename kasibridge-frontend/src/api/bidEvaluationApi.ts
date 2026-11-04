import axios from 'axios'

import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    BidEvaluation,
    BlindEvaluationBid,
    EvaluateBidRequest,
    EvaluationApiErrorResponse,
} from '../types/bidEvaluation'

export async function getBlindBidsForEvaluation(
    tenderId: number,
): Promise<BlindEvaluationBid[]> {
    const validTenderId =
        requirePositiveIdentifier(
            tenderId,
            'Tender ID'
        )

    const response =
        await procurementApiClient
            .get<BlindEvaluationBid[]>(
                '/api/v1/tenders/'
                + validTenderId
                + '/evaluation/bids'
            )

    return response.data
}

export async function scoreBlindBid(
    tenderId: number,
    bidId: number,
    request: EvaluateBidRequest,
): Promise<BidEvaluation> {
    const validTenderId =
        requirePositiveIdentifier(
            tenderId,
            'Tender ID'
        )

    const validBidId =
        requirePositiveIdentifier(
            bidId,
            'Bid ID'
        )

    const validRequest =
        normalizeEvaluateBidRequest(
            request
        )

    const response =
        await procurementApiClient
            .post<BidEvaluation>(
                '/api/v1/tenders/'
                + validTenderId
                + '/evaluation/bids/'
                + validBidId
                + '/score',
                validRequest
            )

    return response.data
}

export function getBidEvaluationApiErrorMessage(
    error: unknown,
    fallbackMessage:
    string = (
        'Unable to complete the '
        + 'bid evaluation request.'
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
            EvaluationApiErrorResponse
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

    const fieldErrors =
        error.response?.data?.fieldErrors

    if (
        Array.isArray(fieldErrors)
        && fieldErrors.length > 0
    ) {
        return fieldErrors
            .map(
                (fieldError) => {
                    const fieldName =
                        fieldError.field
                            .trim()

                    const message =
                        fieldError.message
                            .trim()

                    if (fieldName.length === 0) {
                        return message
                    }

                    return (
                        fieldName
                        + ': '
                        + message
                    )
                }
            )
            .filter(
                (message) =>
                    message.length > 0
            )
            .join(' ')
    }

    if (
        error.code === 'ECONNABORTED'
    ) {
        return (
            'The evaluation request timed '
            + 'out. Please try again.'
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
                'The bid evaluation contains '
                + 'invalid information. Confirm '
                + 'the score and comments.'
            )

        case 401:
            return (
                'The current session is no '
                + 'longer authenticated. Sign '
                + 'in again before continuing.'
            )

        case 403:
            return (
                'The authenticated evaluator '
                + 'is not authorised to access '
                + 'or score this tender.'
            )

        case 404:
            return (
                'The requested tender or blind '
                + 'bid could not be found.'
            )

        case 409:
            return (
                'The bid cannot be scored in '
                + 'its current workflow state, '
                + 'or an evaluation already '
                + 'exists.'
            )

        case 422:
            return (
                'The evaluation request could '
                + 'not be processed. Confirm '
                + 'the score and comments.'
            )

        default:
            return fallbackMessage
    }
}

export function isBidEvaluationForbidden(
    error: unknown,
): boolean {
    return (
        axios.isAxiosError(error)
        && error.response?.status === 403
    )
}

export function isBidEvaluationConflict(
    error: unknown,
): boolean {
    return (
        axios.isAxiosError(error)
        && error.response?.status === 409
    )
}

export function isBidEvaluationNotFound(
    error: unknown,
): boolean {
    return (
        axios.isAxiosError(error)
        && error.response?.status === 404
    )
}

function normalizeEvaluateBidRequest(
    request: EvaluateBidRequest,
): EvaluateBidRequest {
    if (
        request === null
        || request === undefined
    ) {
        throw new Error(
            'A bid evaluation request '
            + 'is required.'
        )
    }

    const technicalScore =
        request.technicalScore

    if (
        !Number.isFinite(
            technicalScore
        )
        || technicalScore < 0
        || technicalScore > 100
    ) {
        throw new Error(
            'Technical score must be '
            + 'between 0 and 100.'
        )
    }

    const comments =
        request.comments.trim()

    if (comments.length < 10) {
        throw new Error(
            'Evaluation comments must '
            + 'contain at least '
            + '10 characters.'
        )
    }

    if (comments.length > 1000) {
        throw new Error(
            'Evaluation comments cannot '
            + 'exceed 1000 characters.'
        )
    }

    return {
        technicalScore,
        comments,
    }
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