import { useContext } from 'react'

import {
    AuthContext,
} from './authContextDefinition'

import type {
    AuthContextValue,
} from './authContextDefinition'

export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext)

    if (!context) {
        throw new Error(
            'useAuth must be used within AuthProvider.'
        )
    }

    return context
}