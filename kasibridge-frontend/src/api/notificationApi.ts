import axios from 'axios'

import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    ApiErrorResponse,
} from '../types/bid'

import type {
    Notification,
    NotificationApiError,
    NotificationPage,
} from '../types/notification'

export type NotificationReadFilter =
    | 'all'
    | 'unread'
    | 'read'

export interface GetNotificationsParameters {
    page?: number
    size?: number
    sort?: string
    filter?: NotificationReadFilter
}

function getNotificationPath(
    filter: NotificationReadFilter
): string {
    if (filter === 'unread') {
        return (
            '/api/v1/notifications/'
            + 'outbox/in-app/unread'
        )
    }

    if (filter === 'read') {
        return (
            '/api/v1/notifications/'
            + 'outbox/in-app/read'
        )
    }

    return (
        '/api/v1/notifications/'
        + 'outbox/in-app'
    )
}

export async function getInAppNotifications(
    parameters: GetNotificationsParameters = {},
): Promise<NotificationPage> {
    const filter =
        parameters.filter ?? 'all'

    const response =
        await procurementApiClient
            .get<NotificationPage>(
                getNotificationPath(filter),
                {
                    params: {
                        page:
                            parameters.page ?? 0,
                        size:
                            parameters.size ?? 20,
                        sort:
                            parameters.sort
                            ?? (
                                filter === 'read'
                                    ? 'readAt,desc'
                                    : 'createdAt,desc'
                            ),
                    },
                },
            )

    return response.data
}

export async function markNotificationRead(
    notificationId: number,
): Promise<Notification> {
    const response =
        await procurementApiClient
            .patch<Notification>(
                '/api/v1/notifications/'
                + 'outbox/in-app/'
                + `${notificationId}/read`,
            )

    return response.data
}

export function getNotificationApiError(
    error: unknown,
): NotificationApiError {
    if (
        axios.isAxiosError<ApiErrorResponse>(
            error
        )
    ) {
        if (error.response?.data) {
            return {
                status:
                error.response.data.status,
                message:
                    error.response.data.message
                    || (
                        'The notification request '
                        + 'could not be completed.'
                    ),
                fieldErrors:
                    error.response.data.fieldErrors
                    ?? null,
            }
        }

        if (error.code === 'ECONNABORTED') {
            return {
                status: 0,
                message:
                    'The notification request '
                    + 'timed out. Please try again.',
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
                || (
                    'The notification request '
                    + 'could not be completed.'
                ),
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
            'An unexpected notification '
            + 'error occurred.',
        fieldErrors: null,
    }
}