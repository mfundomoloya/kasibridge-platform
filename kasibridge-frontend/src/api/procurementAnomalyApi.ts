import axios from 'axios'

import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    ProcurementApiErrorResponse,
} from '../types/procurementTender'

import type {
    ProcurementAnomalyPage,
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

export function getProcurementAnomalyApiErrorMessage(
    error: unknown,
    fallbackMessage:
    string = (
        'Unable to load procurement '
        + 'anomaly records.'
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

    if (!error.response) {
        return (
            'The procurement service is '
            + 'unavailable. Confirm that it '
            + 'is running and try again.'
        )
    }

    switch (error.response.status) {
        case 401:
            return (
                'The current session is no '
                + 'longer authenticated. Sign '
                + 'in again before continuing.'
            )

        case 403:
            return (
                'The authenticated user is not '
                + 'authorised to view anomaly '
                + 'records for this tender.'
            )

        case 404:
            return (
                'The requested tender could '
                + 'not be found.'
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