import type {
    AuthenticatedUser,
    LoginResponse,
} from '../types/auth'

const accessTokenKey =
    'kasibridge_access_token'

const tokenTypeKey =
    'kasibridge_token_type'

const currentUserKey =
    'kasibridge_current_user'

export function saveAuthentication(
    response: LoginResponse
): void {
    sessionStorage.setItem(
        accessTokenKey,
        response.token
    )

    sessionStorage.setItem(
        tokenTypeKey,
        response.tokenType || 'Bearer'
    )

    const user: AuthenticatedUser = {
        userId: response.userId,
        username: response.username,
        orgId: response.orgId,
        roles: response.roles,
    }

    sessionStorage.setItem(
        currentUserKey,
        JSON.stringify(user)
    )
}

export function getAccessToken():
    string | null {
    return sessionStorage.getItem(accessTokenKey)
}

export function getTokenType(): string {
    return (
        sessionStorage.getItem(tokenTypeKey)
        || 'Bearer'
    )
}

export function getCurrentUser():
    AuthenticatedUser | null {
    const storedUser =
        sessionStorage.getItem(currentUserKey)

    if (!storedUser) {
        return null
    }

    try {
        return JSON.parse(
            storedUser
        ) as AuthenticatedUser
    } catch {
        clearAuthentication()
        return null
    }
}

export function clearAuthentication(): void {
    sessionStorage.removeItem(accessTokenKey)
    sessionStorage.removeItem(tokenTypeKey)
    sessionStorage.removeItem(currentUserKey)
}