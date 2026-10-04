import { createContext } from 'react'

import type {
    AuthenticatedUser,
    LoginRequest, UserRole,
} from '../types/auth'

export interface AuthContextValue {
    user: AuthenticatedUser | null
    isAuthenticated: boolean
    login: (
        request: LoginRequest
    ) => Promise<void>
    logout: () => void
    hasRole: (role: UserRole) => boolean
    hasAnyRole: (roles: UserRole[]) => boolean
}

export const AuthContext =
    createContext<AuthContextValue | undefined>(
        undefined
    )