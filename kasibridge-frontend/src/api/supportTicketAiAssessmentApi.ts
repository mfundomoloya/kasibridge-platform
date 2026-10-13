import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    ApproveTicketAiAssessmentRequest,
    EscalateTicketAiAssessmentRequest,
    RejectTicketAiAssessmentRequest,
    SupportTicketAiAssessment,
    SupportTicketAiAssessmentPage,
    TicketAiAssessmentPriority,
    TicketAiAssessmentQuery,
    TicketAiAssessmentStatus,
    AiResolutionMode,
} from '../types/supportTicketAiAssessment'

function getPageParameters(
    query: TicketAiAssessmentQuery,
) {
    return {
        page: query.page ?? 0,
        size: query.size ?? 20,
        sort:
            query.sort
            ?? 'createdAt,desc',
    }
}

export async function generateTicketAiAssessment(
    ticketId: number,
): Promise<SupportTicketAiAssessment> {
    const response =
        await procurementApiClient
            .post<SupportTicketAiAssessment>(
                `/api/v1/tickets/${ticketId}/ai-assessment`
            )

    return response.data
}

export async function getTicketAiAssessment(
    ticketId: number,
): Promise<SupportTicketAiAssessment> {
    const response =
        await procurementApiClient
            .get<SupportTicketAiAssessment>(
                `/api/v1/tickets/${ticketId}/ai-assessment`
            )

    return response.data
}

export async function getTicketAiAssessmentById(
    assessmentId: number,
): Promise<SupportTicketAiAssessment> {
    const response =
        await procurementApiClient
            .get<SupportTicketAiAssessment>(
                `/api/v1/ticket-ai-assessments/${assessmentId}`
            )

    return response.data
}

export async function getTicketAiAssessments(
    query: TicketAiAssessmentQuery = {},
): Promise<SupportTicketAiAssessmentPage> {
    const response =
        await procurementApiClient
            .get<SupportTicketAiAssessmentPage>(
                '/api/v1/ticket-ai-assessments',
                {
                    params:
                        getPageParameters(query),
                }
            )

    return response.data
}

export async function getTicketAiAssessmentsByStatus(
    status: TicketAiAssessmentStatus,
    query: TicketAiAssessmentQuery = {},
): Promise<SupportTicketAiAssessmentPage> {
    const response =
        await procurementApiClient
            .get<SupportTicketAiAssessmentPage>(
                '/api/v1/ticket-ai-assessments'
                + `/status/${status}`,
                {
                    params:
                        getPageParameters(query),
                }
            )

    return response.data
}

export async function getTicketAiAssessmentsByPriority(
    priority: TicketAiAssessmentPriority,
    query: TicketAiAssessmentQuery = {},
): Promise<SupportTicketAiAssessmentPage> {
    const response =
        await procurementApiClient
            .get<SupportTicketAiAssessmentPage>(
                '/api/v1/ticket-ai-assessments'
                + `/priority/${priority}`,
                {
                    params:
                        getPageParameters(query),
                }
            )

    return response.data
}

export async function getTicketAiAssessmentsByResolutionMode(
    resolutionMode: AiResolutionMode,
    query: TicketAiAssessmentQuery = {},
): Promise<SupportTicketAiAssessmentPage> {
    const response =
        await procurementApiClient
            .get<SupportTicketAiAssessmentPage>(
                '/api/v1/ticket-ai-assessments'
                + `/resolution-mode/${resolutionMode}`,
                {
                    params:
                        getPageParameters(query),
                }
            )

    return response.data
}

export async function approveTicketAiAssessment(
    assessmentId: number,
    request: ApproveTicketAiAssessmentRequest,
): Promise<SupportTicketAiAssessment> {
    const response =
        await procurementApiClient
            .patch<SupportTicketAiAssessment>(
                '/api/v1/ticket-ai-assessments'
                + `/${assessmentId}/approve`,
                request
            )

    return response.data
}

export async function rejectTicketAiAssessment(
    assessmentId: number,
    request: RejectTicketAiAssessmentRequest,
): Promise<SupportTicketAiAssessment> {
    const response =
        await procurementApiClient
            .patch<SupportTicketAiAssessment>(
                '/api/v1/ticket-ai-assessments'
                + `/${assessmentId}/reject`,
                request
            )

    return response.data
}

export async function escalateTicketAiAssessment(
    assessmentId: number,
    request: EscalateTicketAiAssessmentRequest,
): Promise<SupportTicketAiAssessment> {
    const response =
        await procurementApiClient
            .patch<SupportTicketAiAssessment>(
                '/api/v1/ticket-ai-assessments'
                + `/${assessmentId}/escalate`,
                request
            )

    return response.data
}

export async function publishTicketAiAssessmentResponse(
    assessmentId: number,
): Promise<SupportTicketAiAssessment> {
    const response =
        await procurementApiClient
            .post<SupportTicketAiAssessment>(
                '/api/v1/ticket-ai-assessments'
                + `/${assessmentId}/publish-response`
            )

    return response.data
}