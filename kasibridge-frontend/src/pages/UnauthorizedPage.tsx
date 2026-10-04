import { useNavigate } from 'react-router-dom'

import { useAuth } from '../auth/useAuth'

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
        <span className="status-code">
          403
        </span>

                <h1>Access not authorised</h1>

                <p>
                    The signed-in account does not have the
                    required role for this KasiBridge area.
                </p>

                {user && (
                    <p className="current-user">
                        Signed in as {user.username}
                    </p>
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
            </section>
        </div>
    )
}