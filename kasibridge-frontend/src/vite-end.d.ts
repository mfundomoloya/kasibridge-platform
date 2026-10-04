/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_AUTH_API_BASE_URL: string
    readonly VITE_TRADER_PROFILE_API_BASE_URL: string
    readonly VITE_APP_NAME: string
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}