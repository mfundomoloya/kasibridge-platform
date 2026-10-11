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

import {
    CreateSupportTicketPage,
} from './pages/CreateSupportTicketPage'

import {
    LoginPage,
} from './pages/LoginPage'

import {
    MyBidsPage,
} from './pages/MyBidsPage'

import {
    OpenTendersPage,
} from './pages/OpenTendersPage'

import {
    ProcurementDashboardPage,
} from './pages/ProcurementDashboardPage'

import {
    SupportTicketDetailsPage,
} from './pages/SupportTicketDetailsPage'

import {
    SupportTicketsPage,
} from './pages/SupportTicketsPage'

import {
    TenderDetailsPage,
} from './pages/TenderDetailsPage'

import {
    TraderDashboardPage,
} from './pages/TraderDashboardPage'

import {
    TraderProfilePage,
} from './pages/TraderProfilePage'

import {
    OfficialClarificationsPage,
} from './pages/OfficialClarificationsPage'

import {
    NotificationsPage,
} from './pages/NotificationsPage'

import {
    ProcurementSupportQueuePage,
} from './pages/ProcurementSupportQueuePage'

import {
    ProcurementSupportTicketDetailsPage,
} from './pages/ProcurementSupportTicketDetailsPage'

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
                path="/tenders"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_TRADER',
                        ]}
                    >
                        <OpenTendersPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/tenders/:tenderId"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_TRADER',
                        ]}
                    >
                        <TenderDetailsPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/my-bids"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_TRADER',
                        ]}
                    >
                        <MyBidsPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/support-tickets"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_TRADER',
                        ]}
                    >
                        <SupportTicketsPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/support-tickets/new"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_TRADER',
                        ]}
                    >
                        <CreateSupportTicketPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/support-tickets/:ticketId"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_TRADER',
                        ]}
                    >
                        <SupportTicketDetailsPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/clarifications"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_TRADER',
                        ]}
                    >
                        <OfficialClarificationsPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/notifications"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_TRADER',
                        ]}
                    >
                        <NotificationsPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/profile"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_TRADER',
                        ]}
                    >
                        <TraderProfilePage />
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
                            'ROLE_PLATFORM_ADMIN',
                        ]}
                    >
                        <ProcurementDashboardPage />
                    </RoleRoute>
                }
            />

            <Route
                path="/procurement/support-tickets"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_SPECIFICATION_OFFICER',
                            'ROLE_ADJUDICATOR',
                            'ROLE_PLATFORM_ADMIN',
                        ]}
                    >
                        <ProcurementSupportQueuePage />
                    </RoleRoute>
                }
            />

            <Route
                path="/procurement/support-tickets/:ticketId"
                element={
                    <RoleRoute
                        allowedRoles={[
                            'ROLE_SPECIFICATION_OFFICER',
                            'ROLE_ADJUDICATOR',
                            'ROLE_PLATFORM_ADMIN',
                        ]}
                    >
                        <ProcurementSupportTicketDetailsPage />
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