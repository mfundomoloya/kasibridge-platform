import type {
    CSSProperties,
} from 'react'

import {
    AI_ASSESSMENT_STATUS_LABELS,
} from '../../types/supportTicketAiAssessment'

import type {
    TicketAiAssessmentStatus,
} from '../../types/supportTicketAiAssessment'

interface TicketAiAssessmentStatusBadgeProps {
    status: TicketAiAssessmentStatus
}

interface StatusTheme {
    backgroundColor: string
    borderColor: string
    color: string
}

const STATUS_THEMES:
    Record<TicketAiAssessmentStatus, StatusTheme> = {
    GENERATED: {
        backgroundColor: '#eff6ff',
        borderColor: '#93c5fd',
        color: '#1d4ed8',
    },
    APPROVED: {
        backgroundColor: '#ecfdf5',
        borderColor: '#6ee7b7',
        color: '#047857',
    },
    EDITED_AND_APPROVED: {
        backgroundColor: '#f0fdf4',
        borderColor: '#86efac',
        color: '#166534',
    },
    RESPONSE_PUBLISHED: {
        backgroundColor: '#f5f3ff',
        borderColor: '#c4b5fd',
        color: '#6d28d9',
    },
    REJECTED: {
        backgroundColor: '#fef2f2',
        borderColor: '#fca5a5',
        color: '#b91c1c',
    },
    ESCALATED: {
        backgroundColor: '#fff7ed',
        borderColor: '#fdba74',
        color: '#c2410c',
    },
}

const badgeStyle: CSSProperties = {
    alignItems: 'center',
    borderRadius: '9999px',
    borderStyle: 'solid',
    borderWidth: '1px',
    display: 'inline-flex',
    fontSize: '0.75rem',
    fontWeight: 700,
    letterSpacing: '0.025em',
    lineHeight: 1,
    padding: '0.4rem 0.7rem',
    textTransform: 'uppercase',
    whiteSpace: 'nowrap',
}

export function TicketAiAssessmentStatusBadge({
                                                  status,
                                              }: TicketAiAssessmentStatusBadgeProps) {
    const theme = STATUS_THEMES[status]

    return (
        <span
            aria-label={
                `AI assessment status: ${
                    AI_ASSESSMENT_STATUS_LABELS[status]
                }`
            }
            style={{
                ...badgeStyle,
                ...theme,
            }}
        >
            {AI_ASSESSMENT_STATUS_LABELS[status]}
        </span>
    )
}