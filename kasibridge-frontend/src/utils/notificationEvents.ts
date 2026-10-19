export const NOTIFICATION_READ_STATE_CHANGED =
    'kasibridge:notification-read-state-changed'

export function notifyNotificationReadStateChanged(): void {
    window.dispatchEvent(
        new Event(
            NOTIFICATION_READ_STATE_CHANGED
        )
    )
}