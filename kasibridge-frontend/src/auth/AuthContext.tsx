import {
    useCallback,
    useMemo,
    useState,
} from 'react'

import type { ReactNode } from 'react'

import { loginUser } from '../api/authApi'

import type {
    AuthenticatedUser,
    LoginRequest,
    UserRole,
} from '../types/auth'

import {
    clearAuthentication,
    getAccessToken,
    getCurrentUser,
    saveAuthentication,
} from '../utils/tokenStorage'

import {
    AuthContext,
} from './authContextDefinition'

import type {
    AuthContextValue,
} from './authContextDefinition'

interface AuthProviderProps {
    children: ReactNode
}

export function AuthProvider({
                                 children,
                             }: AuthProviderProps) {
    const [user, setUser] =
        useState<AuthenticatedUser | null>(
            getCurrentUser()
        )

    const isAuthenticated =
        Boolean(getAccessToken() && user)

    const login = useCallback(
        async (
            request: LoginRequest
        ): Promise<void> => {
            const response =
                await loginUser(request)

            saveAuthentication(response)

            setUser({
                userId: response.userId,
                username: response.username,
                orgId: response.orgId,
                roles: response.roles,
            })
        },
        []
    )

    const logout = useCallback(
        (): void => {
            clearAuthentication()
            setUser(null)
        },
        []
    )

    const hasRole = useCallback(
        (role: UserRole): boolean => {
            return user?.roles.includes(role) ?? false
        },
        [user]
    )

    const hasAnyRole = useCallback(
        (roles: UserRole[]): boolean => {
            if (!user) {
                return false
            }

            return roles.some((role) =>
                user.roles.includes(role)
            )
        },
        [user]
    )

    const value = useMemo<AuthContextValue>(
        () => ({
            user,
            isAuthenticated,
            login,
            logout,
            hasRole,
            hasAnyRole,
        }),
        [
            user,
            isAuthenticated,
            login,
            logout,
            hasRole,
            hasAnyRole,
        ]
    )

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    )
}