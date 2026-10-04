import axios from 'axios'

import {
    clearAuthentication,
    getAccessToken,
    getTokenType,
} from '../utils/tokenStorage'

const apiBaseUrl =
    import.meta.env.VITE_AUTH_API_BASE_URL

if (!apiBaseUrl) {
    throw new Error(
        'VITE_AUTH_API_BASE_URL is not configured.'
    )
}

export const apiClient = axios.create({
    baseURL: apiBaseUrl,
    timeout: 15000,
    headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
    },
})

apiClient.interceptors.request.use(
    (config) => {
        const accessToken = getAccessToken()

        if (accessToken) {
            config.headers.Authorization =
                `${getTokenType()} ${accessToken}`
        }

        return config
    }
)

apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            clearAuthentication()
        }

        return Promise.reject(error)
    }
)