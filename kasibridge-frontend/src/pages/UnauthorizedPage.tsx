import { useNavigate } from 'react-router-dom'

import { useAuth } from '../auth/useAuth'

import './UnauthorizedPage.css'

export function UnauthorizedPage() {
    const navigate = useNavigate()

    const {
        user,
        logout,
    } = useAuth()

    function returnToPortal(): void {
        navigate('/portal', {
            replace: true,
        })
    }

    function handleLogout(): void {
        logout()

        navigate('/login', {
            replace: true,
        })
    }

    return (
        <div className="status-page">
            <section className="status-card">
                <div
                    className="status-icon"
                    aria-hidden="true"
                >
                    <svg
                        viewBox="0 0 48 48"
                        focusable="false"
                    >
                        <path
                            d="M24 5 39 11v11c0 9.5-6.2 16.8-15 21C15.2 38.8 9 31.5 9 22V11l15-6Z"
                        />
                        <path
                            d="m17.5 24 4.2 4.2L31 19"
                        />
                    </svg>
                </div>

                <span className="status-code">
                    Error 403
                </span>

                <h1>Access not authorised</h1>

                <p className="status-description">
                    This area is restricted to authorised
                    KasiBridge roles. Your account is signed
                    in, but does not have the access required
                    to continue.
                </p>

                {user && (
                    <div className="status-user">
                        <span className="status-user-label">
                            Signed in as
                        </span>

                        <strong>
                            {user.username}
                        </strong>
                    </div>
                )}

                <div className="status-actions">
                    <button
                        className="primary-button"
                        type="button"
                        onClick={returnToPortal}
                    >
                        Return to my portal
                    </button>

                    <button
                        className="secondary-button"
                        type="button"
                        onClick={handleLogout}
                    >
                        Sign out
                    </button>
                </div>

                <p className="status-help">
                    If you believe this is a mistake, contact
                    your KasiBridge administrator.
                </p>
            </section>
        </div>
    )
}