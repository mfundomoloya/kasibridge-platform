import { apiClient } from './apiClient'

import type {
    LoginRequest,
    LoginResponse,
} from '../types/auth'

export async function loginUser(
    request: LoginRequest
): Promise<LoginResponse> {
    const response =
        await apiClient.post<LoginResponse>(
            '/api/v1/auth/login',
            request
        )

    return response.data
}