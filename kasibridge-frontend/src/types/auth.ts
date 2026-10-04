export interface LoginRequest {
    username: string
    password: string
}

export type UserRole =
    | 'ROLE_ANALYST'
    | 'ROLE_PLATFORM_ADMIN'
    | 'ROLE_SYSTEM'
    | 'ROLE_TRADER'
    | 'ROLE_SPECIFICATION_OFFICER'
    | 'ROLE_EVALUATOR'
    | 'ROLE_ADJUDICATOR'

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