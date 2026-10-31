import type {
    ReactNode,
} from 'react'

export type FraudMetricTone =
    | 'default'
    | 'positive'
    | 'warning'
    | 'danger'
    | 'info'

interface FraudMetricCardProps {
    label: string
    value: number | string
    description: string
    tone?: FraudMetricTone
    isLoading?: boolean
    footer?: ReactNode
}

export function FraudMetricCard({
                                    label,
                                    value,
                                    description,
                                    tone = 'default',
                                    isLoading = false,
                                    footer,
                                }: FraudMetricCardProps) {
    return (
        <article
            className={
                'fraud-metric-card '
                + `fraud-metric-${tone}`
            }
            aria-busy={isLoading}
        >
            <span className="fraud-metric-label">
                {label}
            </span>

            {isLoading ? (
                <span
                    className="fraud-metric-loading"
                    aria-label={
                        `Loading ${label}`
                    }
                >
                    <span
                        className={
                            'fraud-metric-loading-bar'
                        }
                        aria-hidden="true"
                    />
                </span>
            ) : (
                <strong className="fraud-metric-value">
                    {value}
                </strong>
            )}

            <p className="fraud-metric-description">
                {description}
            </p>

            {footer && (
                <div className="fraud-metric-footer">
                    {footer}
                </div>
            )}
        </article>
    )
}