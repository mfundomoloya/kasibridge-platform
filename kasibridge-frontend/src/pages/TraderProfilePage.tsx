import {
    useEffect,
    useState,
} from 'react'

import type {
    FormEvent,
} from 'react'

import axios from 'axios'

import {
    createTraderProfile,
    getMyTraderProfile,
    updateMyTraderProfile,
} from '../api/traderProfileApi'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import {
    TraderProfileForm,
} from '../components/forms/TraderProfileForm'

import type {
    CreateTraderProfileRequest,
    TraderProfile,
    TraderProfileFormValues,
    UpdateTraderProfileRequest,
} from '../types/traderProfile'

const emptyForm: TraderProfileFormValues = {
    fullName: '',
    phoneNumber: '',
    email: '',
    idNumber: '',
    businessName: '',
    businessType: '',
    tradingArea: '',
    businessDescription: '',
    hasBusinessRegistration: false,
    cipcNumber: '',
    hasBankAccount: false,
    bankName: '',
    onboardingChannel: 'WEB',
}

function profileToForm(
    profile: TraderProfile
): TraderProfileFormValues {
    return {
        fullName: profile.fullName,
        phoneNumber: profile.phoneNumber,
        email: profile.email ?? '',
        idNumber: '',
        businessName: profile.businessName,
        businessType:
            profile.businessType ?? '',
        tradingArea: profile.tradingArea,
        businessDescription:
            profile.businessDescription ?? '',
        hasBusinessRegistration:
        profile.hasBusinessRegistration,
        cipcNumber:
            profile.cipcNumber ?? '',
        hasBankAccount:
        profile.hasBankAccount,
        bankName:
            profile.bankName ?? '',
        onboardingChannel:
        profile.onboardingChannel,
    }
}

function optionalValue(
    value: string
): string | null {
    const trimmedValue = value.trim()

    return trimmedValue.length > 0
        ? trimmedValue
        : null
}

function maskIdNumber(
    idNumber: string | null
): string {
    if (!idNumber) {
        return 'Not supplied'
    }

    const finalDigits =
        idNumber.slice(-4)

    return `*********${finalDigits}`
}

function getApiErrorMessage(
    error: unknown
): string {
    if (!axios.isAxiosError(error)) {
        return 'An unexpected profile error occurred.'
    }

    if (error.response?.status === 403) {
        return (
            'The signed-in account is not authorised '
            + 'to access this trader profile.'
        )
    }

    if (error.response?.status === 409) {
        return (
            'A profile already exists with one of '
            + 'the supplied contact details.'
        )
    }

    if (error.response?.status === 400) {
        return (
            'The profile information is invalid. '
            + 'Review the highlighted requirements.'
        )
    }

    if (!error.response) {
        return (
            'The Trader Profile Service could not '
            + 'be reached.'
        )
    }

    return 'The profile request could not be completed.'
}

export function TraderProfilePage() {
    const [profile, setProfile] =
        useState<TraderProfile | null>(null)

    const [formValues, setFormValues] =
        useState<TraderProfileFormValues>(
            emptyForm
        )

    const [isLoading, setIsLoading] =
        useState(true)

    const [isEditing, setIsEditing] =
        useState(false)

    const [isCreating, setIsCreating] =
        useState(false)

    const [isSubmitting, setIsSubmitting] =
        useState(false)

    const [errorMessage, setErrorMessage] =
        useState('')

    const [successMessage, setSuccessMessage] =
        useState('')

    useEffect(() => {
        let isActive = true

        getMyTraderProfile() .
        then((loadedProfile) => {
            if (!isActive) {
                return
            }
            setProfile(loadedProfile)
            setFormValues( profileToForm(loadedProfile) )
            setIsCreating(false)
            setIsEditing(false)
            setErrorMessage('')
        }) .catch((error: unknown) => {

            if (!isActive) {
                return }

            if ( axios.isAxiosError(error) && error.response?.status === 404 ) {
                setProfile(null)
                setFormValues(emptyForm)
                setIsCreating(true)
                setIsEditing(true)
                setErrorMessage('')

                return
            }

            setErrorMessage( getApiErrorMessage(error) ) })
            .finally(() => {
                if (isActive) {
                    setIsLoading(false)
                }
            })

        return () => {
            isActive = false
        }
    }, [])

    async function handleSubmit(
        event: FormEvent<HTMLFormElement>
    ): Promise<void> {
        event.preventDefault()

        setErrorMessage('')
        setSuccessMessage('')
        setIsSubmitting(true)

        try {
            let savedProfile: TraderProfile

            if (isCreating) {
                const request:
                    CreateTraderProfileRequest = {
                    fullName:
                        formValues.fullName.trim(),
                    phoneNumber:
                        formValues.phoneNumber.trim(),
                    email:
                        optionalValue(formValues.email),
                    idNumber:
                        optionalValue(formValues.idNumber),
                    businessName:
                        formValues.businessName.trim(),
                    businessType:
                        optionalValue(
                            formValues.businessType
                        ),
                    tradingArea:
                        formValues.tradingArea.trim(),
                    businessDescription:
                        optionalValue(
                            formValues.businessDescription
                        ),
                    hasBusinessRegistration:
                    formValues
                        .hasBusinessRegistration,
                    cipcNumber:
                        formValues
                            .hasBusinessRegistration
                            ? optionalValue(
                                formValues.cipcNumber
                            )
                            : null,
                    hasBankAccount:
                    formValues.hasBankAccount,
                    bankName:
                        formValues.hasBankAccount
                            ? optionalValue(
                                formValues.bankName
                            )
                            : null,
                    onboardingChannel:
                    formValues.onboardingChannel,
                }

                savedProfile =
                    await createTraderProfile(request)

                setSuccessMessage(
                    'The business profile was created.'
                )
            } else {
                const request:
                    UpdateTraderProfileRequest = {
                    businessName:
                        formValues.businessName.trim(),
                    businessType:
                        optionalValue(
                            formValues.businessType
                        ),
                    tradingArea:
                        formValues.tradingArea.trim(),
                    businessDescription:
                        optionalValue(
                            formValues.businessDescription
                        ),
                    hasBusinessRegistration:
                    formValues
                        .hasBusinessRegistration,
                    cipcNumber:
                        formValues
                            .hasBusinessRegistration
                            ? optionalValue(
                                formValues.cipcNumber
                            )
                            : null,
                    hasBankAccount:
                    formValues.hasBankAccount,
                    bankName:
                        formValues.hasBankAccount
                            ? optionalValue(
                                formValues.bankName
                            )
                            : null,
                }

                savedProfile =
                    await updateMyTraderProfile(
                        request
                    )

                setSuccessMessage(
                    'The business profile was updated.'
                )
            }

            setProfile(savedProfile)

            setFormValues(
                profileToForm(savedProfile)
            )

            setIsCreating(false)
            setIsEditing(false)
        } catch (error) {
            setErrorMessage(
                getApiErrorMessage(error)
            )
        } finally {
            setIsSubmitting(false)
        }
    }

    function handleCancel(): void {
        if (!profile) {
            return
        }

        setFormValues(
            profileToForm(profile)
        )

        setErrorMessage('')
        setSuccessMessage('')
        setIsEditing(false)
    }

    if (isLoading) {
        return (
            <AppLayout>
                <section className="content-card">
                    <p>Loading business profile...</p>
                </section>
            </AppLayout>
        )
    }

    return (
        <AppLayout>
            <section className="page-heading">
                <div>
          <span className="eyebrow">
            Trader identity
          </span>

                    <h1>Business profile</h1>

                    <p>
                        Maintain the verified business details
                        used throughout KasiBridge.
                    </p>
                </div>

                {profile && !isEditing && (
                    <button
                        className="primary-button"
                        type="button"
                        onClick={() => {
                            setSuccessMessage('')
                            setIsEditing(true)
                        }}
                    >
                        Edit profile
                    </button>
                )}
            </section>

            {errorMessage && (
                <div
                    className="error-message page-message"
                    role="alert"
                >
                    {errorMessage}
                </div>
            )}

            {successMessage && (
                <div
                    className="success-message page-message"
                    role="status"
                >
                    {successMessage}
                </div>
            )}

            {profile && !isEditing ? (
                <section className="profile-grid">
                    <article className="content-card">
                        <div className="card-heading">
                            <div>
                <span className="eyebrow">
                  Profile status
                </span>

                                <h2>{profile.businessName}</h2>
                            </div>

                            <span
                                className={
                                    `status-badge status-${profile
                                        .status
                                        .toLowerCase()}`
                                }
                            >
                {profile.status}
              </span>
                        </div>

                        <dl className="profile-details">
                            <div>
                                <dt>Full name</dt>
                                <dd>{profile.fullName}</dd>
                            </div>

                            <div>
                                <dt>Phone</dt>
                                <dd>{profile.phoneNumber}</dd>
                            </div>

                            <div>
                                <dt>Email</dt>
                                <dd>
                                    {profile.email ?? 'Not supplied'}
                                </dd>
                            </div>

                            <div>
                                <dt>ID number</dt>
                                <dd>
                                    {maskIdNumber(
                                        profile.idNumber
                                    )}
                                </dd>
                            </div>

                            <div>
                                <dt>Business type</dt>
                                <dd>
                                    {profile.businessType
                                        ?? 'Not supplied'}
                                </dd>
                            </div>

                            <div>
                                <dt>Trading area</dt>
                                <dd>{profile.tradingArea}</dd>
                            </div>

                            <div>
                                <dt>CIPC number</dt>
                                <dd>
                                    {profile.cipcNumber
                                        ?? 'Not supplied'}
                                </dd>
                            </div>

                            <div>
                                <dt>Bank</dt>
                                <dd>
                                    {profile.bankName
                                        ?? 'Not supplied'}
                                </dd>
                            </div>

                            <div>
                                <dt>Onboarding channel</dt>
                                <dd>
                                    {profile.onboardingChannel}
                                </dd>
                            </div>

                            <div>
                                <dt>Credibility score</dt>
                                <dd>
                                    {profile.credibilityScore
                                        ?? 'Not available yet'}
                                </dd>
                            </div>
                        </dl>
                    </article>

                    <article className="content-card">
            <span className="eyebrow">
              Business description
            </span>

                        <h2>About the business</h2>

                        <p className="profile-description">
                            {profile.businessDescription
                                ?? 'No business description '
                                + 'has been supplied.'}
                        </p>
                    </article>
                </section>
            ) : (
                <TraderProfileForm
                    values={formValues}
                    isCreating={isCreating}
                    isSubmitting={isSubmitting}
                    onChange={setFormValues}
                    onSubmit={handleSubmit}
                    onCancel={
                        profile
                            ? handleCancel
                            : undefined
                    }
                />
            )}
        </AppLayout>
    )
}