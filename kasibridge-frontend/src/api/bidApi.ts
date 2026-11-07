import axios from 'axios'

import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    ApiErrorResponse,
    Bid,
    TraderBidOutcome,
} from '../types/bid'

export interface SubmitBidRequest {
    technicalProposal: string
    priceAmount: number
    csdValid: boolean
    taxClearanceValid: boolean
    bbbeeValid: boolean
    requiredDocumentsUploaded: boolean
}

export interface BidApiError {
    status: number
    message: string
    fieldErrors: Record<string, string> | null
}

export async function submitBid(
    tenderId: number,
    request: SubmitBidRequest,
): Promise<Bid> {
    const response =
        await procurementApiClient.post<Bid>(
            `/api/v1/tenders/${tenderId}/bids`,
            request,
        )

    return response.data
}

export async function getMyBids(): Promise<Bid[]> {
    const response =
        await procurementApiClient.get<Bid[]>(
            '/api/v1/bids/my',
        )

    return response.data
}

export async function withdrawBid(
    bidId: number,
): Promise<Bid> {
    const response =
        await procurementApiClient.patch<Bid>(
            `/api/v1/bids/${bidId}/withdraw`,
        )

    return response.data
}

export function getBidApiError(
    error: unknown,
): BidApiError {
    if (
        error instanceof Error
        && !axios.isAxiosError(error)
    ) {
        const message =
            error.message.trim()

        return {
            status: 0,
            message:
                message.length > 0
                    ? message
                    : (
                        'The bid request could '
                        + 'not be completed.'
                    ),
            fieldErrors: null,
        }
    }

    if (
        axios.isAxiosError<ApiErrorResponse>(
            error,
        )
    ) {
        if (error.response?.data) {
            return {
                status:
                error.response.data.status,
                message:
                    error.response.data.message
                    || 'The bid request could not be completed.',
                fieldErrors:
                    error.response.data.fieldErrors
                    ?? null,
            }
        }

        if (error.code === 'ECONNABORTED') {
            return {
                status: 0,
                message:
                    'The bid request timed out. '
                    + 'Please try again.',
                fieldErrors: null,
            }
        }

        if (!error.response) {
            return {
                status: 0,
                message:
                    'Unable to connect to the '
                    + 'procurement service.',
                fieldErrors: null,
            }
        }

        return {
            status: error.response.status,
            message:
                error.message
                || 'The bid request could not be completed.',
            fieldErrors: null,
        }
    }

    if (error instanceof Error) {
        return {
            status: 0,
            message: error.message,
            fieldErrors: null,
        }
    }

    return {
        status: 0,
        message:
            'An unexpected bid error occurred.',
        fieldErrors: null,
    }
}

export async function getCurrentTraderBidOutcome(
    bidId: number,
): Promise<TraderBidOutcome> {
    const validBidId =
        requirePositiveIdentifier(
            bidId,
            'Bid ID'
        )

    const response =
        await procurementApiClient
            .get<TraderBidOutcome>(
                '/api/v1/bids/'
                + validBidId
                + '/outcome'
            )

    return response.data
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