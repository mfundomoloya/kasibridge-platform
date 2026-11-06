import axios from 'axios'

import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    AdministrativeCommitteeAssignment,
    AssignCommitteeMemberRequest,
    CommitteeRole,
    CurrentUserCommitteeAssignment,
} from '../types/committeeAssignment'

interface ProcurementApiErrorResponse {
    status?: number
    error?: string
    message?: string
    timestamp?: string
}

export async function getMyCommitteeAssignmentQueue(
    committeeRole?: CommitteeRole,
): Promise<
    CurrentUserCommitteeAssignment[]
> {
    const response =
        await procurementApiClient
            .get<
                CurrentUserCommitteeAssignment[]
            >(
                '/api/v1/'
                + 'committee-assignments/'
                + 'my/queue',
                {
                    params:
                        committeeRole === undefined
                            ? undefined
                            : {
                                committeeRole,
                            },
                }
            )

    return response.data
}

export async function assignCommitteeMember(
    tenderId: number,
    request: AssignCommitteeMemberRequest,
): Promise<AdministrativeCommitteeAssignment> {
    const validTenderId =
        requirePositiveIdentifier(
            tenderId,
            'Tender ID'
        )

    const validRequest =
        normalizeAssignmentRequest(
            request
        )

    const response =
        await procurementApiClient
            .post<AdministrativeCommitteeAssignment>(
                '/api/v1/tenders/'
                + validTenderId
                + '/committee',
                validRequest
            )

    return response.data
}

export async function getTenderCommitteeAssignments(
    tenderId: number,
): Promise<AdministrativeCommitteeAssignment[]> {
    const validTenderId =
        requirePositiveIdentifier(
            tenderId,
            'Tender ID'
        )

    const response =
        await procurementApiClient
            .get<
                AdministrativeCommitteeAssignment[]
            >(
                '/api/v1/tenders/'
                + validTenderId
                + '/committee'
            )

    return response.data
}

export async function
getMyEvaluatorAssignmentQueue():
    Promise<
        CurrentUserCommitteeAssignment[]
    > {
    return getMyCommitteeAssignmentQueue(
        'EVALUATOR'
    )
}

export async function
getMyAdjudicatorAssignmentQueue():
    Promise<
        CurrentUserCommitteeAssignment[]
    > {
    return getMyCommitteeAssignmentQueue(
        'ADJUDICATOR'
    )
}

export async function
getMySpecificationAssignmentQueue():
    Promise<
        CurrentUserCommitteeAssignment[]
    > {
    return getMyCommitteeAssignmentQueue(
        'SPECIFICATION_OFFICER'
    )
}

export function
getCommitteeAssignmentApiErrorMessage(
    error: unknown,
    fallbackMessage:
    string = (
        'Unable to load committee '
        + 'assignments.'
    ),
): string {

    if (
        error instanceof Error
        && !axios.isAxiosError(error)
    ) {
        const message =
            error.message.trim()

        return message.length > 0
            ? message
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

    if (
        error.code === 'ECONNABORTED'
    ) {
        return (
            'The committee assignment request '
            + 'timed out. Please try again.'
        )
    }

    if (!error.response) {
        return (
            'The procurement service is '
            + 'unavailable. Confirm that the '
            + 'service is running and try again.'
        )
    }

    switch (error.response.status) {
        case 400:
            return (
                'The committee assignment contains '
                + 'invalid information.'
            )

        case 401:
            return (
                'The current session is no longer '
                + 'authenticated. Sign in again '
                + 'and retry.'
            )

        case 403:
            return (
                'The authenticated account is not '
                + 'authorised to access the requested '
                + 'committee assignment resource.'
            )

        case 404:
            return (
                'The selected tender or committee '
                + 'member could not be found.'
            )

        case 409:
            return (
                'The user already has an active '
                + 'committee role on this tender, '
                + 'or the assignment violates '
                + 'segregation of duties.'
            )

        default:
            return fallbackMessage
    }
}

export function
isCommitteeAssignmentApiForbidden(
    error: unknown,
): boolean {
    return (
        axios.isAxiosError(error)
        && error.response?.status === 403
    )

}

function normalizeAssignmentRequest(
    request:
    AssignCommitteeMemberRequest,
): AssignCommitteeMemberRequest {
    if (
        request === null
        || request === undefined
    ) {
        throw new Error(
            'A committee assignment '
            + 'request is required.'
        )
    }

    const userId =
        requirePositiveIdentifier(
            request.userId,
            'Committee member user ID'
        )

    const assignedByUserId =
        requirePositiveIdentifier(
            request.assignedByUserId,
            'Assigning user ID'
        )

    const reason =
        request.reason.trim()

    if (reason.length < 10) {
        throw new Error(
            'The assignment reason must '
            + 'contain at least '
            + '10 characters.'
        )
    }

    if (reason.length > 1000) {
        throw new Error(
            'The assignment reason cannot '
            + 'exceed 1000 characters.'
        )
    }

    return {
        userId,
        committeeRole:
        request.committeeRole,
        assignedByUserId,
        reason,
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