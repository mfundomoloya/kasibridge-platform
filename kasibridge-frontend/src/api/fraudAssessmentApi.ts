import axios from 'axios'

import {
    fraudApiClient,
} from './fraudApiClient'

import type {
    FraudApiErrorResponse,
    FraudAssessment,
    FraudAssessmentDashboard,
    FraudAssessmentPage,
    FraudAssessmentQuery,
    FraudAssessmentReviewRequest,
} from '../types/fraudAssessment'

function normalizeOptionalText(
    value: string | undefined,
): string | undefined {
    if (
        value === undefined
        || value.trim().length === 0
    ) {
        return undefined
    }

    return value.trim()
}

function buildAssessmentQueryParameters(
    query: FraudAssessmentQuery,
) {
    return {
        reviewStatus:
            query.reviewStatus
            || undefined,

        riskLevel:
            query.riskLevel
            || undefined,

        classification:
            query.classification
            || undefined,

        supplierId:
            normalizeOptionalText(
                query.supplierId
            ),

        departmentId:
            normalizeOptionalText(
                query.departmentId
            ),

        modelVersion:
            normalizeOptionalText(
                query.modelVersion
            ),

        assessedFrom:
            normalizeOptionalText(
                query.assessedFrom
            ),

        assessedTo:
            normalizeOptionalText(
                query.assessedTo
            ),

        page:
            query.page ?? 0,

        size:
            query.size ?? 20,

        sort:
        query.sort,
    }
}

export async function getFraudAssessmentDashboard():
    Promise<FraudAssessmentDashboard> {
    const response =
        await fraudApiClient
            .get<FraudAssessmentDashboard>(
                '/api/v1/fraud/assessments/dashboard'
            )

    return response.data
}

export async function getFraudAssessments(
    query: FraudAssessmentQuery = {},
): Promise<FraudAssessmentPage> {
    const response =
        await fraudApiClient
            .get<FraudAssessmentPage>(
                '/api/v1/fraud/assessments',
                {
                    params:
                        buildAssessmentQueryParameters(
                            query
                        ),
                }
            )

    return response.data
}

export async function getFraudAssessment(
    assessmentReference: string,
): Promise<FraudAssessment> {
    const normalizedReference =
        requireAssessmentReference(
            assessmentReference
        )

    const response =
        await fraudApiClient
            .get<FraudAssessment>(
                '/api/v1/fraud/assessments/'
                + encodeURIComponent(
                    normalizedReference
                )
            )

    return response.data
}

export async function confirmFraudAssessment(
    assessmentReference: string,
    request: FraudAssessmentReviewRequest,
): Promise<FraudAssessment> {
    const normalizedReference =
        requireAssessmentReference(
            assessmentReference
        )

    const response =
        await fraudApiClient
            .post<FraudAssessment>(
                '/api/v1/fraud/assessments/'
                + encodeURIComponent(
                    normalizedReference
                )
                + '/confirm',
                request
            )

    return response.data
}

export async function markFraudAssessmentFalsePositive(
    assessmentReference: string,
    request: FraudAssessmentReviewRequest,
): Promise<FraudAssessment> {
    const normalizedReference =
        requireAssessmentReference(
            assessmentReference
        )

    const response =
        await fraudApiClient
            .post<FraudAssessment>(
                '/api/v1/fraud/assessments/'
                + encodeURIComponent(
                    normalizedReference
                )
                + '/false-positive',
                request
            )

    return response.data
}

export async function escalateFraudAssessment(
    assessmentReference: string,
    request: FraudAssessmentReviewRequest,
): Promise<FraudAssessment> {
    const normalizedReference =
        requireAssessmentReference(
            assessmentReference
        )

    const response =
        await fraudApiClient
            .post<FraudAssessment>(
                '/api/v1/fraud/assessments/'
                + encodeURIComponent(
                    normalizedReference
                )
                + '/escalate',
                request
            )

    return response.data
}

export function getFraudApiErrorMessage(
    error: unknown,
    fallbackMessage:
    string = 'The fraud service request failed.',
): string {
    if (
        !axios.isAxiosError<
            FraudApiErrorResponse
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

    if (
        error.code === 'ECONNABORTED'
    ) {
        return (
            'The fraud service request timed out. '
            + 'Please try again.'
        )
    }

    if (!error.response) {
        return (
            'The fraud service is unavailable. '
            + 'Confirm that the service is running '
            + 'and try again.'
        )
    }

    return fallbackMessage
}

export function isFraudAssessmentVersionConflict(
    error: unknown,
): boolean {
    if (
        !axios.isAxiosError<
            FraudApiErrorResponse
        >(error)
    ) {
        return false
    }

    return (
        error.response?.status === 409
        && error.response.data?.error
        === 'Fraud assessment version conflict'
    )
}

export function isFraudAssessmentStateConflict(
    error: unknown,
): boolean {
    if (
        !axios.isAxiosError<
            FraudApiErrorResponse
        >(error)
    ) {
        return false
    }

    return (
        error.response?.status === 409
        && error.response.data?.error
        === 'Invalid assessment state'
    )
}

export function isFraudApiForbidden(
    error: unknown,
): boolean {
    return (
        axios.isAxiosError(error)
        && error.response?.status === 403
    )
}

function requireAssessmentReference(
    assessmentReference: string,
): string {
    const normalizedReference =
        assessmentReference.trim()

    if (normalizedReference.length === 0) {
        throw new Error(
            'Assessment reference is required.'
        )
    }

    return normalizedReference
}