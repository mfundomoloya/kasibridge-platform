import {
    procurementApiClient,
} from './procurementApiClient'

import type {
    TenderPageResponse,
    TenderQuery,
    TraderTender,
} from '../types/tender'

export async function getOpenTenders(
    query: TenderQuery
): Promise<TenderPageResponse> {
    const response =
        await procurementApiClient
            .get<TenderPageResponse>(
                '/api/v1/tenders/open',
                {
                    params: {
                        page: query.page,
                        size: query.size,
                    },
                }
            )

    return response.data
}

export async function getOpenTenderById(
    tenderId: number
): Promise<TraderTender> {
    const response =
        await procurementApiClient
            .get<TraderTender>(
                `/api/v1/tenders/open/${tenderId}`
            )

    return response.data
}