import axios from 'axios'

import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    CommitteeRole,
    CurrentUserCommitteeAssignment,
} from '../types/committeeAssignment'

interface ProcurementApiErrorResponse {
    status?: number
    error?: string
    message?: string
    timestamp?: string
}

export async function
getMyCommitteeAssignmentQueue(
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
                'The committee assignment query '
                + 'is invalid.'
            )

        case 401:
            return (
                'The current session is no '
                + 'longer authenticated. Sign in '
                + 'again and retry.'
            )

        case 403:
            return (
                'The authenticated account is '
                + 'not authorised to access this '
                + 'committee assignment queue.'
            )

        case 404:
            return (
                'A tender associated with an '
                + 'active committee assignment '
                + 'could not be found.'
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