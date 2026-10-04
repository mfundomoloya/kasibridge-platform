import {
    Navigate,
    Route,
    Routes,
} from 'react-router-dom'

import { ProtectedRoute } from './auth/ProtectedRoute'

import { LoginPage } from './pages/LoginPage'

import {
    TraderDashboardPage,
} from './pages/TraderDashboardPage'

function App() {
    return (
        <Routes>
            <Route
                path="/"
                element={
                    <Navigate
                        to="/dashboard"
                        replace
                    />
                }
            />

            <Route
                path="/login"
                element={<LoginPage />}
            />

            <Route
                path="/dashboard"
                element={
                    <ProtectedRoute>
                        <TraderDashboardPage />
                    </ProtectedRoute>
                }
            />

            <Route
                path="*"
                element={
                    <Navigate
                        to="/dashboard"
                        replace
                    />
                }
            />
        </Routes>
    )
}

export default App