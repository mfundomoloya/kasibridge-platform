import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    SupportTicket,
    SupportTicketPage,
    SupportTicketStatus,
} from '../types/supportTicket'

export interface ProcurementTicketQuery {
    page?: number
    size?: number
    sort?: string
}

export interface RespondToTicketRequest {
    response: string
    publicClarification: boolean
}

export interface RejectSupportTicketRequest {
    rejectionReason: string
}

export interface CloseTicketRequest {
    closureNotes: string
}

export interface ReturnTicketToQueueRequest {
    reason: string
}

export interface AssignSupportTicketRequest {
    assignedToUserId: number
    assignmentReason: string
}

function getPageParameters(
    query: ProcurementTicketQuery
) {
    return {
        page: query.page ?? 0,
        size: query.size ?? 20,
        sort:
            query.sort
            ?? 'assignedAt,desc',
    }
}

export async function getAssignedSupportTickets(
    query: ProcurementTicketQuery = {},
): Promise<SupportTicketPage> {
    const response =
        await procurementApiClient
            .get<SupportTicketPage>(
                '/api/v1/tickets/assigned-to-me',
                {
                    params:
                        getPageParameters(query),
                }
            )

    return response.data
}

export async function getAssignedSupportTicketsByStatus(
    status: SupportTicketStatus,
    query: ProcurementTicketQuery = {},
): Promise<SupportTicketPage> {
    const response =
        await procurementApiClient
            .get<SupportTicketPage>(
                '/api/v1/tickets/'
                + 'assigned-to-me/status/'
                + status,
                {
                    params:
                        getPageParameters(query),
                }
            )

    return response.data
}

export async function getUnassignedSupportTickets(
    query: ProcurementTicketQuery = {},
): Promise<SupportTicketPage> {
    const response =
        await procurementApiClient
            .get<SupportTicketPage>(
                '/api/v1/tickets/unassigned',
                {
                    params: {
                        page: query.page ?? 0,
                        size: query.size ?? 20,
                        sort:
                            query.sort
                            ?? 'createdAt,asc',
                    },
                }
            )

    return response.data
}

export async function assignSupportTicket(
    ticketId: number,
    request: AssignSupportTicketRequest,
): Promise<SupportTicket> {
    const response =
        await procurementApiClient
            .patch<SupportTicket>(
                `/api/v1/tickets/${ticketId}/assign`,
                request
            )

    return response.data
}

export async function startSupportTicketReview(
    ticketId: number,
): Promise<SupportTicket> {
    const response =
        await procurementApiClient
            .patch<SupportTicket>(
                `/api/v1/tickets/${ticketId}/start-review`
            )

    return response.data
}

export async function respondToSupportTicket(
    ticketId: number,
    request: RespondToTicketRequest,
): Promise<SupportTicket> {
    const response =
        await procurementApiClient
            .patch<SupportTicket>(
                `/api/v1/tickets/${ticketId}/respond`,
                request
            )

    return response.data
}

export async function rejectSupportTicket(
    ticketId: number,
    request: RejectSupportTicketRequest,
): Promise<SupportTicket> {
    const response =
        await procurementApiClient
            .patch<SupportTicket>(
                `/api/v1/tickets/${ticketId}/reject`,
                request
            )

    return response.data
}

export async function closeSupportTicket(
    ticketId: number,
    request: CloseTicketRequest,
): Promise<SupportTicket> {
    const response =
        await procurementApiClient
            .patch<SupportTicket>(
                `/api/v1/tickets/${ticketId}/close`,
                request
            )

    return response.data
}

export async function returnSupportTicketToQueue(
    ticketId: number,
    request: ReturnTicketToQueueRequest,
): Promise<SupportTicket> {
    const response =
        await procurementApiClient
            .patch<SupportTicket>(
                `/api/v1/tickets/${ticketId}/return-to-queue`,
                request
            )

    return response.data
}