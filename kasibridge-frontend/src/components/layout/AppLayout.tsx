import type { ReactNode } from 'react'

import {
    NavLink,
    useNavigate,
} from 'react-router-dom'

import { useAuth } from '../../auth/useAuth'

interface AppLayoutProps {
    children: ReactNode
}

export function AppLayout({
                              children,
                          }: AppLayoutProps) {
    const navigate = useNavigate()

    const {
        user,
        logout,
    } = useAuth()

    function handleLogout(): void {
        logout()

        navigate('/login', {
            replace: true,
        })
    }

    return (
        <div className="application-shell">
            <aside className="sidebar">
                <div className="brand">
          <span className="brand-mark">
            KB
          </span>

                    <div>
                        <strong>KasiBridge</strong>

                        <small>
                            Trader Dashboard
                        </small>
                    </div>
                </div>

                <nav
                    className="navigation"
                    aria-label="Trader navigation"
                >
                    <NavLink to="/dashboard">
                        Dashboard
                    </NavLink>

                    <NavLink to="/tenders">
                        Open tenders
                    </NavLink>

                    <NavLink to="/bids">
                        My bids
                    </NavLink>

                    <NavLink to="/support-tickets">
                        Support tickets
                    </NavLink>

                    <NavLink to="/notifications">
                        Notifications
                    </NavLink>

                    <NavLink to="/clarifications">
                        Official clarifications
                    </NavLink>

                    <NavLink to="/profile">
                        Business profile
                    </NavLink>
                </nav>
            </aside>

            <div className="main-area">
                <header className="topbar">
                    <div>
                        <strong>
                            Trader Web Dashboard
                        </strong>

                        {user && (
                            <small className="current-user">
                                Signed in as {user.username}
                            </small>
                        )}
                    </div>

                    <button
                        className="secondary-button"
                        type="button"
                        onClick={handleLogout}
                    >
                        Sign out
                    </button>
                </header>

                <main className="page-content">
                    {children}
                </main>
            </div>
        </div>
    )
}