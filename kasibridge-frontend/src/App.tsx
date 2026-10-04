import {
    Navigate,
    Route,
    Routes,
} from 'react-router-dom'

import {
    PortalRedirect,
} from './auth/PortalRedirect'

import {
    ProtectedRoute,
} from './auth/ProtectedRoute'

import {
    RoleRoute,
} from './auth/RoleRoute'

import {
    AdminDashboardPage,
} from './pages/AdminDashboardPage'

import { LoginPage } from './pages/LoginPage'

import {
    ProcurementDashboardPage,
} from './pages/ProcurementDashboardPage'

import {
    TraderDashboardPage,
} from './pages/TraderDashboardPage'

import {
    UnauthorizedPage,
} from './pages/UnauthorizedPage'

function App() {
    return (
        <Routes>
            <Route
                path="/"
                element={
                    <Navigate
                        to="/portal"
                        replace
                    />
                }
            />

            <Route
                path="/login"
                element={<LoginPage />}
            />

            <Route
                path="/portal"
                element={
                    <ProtectedRoute>
                        <PortalRedirect />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/dashboard"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_TRADER',
                        ]}
                    >
                        <TraderDashboardPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/admin"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_PLATFORM_ADMIN',
                        ]}
                    >
                        <AdminDashboardPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/procurement"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_ANALYST',
                            'ROLE_SPECIFICATION_OFFICER',
                            'ROLE_EVALUATOR',
                            'ROLE_ADJUDICATOR',
                        ]}
                    >
                        <ProcurementDashboardPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/unauthorized"
                element={
                    <ProtectedRoute>
                        <UnauthorizedPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="*"
                element={
                    <Navigate
                        to="/portal"
                        replace
                    />
                }
            />
        </Routes>
    )
}

export default App