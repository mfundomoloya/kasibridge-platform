import type { ReactNode } from 'react'

import { Navigate } from 'react-router-dom'

import type {
    UserRole,
} from '../types/auth'

import { useAuth } from './useAuth'

interface RoleRouteProps {
    allowedRoles: UserRole[]
    children: ReactNode
}

export function RoleRoute({
                              allowedRoles,
                              children,
                          }: RoleRouteProps) {
    const {
        isAuthenticated,
        hasAnyRole,
    } = useAuth()

    if (!isAuthenticated) {
        return (
            <Navigate
                to="/login"
                replace
            />
        )
    }

    if (!hasAnyRole(allowedRoles)) {
        return (
            <Navigate
                to="/unauthorized"
                replace
            />
        )
    }

    return children
}