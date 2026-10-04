export type OnboardingChannel =
    | 'WEB'
    | 'WHATSAPP'

export type ProfileStatus =
    | 'PENDING'
    | 'ACTIVE'
    | 'SUSPENDED'
    | 'INACTIVE'

export interface TraderProfile {
    id: number
    userId: number
    fullName: string
    phoneNumber: string
    email: string | null
    idNumber: string | null
    businessName: string
    businessType: string | null
    tradingArea: string
    businessDescription: string | null
    hasBusinessRegistration: boolean
    cipcNumber: string | null
    hasBankAccount: boolean
    bankName: string | null
    onboardingChannel: OnboardingChannel
    status: ProfileStatus
    createdAt: string
    updatedAt: string
    credibilityScore: number | null
}

export interface CreateTraderProfileRequest {
    fullName: string
    phoneNumber: string
    email: string | null
    businessName: string
    businessType: string | null
    tradingArea: string
    businessDescription: string | null
    idNumber: string | null
    hasBusinessRegistration: boolean
    cipcNumber: string | null
    hasBankAccount: boolean
    bankName: string | null
    onboardingChannel: OnboardingChannel
}

export interface UpdateTraderProfileRequest {
    businessName: string
    businessType: string | null
    tradingArea: string
    businessDescription: string | null
    hasBusinessRegistration: boolean
    cipcNumber: string | null
    hasBankAccount: boolean
    bankName: string | null
}

export interface TraderProfileFormValues {
    fullName: string
    phoneNumber: string
    email: string
    idNumber: string
    businessName: string
    businessType: string
    tradingArea: string
    businessDescription: string
    hasBusinessRegistration: boolean
    cipcNumber: string
    hasBankAccount: boolean
    bankName: string
    onboardingChannel: OnboardingChannel
}