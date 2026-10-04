export interface LoginRequest {
    username: string
    password: string
}

export type UserRole = string

export interface LoginResponse {
    token: string
    tokenType: string
    userId: number
    username: string
    orgId: string | null
    roles: UserRole[]
}

export interface AuthenticatedUser {
    userId: number
    username: string
    orgId: string | null
    roles: UserRole[]
}