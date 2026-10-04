import axios from 'axios'

import {
    clearAuthentication,
    getAccessToken,
    getTokenType,
} from '../utils/tokenStorage'

import type {
    CreateTraderProfileRequest,
    TraderProfile,
    UpdateTraderProfileRequest,
} from '../types/traderProfile'

const traderProfileApiBaseUrl =
    import.meta.env
        .VITE_TRADER_PROFILE_API_BASE_URL

if (!traderProfileApiBaseUrl) {
    throw new Error(
        'VITE_TRADER_PROFILE_API_BASE_URL '
        + 'is not configured.'
    )
}

const traderProfileApiClient =
    axios.create({
        baseURL: traderProfileApiBaseUrl,
        timeout: 15000,
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
        },
    })

traderProfileApiClient.interceptors
    .request.use((config) => {
    const accessToken = getAccessToken()

    if (accessToken) {
        config.headers.Authorization =
            `${getTokenType()} ${accessToken}`
    }

    return config
})

traderProfileApiClient.interceptors
    .response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            clearAuthentication()
        }

        return Promise.reject(error)
    }
)

export async function getMyTraderProfile():
    Promise<TraderProfile> {
    const response =
        await traderProfileApiClient
            .get<TraderProfile>(
                '/api/v1/traders/me'
            )

    return response.data
}

export async function createTraderProfile(
    request: CreateTraderProfileRequest
): Promise<TraderProfile> {
    const response =
        await traderProfileApiClient
            .post<TraderProfile>(
                '/api/v1/traders',
                request
            )

    return response.data
}

export async function updateMyTraderProfile(
    request: UpdateTraderProfileRequest
): Promise<TraderProfile> {
    const response =
        await traderProfileApiClient
            .put<TraderProfile>(
                '/api/v1/traders/me',
                request
            )

    return response.data
}
