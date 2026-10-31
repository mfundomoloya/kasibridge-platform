import axios from 'axios'

import {
    clearAuthentication,
    getAccessToken,
    getTokenType,
} from '../utils/tokenStorage'

const fraudApiBaseUrl =
    import.meta.env.VITE_FRAUD_API_BASE_URL

if (!fraudApiBaseUrl) {
    throw new Error(
        'VITE_FRAUD_API_BASE_URL '
        + 'is not configured.'
    )
}

export const fraudApiClient =
    axios.create({
        baseURL: fraudApiBaseUrl,
        timeout: 15000,
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
        },
    })

fraudApiClient.interceptors.request.use(
    (config) => {
        const accessToken =
            getAccessToken()

        if (accessToken) {
            config.headers.Authorization =
                `${getTokenType()} ${accessToken}`
        }

        return config
    }
)

fraudApiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            clearAuthentication()
        }

        return Promise.reject(error)
    }
)