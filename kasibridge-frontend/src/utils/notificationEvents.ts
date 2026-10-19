export const NOTIFICATION_READ_STATE_CHANGED =
    'kasibridge:notification-read-state-changed'

const NOTIFICATION_CHANNEL_NAME =
    'kasibridge-notification-events'

const NOTIFICATION_STORAGE_KEY =
    'kasibridge:notification-read-state-revision'

interface NotificationChangeMessage {
    type: typeof NOTIFICATION_READ_STATE_CHANGED
    revision: number
}

function createChangeMessage():
    NotificationChangeMessage {
    return {
        type: NOTIFICATION_READ_STATE_CHANGED,
        revision: Date.now(),
    }
}

function supportsBroadcastChannel(): boolean {
    return (
        typeof globalThis.BroadcastChannel
        !== 'undefined'
    )
}

function dispatchCurrentTabEvent(): void {
    window.dispatchEvent(
        new Event(
            NOTIFICATION_READ_STATE_CHANGED
        )
    )
}

export function notifyNotificationReadStateChanged():
    void {
    if (typeof window === 'undefined') {
        return
    }

    const message =
        createChangeMessage()

    dispatchCurrentTabEvent()

    if (supportsBroadcastChannel()) {
        const channel =
            new globalThis.BroadcastChannel(
                NOTIFICATION_CHANNEL_NAME
            )

        channel.postMessage(message)
        channel.close()

        return
    }

    try {
        globalThis.localStorage.setItem(
            NOTIFICATION_STORAGE_KEY,
            JSON.stringify(message)
        )
    } catch {
        /*
         * The current tab has already been notified.
         * Cross-tab synchronization is unavailable
         * when local-storage access is blocked.
         */
    }
}

export function subscribeToNotificationReadStateChanges(
    listener: () => void
): () => void {
    if (typeof window === 'undefined') {
        return () => undefined
    }

    function handleCurrentTabEvent() {
        listener()
    }

    window.addEventListener(
        NOTIFICATION_READ_STATE_CHANGED,
        handleCurrentTabEvent
    )

    if (supportsBroadcastChannel()) {
        const channel =
            new globalThis.BroadcastChannel(
                NOTIFICATION_CHANNEL_NAME
            )

        function handleChannelMessage(
            event: MessageEvent<unknown>
        ) {
            if (
                typeof event.data !== 'object'
                || event.data === null
                || !('type' in event.data)
            ) {
                return
            }

            const message =
                event.data as
                    Partial<NotificationChangeMessage>

            if (
                message.type
                === NOTIFICATION_READ_STATE_CHANGED
            ) {
                listener()
            }
        }

        channel.addEventListener(
            'message',
            handleChannelMessage
        )

        return () => {
            window.removeEventListener(
                NOTIFICATION_READ_STATE_CHANGED,
                handleCurrentTabEvent
            )

            channel.removeEventListener(
                'message',
                handleChannelMessage
            )

            channel.close()
        }
    }

    function handleStorageEvent(
        event: StorageEvent
    ) {
        if (
            event.key
            !== NOTIFICATION_STORAGE_KEY
            || event.newValue === null
        ) {
            return
        }

        try {
            const parsedMessage =
                JSON.parse(
                    event.newValue
                ) as unknown

            if (
                typeof parsedMessage !== 'object'
                || parsedMessage === null
                || !('type' in parsedMessage)
            ) {
                return
            }

            const message =
                parsedMessage as
                    Partial<NotificationChangeMessage>

            if (
                message.type
                === NOTIFICATION_READ_STATE_CHANGED
            ) {
                listener()
            }
        } catch {
            /*
             * Ignore malformed events written by
             * older or unrelated browser clients.
             */
        }
    }

    window.addEventListener(
        'storage',
        handleStorageEvent
    )

    return () => {
        window.removeEventListener(
            NOTIFICATION_READ_STATE_CHANGED,
            handleCurrentTabEvent
        )

        window.removeEventListener(
            'storage',
            handleStorageEvent
        )
    }
}