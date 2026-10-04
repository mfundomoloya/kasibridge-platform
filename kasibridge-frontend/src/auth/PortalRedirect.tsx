import { Navigate } from 'react-router-dom'

import { useAuth } from './useAuth'

export function PortalRedirect() {
    const {
        isAuthenticated,
        hasRole,
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

    if (hasRole('ROLE_PLATFORM_ADMIN')) {
        return (
            <Navigate
                to="/admin"
                replace
            />
        )
    }

    if (hasRole('ROLE_TRADER')) {
        return (
            <Navigate
                to="/dashboard"
                replace
            />
        )
    }

    if (
        hasAnyRole([
            'ROLE_ANALYST',
            'ROLE_SPECIFICATION_OFFICER',
            'ROLE_EVALUATOR',
            'ROLE_ADJUDICATOR',
        ])
    ) {
        return (
            <Navigate
                to="/procurement"
                replace
            />
        )
    }

    return (
        <Navigate
            to="/unauthorized"
            replace
        />
    )
}