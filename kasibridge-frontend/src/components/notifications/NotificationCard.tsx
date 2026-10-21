import {
    Link,
} from 'react-router-dom'

import {
    NOTIFICATION_TEMPLATE_LABELS,
} from '../../types/notification'

import type {
    Notification,
} from '../../types/notification'

import {
    formatDateTime,
} from '../../utils/tenderFormatters'

interface NotificationCardProps {
    notification: Notification
    isMarkingRead?: boolean
    isHighlighted?: boolean
    onMarkRead?: (
        notification: Notification
    ) => void
}

function getRelatedItemLink(
    notification: Notification
): string | null {
    if (
        notification.relatedTicketId !== null
    ) {
        return (
            '/support-tickets/'
            + notification.relatedTicketId
        )
    }

    if (
        notification.relatedTenderId !== null
    ) {
        return (
            '/tenders/'
            + notification.relatedTenderId
        )
    }

    if (
        notification.relatedBidId !== null
    ) {
        return '/my-bids'
    }

    return null
}

function getRelatedItemLabel(
    notification: Notification
): string {
    if (
        notification.relatedTicketId !== null
    ) {
        return 'View support ticket'
    }

    if (
        notification.relatedTenderId !== null
    ) {
        return 'View tender'
    }

    if (
        notification.relatedBidId !== null
    ) {
        return 'View my bids'
    }

    return 'View details'
}

export function NotificationCard({
                                     notification,
                                     isMarkingRead = false,
                                     isHighlighted = false,
                                     onMarkRead,
                                 }: NotificationCardProps) {
    const relatedItemLink =
        getRelatedItemLink(notification)

    const templateLabel =
        NOTIFICATION_TEMPLATE_LABELS[
            notification.templateType
            ]

    return (
        <article
            id={`notification-${notification.id}`}
            className={
                'content-card notification-card '
                + (
                    notification.read
                        ? 'notification-read'
                        : 'notification-unread'
                )
                + (
                    isHighlighted
                        ? ' notification-highlighted'
                        : ''
                )
            }
            aria-current={
                isHighlighted
                    ? 'true'
                    : undefined
            }
        >
            <div className="notification-card-header">
                <div>
                    <span className="eyebrow">
                        {notification.notificationReference}
                    </span>

                    <h2>{templateLabel}</h2>
                </div>

                <span
                    className={
                        'notification-read-badge '
                        + (
                            notification.read
                                ? 'is-read'
                                : 'is-unread'
                        )
                    }
                >
                    {notification.read
                        ? 'Read'
                        : 'Unread'}
                </span>
            </div>

            <p className="notification-message">
                {notification.message}
            </p>

            <dl className="profile-details">
                <div>
                    <dt>Received</dt>

                    <dd>
                        {formatDateTime(
                            notification.createdAt
                        )}
                    </dd>
                </div>

                {notification.readAt && (
                    <div>
                        <dt>Read</dt>

                        <dd>
                            {formatDateTime(
                                notification.readAt
                            )}
                        </dd>
                    </div>
                )}

                {notification.relatedTenderId
                    !== null && (
                        <div>
                            <dt>Tender ID</dt>

                            <dd>
                                {
                                    notification
                                        .relatedTenderId
                                }
                            </dd>
                        </div>
                    )}

                {notification.relatedBidId
                    !== null && (
                        <div>
                            <dt>Bid ID</dt>

                            <dd>
                                {
                                    notification
                                        .relatedBidId
                                }
                            </dd>
                        </div>
                    )}

                {notification.relatedTicketId
                    !== null && (
                        <div>
                            <dt>Ticket ID</dt>

                            <dd>
                                {
                                    notification
                                        .relatedTicketId
                                }
                            </dd>
                        </div>
                    )}
            </dl>

            <div className="form-actions">
                {!notification.read
                    && onMarkRead && (
                        <button
                            className="primary-button"
                            type="button"
                            disabled={isMarkingRead}
                            onClick={() => {
                                onMarkRead(notification)
                            }}
                        >
                            {isMarkingRead
                                ? 'Marking as read...'
                                : 'Mark as read'}
                        </button>
                    )}

                {relatedItemLink && (
                    <Link
                        className="secondary-link-button"
                        to={relatedItemLink}
                    >
                        {getRelatedItemLabel(
                            notification
                        )}
                    </Link>
                )}
            </div>
        </article>
    )
}