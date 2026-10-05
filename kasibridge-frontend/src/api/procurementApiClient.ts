import axios from 'axios'

import {
    clearAuthentication,
    getAccessToken,
    getTokenType,
} from '../utils/tokenStorage'

const procurementApiBaseUrl =
    import.meta.env.VITE_PROCUREMENT_API_BASE_URL

if (!procurementApiBaseUrl) {
    throw new Error(
        'VITE_PROCUREMENT_API_BASE_URL '
        + 'is not configured.'
    )
}

export const procurementApiClient =
    axios.create({
        baseURL: procurementApiBaseUrl,
        timeout: 15000,
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
        },
    })

procurementApiClient.interceptors.request.use(
    (config) => {
        const accessToken = getAccessToken()

        if (accessToken) {
            config.headers.Authorization =
                `${getTokenType()} ${accessToken}`
        }

        return config
    }
)

procurementApiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            clearAuthentication()
        }

        return Promise.reject(error)
    }
)
