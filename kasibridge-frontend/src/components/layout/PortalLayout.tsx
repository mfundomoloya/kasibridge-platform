import type {
    ReactNode,
} from 'react'

import {
    NavLink,
    useNavigate,
} from 'react-router-dom'

import type {
    UserRole,
} from '../../types/auth'

import {
    useAuth,
} from '../../auth/useAuth'

export interface NavigationItem {
    label: string
    path: string
    roles?: UserRole[]
    badge?: number
}

interface PortalLayoutProps {
    portalName: string
    navigationItems: NavigationItem[]
    children: ReactNode
}

export function PortalLayout({
                                 portalName,
                                 navigationItems,
                                 children,
                             }: PortalLayoutProps) {
    const navigate = useNavigate()

    const {
        user,
        logout,
        hasAnyRole,
    } = useAuth()

    const visibleNavigationItems =
        navigationItems.filter((item) => {
            if (!item.roles) {
                return true
            }

            return hasAnyRole(item.roles)
        })

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

                        <small>{portalName}</small>
                    </div>
                </div>

                <nav
                    className="navigation"
                    aria-label={
                        `${portalName} navigation`
                    }
                >
                    {visibleNavigationItems.map(
                        (item) => (
                            <NavLink
                                key={item.path}
                                to={item.path}
                            >
                                <span>
                                    {item.label}
                                </span>

                                {item.badge !== undefined
                                    && item.badge > 0 && (
                                        <span
                                            className={
                                                'navigation-badge'
                                            }
                                            aria-label={
                                                `${item.badge} unread`
                                            }
                                        >
                                            {item.badge > 99
                                                ? '99+'
                                                : item.badge}
                                        </span>
                                    )}
                            </NavLink>
                        )
                    )}
                </nav>
            </aside>

            <div className="main-area">
                <header className="topbar">
                    <div>
                        <strong>
                            {portalName}
                        </strong>

                        {user && (
                            <small
                                className="current-user"
                            >
                                Signed in as{' '}
                                {user.username}
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