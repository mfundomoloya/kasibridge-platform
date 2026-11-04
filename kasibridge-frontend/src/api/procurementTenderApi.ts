import axios from 'axios'

import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    CreateProcurementTenderRequest,
    ProcurementApiErrorResponse,
    ProcurementTender,
    ProcurementTenderPage,
    ProcurementTenderQuery,
    SpecificationIntegrity,
} from '../types/procurementTender'

export async function createProcurementTender(
    request: CreateProcurementTenderRequest,
): Promise<ProcurementTender> {
    const response =
        await procurementApiClient
            .post<ProcurementTender>(
                '/api/v1/tenders',
                normalizeCreateTenderRequest(
                    request
                )
            )

    return response.data
}

export async function getProcurementTenders(
    query: ProcurementTenderQuery = {},
): Promise<ProcurementTenderPage> {
    const response =
        await procurementApiClient
            .get<ProcurementTenderPage>(
                '/api/v1/tenders',
                {
                    params: {
                        page:
                            query.page ?? 0,

                        size:
                            query.size ?? 20,

                        sort:
                            query.sort
                            ?? 'createdAt,desc',
                    },
                }
            )

    return response.data
}

export async function getProcurementTender(
    tenderId: number,
): Promise<ProcurementTender> {
    const validatedTenderId =
        requirePositiveTenderId(
            tenderId
        )

    const response =
        await procurementApiClient
            .get<ProcurementTender>(
                `/api/v1/tenders/${validatedTenderId}`
            )

    return response.data
}

export async function getProcurementTenderByReference(
    tenderReference: string,
): Promise<ProcurementTender> {
    const normalizedReference =
        requireTenderReference(
            tenderReference
        )

    const response =
        await procurementApiClient
            .get<ProcurementTender>(
                '/api/v1/tenders/reference/'
                + encodeURIComponent(
                    normalizedReference
                )
            )

    return response.data
}

export async function publishProcurementTender(
    tenderId: number,
): Promise<ProcurementTender> {
    const validatedTenderId =
        requirePositiveTenderId(
            tenderId
        )

    const response =
        await procurementApiClient
            .patch<ProcurementTender>(
                `/api/v1/tenders/${validatedTenderId}/publish`
            )

    return response.data
}

export async function closeProcurementTenderBidding(
    tenderId: number,
): Promise<ProcurementTender> {
    const validatedTenderId =
        requirePositiveTenderId(
            tenderId
        )

    const response =
        await procurementApiClient
            .patch<ProcurementTender>(
                `/api/v1/tenders/${validatedTenderId}/close-bidding`
            )

    return response.data
}

export async function startProcurementTenderEvaluation(
    tenderId: number,
): Promise<ProcurementTender> {
    const validatedTenderId =
        requirePositiveTenderId(
            tenderId
        )

    const response =
        await procurementApiClient
            .patch<ProcurementTender>(
                `/api/v1/tenders/${validatedTenderId}/start-evaluation`
            )

    return response.data
}

export async function verifyProcurementTenderSpecification(
    tenderId: number,
): Promise<SpecificationIntegrity> {
    const validatedTenderId =
        requirePositiveTenderId(
            tenderId
        )

    const response =
        await procurementApiClient
            .get<SpecificationIntegrity>(
                `/api/v1/tenders/${validatedTenderId}/specification/verify`
            )

    return response.data
}

export function getProcurementTenderApiErrorMessage(
    error: unknown,
    fallbackMessage:
    string = 'The procurement request failed.',
): string {
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
        responseMessage
        && responseMessage.trim().length > 0
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
                (fieldError) =>
                    `${fieldError.field}: `
                    + fieldError.message
            )
            .join(' ')
    }

    if (
        error.code === 'ECONNABORTED'
    ) {
        return (
            'The procurement request timed out. '
            + 'Please try again.'
        )
    }

    if (!error.response) {
        return (
            'The procurement service is unavailable. '
            + 'Confirm that the service is running '
            + 'and try again.'
        )
    }

    if (
        error.response.status === 403
    ) {
        return (
            'The authenticated user is not '
            + 'authorised to perform this '
            + 'procurement action.'
        )
    }

    if (
        error.response.status === 404
    ) {
        return (
            'The requested tender could not '
            + 'be found.'
        )
    }

    if (
        error.response.status === 409
    ) {
        return (
            responseMessage
            ?? 'The tender cannot perform this '
            + 'workflow transition in its '
            + 'current state.'
        )
    }

    return fallbackMessage
}

export function isTenderStateConflict(
    error: unknown,
): boolean {
    return (
        axios.isAxiosError(error)
        && error.response?.status === 409
    )
}

export function isProcurementForbidden(
    error: unknown,
): boolean {
    return (
        axios.isAxiosError(error)
        && error.response?.status === 403
    )
}

function normalizeCreateTenderRequest(
    request: CreateProcurementTenderRequest,
): CreateProcurementTenderRequest {
    return {
        title:
            requireText(
                request.title,
                'Tender title'
            ),

        description:
            requireText(
                request.description,
                'Tender description'
            ),

        evaluationCriteria:
            requireText(
                request.evaluationCriteria,
                'Evaluation criteria'
            ),

        budgetAmount:
            requirePositiveAmount(
                request.budgetAmount
            ),

        buyerOrgId:
            requireText(
                request.buyerOrgId,
                'Buyer organisation ID'
            ),

        requiredBusinessType:
            requireText(
                request.requiredBusinessType,
                'Required business type'
            ),

        serviceArea:
            requireText(
                request.serviceArea,
                'Service area'
            ),

        createdByUserId:
            requirePositiveUserId(
                request.createdByUserId
            ),
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

function requirePositiveUserId(
    userId: number,
): number {
    if (
        !Number.isInteger(userId)
        || userId <= 0
    ) {
        throw new Error(
            'A valid creator user ID is required.'
        )
    }

    return userId
}

function requirePositiveAmount(
    amount: number,
): number {
    if (
        !Number.isFinite(amount)
        || amount <= 0
    ) {
        throw new Error(
            'Budget amount must be greater '
            + 'than zero.'
        )
    }

    return amount
}

function requireTenderReference(
    tenderReference: string,
): string {
    return requireText(
        tenderReference,
        'Tender reference'
    )
}

function requireText(
    value: string,
    fieldName: string,
): string {
    const normalizedValue =
        value.trim()

    if (normalizedValue.length === 0) {
        throw new Error(
            `${fieldName} is required.`
        )
    }

    return normalizedValue
}