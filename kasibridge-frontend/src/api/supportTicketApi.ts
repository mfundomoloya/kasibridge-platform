import axios from 'axios'

import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    ApiErrorResponse,
} from '../types/bid'

import type {
    CreateSupportTicketRequest,
    SupportTicket,
    SupportTicketApiError,
    SupportTicketPage,
} from '../types/supportTicket'

export interface GetMyTicketsParameters {
    page?: number
    size?: number
    sort?: string
}

export async function createSupportTicket(
    tenderId: number,
    request: CreateSupportTicketRequest,
): Promise<SupportTicket> {
    const response =
        await procurementApiClient.post<SupportTicket>(
            `/api/v1/tenders/${tenderId}/tickets`,
            request,
        )

    return response.data
}

export async function getMySupportTickets(
    parameters: GetMyTicketsParameters = {},
): Promise<SupportTicketPage> {
    const response =
        await procurementApiClient.get<SupportTicketPage>(
            '/api/v1/tickets/my',
            {
                params: {
                    page: parameters.page ?? 0,
                    size: parameters.size ?? 20,
                    sort:
                        parameters.sort
                        ?? 'createdAt,desc',
                },
            },
        )

    return response.data
}

export async function getSupportTicketById(
    ticketId: number,
): Promise<SupportTicket> {
    const response =
        await procurementApiClient.get<SupportTicket>(
            `/api/v1/tickets/${ticketId}`,
        )

    return response.data
}

export function getSupportTicketApiError(
    error: unknown,
): SupportTicketApiError {
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
                    || 'The support ticket request could not be completed.',
                fieldErrors:
                    error.response.data.fieldErrors
                    ?? null,
            }
        }

        if (error.code === 'ECONNABORTED') {
            return {
                status: 0,
                message:
                    'The support ticket request timed out. '
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
                || 'The support ticket request could not be completed.',
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
            'An unexpected support ticket error occurred.',
        fieldErrors: null,
    }
}