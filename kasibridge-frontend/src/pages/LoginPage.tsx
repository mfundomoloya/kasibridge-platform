import {
    useEffect,
    useState,
} from 'react'

import axios from 'axios'

import {
    useLocation,
    useNavigate,
} from 'react-router-dom'

import { useAuth } from '../auth/useAuth'

interface LocationState {
    from?: string
}

export function LoginPage() {
    const navigate = useNavigate()
    const location = useLocation()

    const {
        login,
        isAuthenticated,
    } = useAuth()

    const [username, setUsername] =
        useState('')

    const [password, setPassword] =
        useState('')

    const [errorMessage, setErrorMessage] =
        useState('')

    const [isSubmitting, setIsSubmitting] =
        useState(false)

    const locationState =
        location.state as LocationState | null

    useEffect(() => {
        if (isAuthenticated) {
            navigate('/portal', {
                replace: true,
            })
        }
    }, [isAuthenticated, navigate])

    async function handleSubmit(
        event: React.FormEvent<HTMLFormElement>
    ) {
        event.preventDefault()

        setErrorMessage('')
        setIsSubmitting(true)

        try {
            await login({
                username: username.trim(),
                password,
            })

            navigate(
                locationState?.from || '/portal',
                {
                    replace: true,
                }
            )
        } catch (error) {
            if (axios.isAxiosError(error)) {
                if (error.response?.status === 401) {
                    setErrorMessage(
                        'The username or password is incorrect.'
                    )
                } else if (error.response?.status === 403) {
                    setErrorMessage(
                        'This account is not permitted to access the portal.'
                    )
                } else if (error.response) {
                    setErrorMessage(
                        'The authentication request could not be completed.'
                    )
                } else {
                    setErrorMessage(
                        'The authentication request could not reach '
                        + 'the Auth Service. Confirm that the service '
                        + 'is running and permits this frontend origin.'
                    )
                }
            } else {
                setErrorMessage(
                    'An unexpected authentication error occurred.'
                )
            }
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <div className="login-page">
            <section className="login-panel">
                <div className="login-brand">
          <span className="brand-mark">
            KB
          </span>

                    <div>
                        <h1>KasiBridge</h1>

                        <p>
                            Trader and Supplier Portal
                        </p>
                    </div>
                </div>

                <div className="login-introduction">
                    <h2>Welcome back</h2>

                    <p>
                        Sign in to access tenders, bid updates,
                        official clarifications, support tickets
                        and notifications.
                    </p>
                </div>

                <form onSubmit={handleSubmit}>
                    <label htmlFor="username">
                        Username
                    </label>

                    <input
                        id="username"
                        type="text"
                        autoComplete="username"
                        value={username}
                        onChange={(event) =>
                            setUsername(event.target.value)
                        }
                        disabled={isSubmitting}
                        required
                    />

                    <label htmlFor="password">
                        Password
                    </label>

                    <input
                        id="password"
                        type="password"
                        autoComplete="current-password"
                        value={password}
                        onChange={(event) =>
                            setPassword(event.target.value)
                        }
                        disabled={isSubmitting}
                        required
                    />

                    {errorMessage && (
                        <div
                            className="error-message"
                            role="alert"
                        >
                            {errorMessage}
                        </div>
                    )}

                    <button
                        className="primary-button"
                        type="submit"
                        disabled={isSubmitting}
                    >
                        {isSubmitting
                            ? 'Signing in...'
                            : 'Sign in'}
                    </button>
                </form>

                <p className="login-note">
                    Authoritative procurement records remain
                    within the KasiBridge platform.
                </p>
            </section>

            <section className="login-information">
                <div>
          <span className="eyebrow">
            Township business participation
          </span>

                    <h2>
                        Discover opportunities and follow every
                        procurement step.
                    </h2>

                    <p>
                        Use the dashboard to maintain your
                        business profile, view open tenders,
                        monitor bids and receive official
                        procurement updates.
                    </p>
                </div>
            </section>
        </div>
    )
}