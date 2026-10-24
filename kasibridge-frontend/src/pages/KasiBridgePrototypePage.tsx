import {
    useMemo,
    useReducer,
} from 'react'

import {
    Link,
} from 'react-router-dom'

import './KasiBridgePrototypePage.css'

type PrototypeScenario =
    | 'NORMAL'
    | 'MANIPULATION_RISK'

type PrototypeStep =
    | 'TENDER_SETUP'
    | 'PUBLICATION'
    | 'MATCHING'
    | 'BIDDING'
    | 'COMPLIANCE'
    | 'EVALUATION'
    | 'INTEGRITY'
    | 'AUDIT'

interface AuditEvent {
    id: number
    action: string
    actor: string
    result: 'SUCCESS' | 'BLOCKED'
    evidence: string
    timestamp: string
    hash: string
}

interface PrototypeState {
    currentStepIndex: number
    scenario: PrototypeScenario
    completedSteps: PrototypeStep[]
    tenderReference: string
    specificationHash: string | null
    auditEvents: AuditEvent[]
}

type PrototypeAction =
    | {
    type: 'RESET'
}
    | {
    type: 'SET_SCENARIO'
    scenario: PrototypeScenario
}
    | {
    type: 'COMPLETE_CURRENT_STEP'
}
    | {
    type: 'GO_TO_STEP'
    stepIndex: number
}
    | {
    type: 'PREVIOUS_STEP'
}
    | {
    type: 'NEXT_STEP'
}

interface StepDefinition {
    id: PrototypeStep
    shortLabel: string
    title: string
    description: string
    actionLabel: string
}

const STEPS:
    StepDefinition[] = [
    {
        id: 'TENDER_SETUP',
        shortLabel: 'Tender',
        title: 'Create a controlled tender',
        description:
            'Create a draft tender using structured '
            + 'procurement criteria.',
        actionLabel: 'Create draft tender',
    },
    {
        id: 'PUBLICATION',
        shortLabel: 'Publish',
        title: 'Lock and publish the tender',
        description:
            'Generate the specification hash and establish '
            + 'the immutable publication record.',
        actionLabel: 'Publish and lock',
    },
    {
        id: 'MATCHING',
        shortLabel: 'Notify',
        title: 'Match and notify eligible traders',
        description:
            'Find active traders using business type and '
            + 'service area, then queue durable notifications.',
        actionLabel: 'Dispatch notifications',
    },
    {
        id: 'BIDDING',
        shortLabel: 'Bids',
        title: 'Receive anonymous bids',
        description:
            'Accept three synthetic bids and assign controlled '
            + 'aliases before evaluation.',
        actionLabel: 'Submit demo bids',
    },
    {
        id: 'COMPLIANCE',
        shortLabel: 'Compliance',
        title: 'Run mandatory compliance checks',
        description:
            'Prevent noncompliant bids from progressing through '
            + 'the normal procurement workflow.',
        actionLabel: 'Run compliance checks',
    },
    {
        id: 'EVALUATION',
        shortLabel: 'Evaluation',
        title: 'Evaluate bidders anonymously',
        description:
            'Score only compliant bids without showing supplier '
            + 'identities to evaluators.',
        actionLabel: 'Complete evaluation',
    },
    {
        id: 'INTEGRITY',
        shortLabel: 'AI gate',
        title: 'Assess award integrity',
        description:
            'Combine deterministic controls with a supervised '
            + 'AI risk classification.',
        actionLabel: 'Run integrity assessment',
    },
    {
        id: 'AUDIT',
        shortLabel: 'Evidence',
        title: 'Review immutable evidence',
        description:
            'Inspect the complete demonstration audit trail.',
        actionLabel: 'Finalise demonstration',
    },
]

const INITIAL_STATE:
    PrototypeState = {
    currentStepIndex: 0,
    scenario: 'MANIPULATION_RISK',
    completedSteps: [],
    tenderReference: 'KB-DEMO-6FBA9BFB',
    specificationHash: null,
    auditEvents: [],
}

function demoTimestamp(
    sequence: number
): string {
    const minute =
        String(10 + sequence)
            .padStart(2, '0')

    return `10:${minute}:00`
}

function demoHash(
    sequence: number
): string {
    const suffix =
        String(sequence)
            .padStart(4, '0')

    return `8f5d7c9a…${suffix}`
}

function buildAuditEvent(
    step: PrototypeStep,
    scenario: PrototypeScenario,
    sequence: number
): AuditEvent {
    const definitions:
        Record<
            PrototypeStep,
            Omit<
                AuditEvent,
                'id'
                | 'timestamp'
                | 'hash'
            >
        > = {
        TENDER_SETUP: {
            action: 'TENDER_CREATED',
            actor: 'Specification Officer',
            result: 'SUCCESS',
            evidence:
                'Structured matching criteria and budget recorded.',
        },
        PUBLICATION: {
            action: 'TENDER_PUBLISHED',
            actor: 'Specification Officer',
            result: 'SUCCESS',
            evidence:
                'Specification hash generated and publication locked.',
        },
        MATCHING: {
            action: 'MATCHING_TRADERS_NOTIFIED',
            actor: 'KasiBridge System',
            result: 'SUCCESS',
            evidence:
                'Three approved WhatsApp template messages accepted.',
        },
        BIDDING: {
            action: 'BIDS_SUBMITTED',
            actor: 'Trader Accounts',
            result: 'SUCCESS',
            evidence:
                'Three sealed bids received before the deadline.',
        },
        COMPLIANCE: {
            action: 'BID_COMPLIANCE_COMPLETED',
            actor: 'Compliance Gate',
            result: 'SUCCESS',
            evidence:
                'Two bids passed. One bid failed mandatory compliance.',
        },
        EVALUATION: {
            action: 'ANONYMOUS_EVALUATION_COMPLETED',
            actor: 'Evaluation Committee',
            result: 'SUCCESS',
            evidence:
                'BID-CHARLIE ranked first with a validated score of 91.',
        },
        INTEGRITY: scenario
        === 'MANIPULATION_RISK'
            ? {
                action: 'AWARD_BLOCKED',
                actor: 'Procurement Integrity Gate',
                result: 'BLOCKED',
                evidence:
                    'Noncompliant bidder, conflict link and score bypass detected.',
            }
            : {
                action: 'AWARD_RISK_ASSESSED',
                actor: 'Procurement Integrity Gate',
                result: 'SUCCESS',
                evidence:
                    'Highest compliant bidder selected with no critical conflicts.',
            },
        AUDIT: {
            action: 'AUDIT_CHAIN_VERIFIED',
            actor: 'Platform Auditor',
            result: 'SUCCESS',
            evidence:
                'All demonstration events are complete and hash-linked.',
        },
    }

    return {
        id: sequence,
        ...definitions[step],
        timestamp:
            demoTimestamp(sequence),
        hash:
            demoHash(sequence),
    }
}

function prototypeReducer(
    state: PrototypeState,
    action: PrototypeAction
): PrototypeState {
    if (
        action.type === 'RESET'
    ) {
        return INITIAL_STATE
    }

    if (
        action.type === 'SET_SCENARIO'
    ) {
        return {
            ...INITIAL_STATE,
            scenario: action.scenario,
        }
    }

    if (
        action.type === 'GO_TO_STEP'
    ) {
        const highestUnlockedIndex =
            Math.min(
                state.completedSteps.length,
                STEPS.length - 1
            )

        return {
            ...state,
            currentStepIndex:
                Math.min(
                    action.stepIndex,
                    highestUnlockedIndex
                ),
        }
    }

    if (
        action.type === 'PREVIOUS_STEP'
    ) {
        return {
            ...state,
            currentStepIndex:
                Math.max(
                    state.currentStepIndex - 1,
                    0
                ),
        }
    }

    if (
        action.type === 'NEXT_STEP'
    ) {
        const highestUnlockedIndex =
            Math.min(
                state.completedSteps.length,
                STEPS.length - 1
            )

        return {
            ...state,
            currentStepIndex:
                Math.min(
                    state.currentStepIndex + 1,
                    highestUnlockedIndex
                ),
        }
    }

    const currentStep =
        STEPS[state.currentStepIndex]

    if (
        state.completedSteps.includes(
            currentStep.id
        )
    ) {
        return state
    }

    const completedSteps = [
        ...state.completedSteps,
        currentStep.id,
    ]

    const auditEvent =
        buildAuditEvent(
            currentStep.id,
            state.scenario,
            completedSteps.length
        )

    return {
        ...state,
        completedSteps,
        specificationHash:
            currentStep.id === 'PUBLICATION'
                ? 'e6234dd53ea31e868c2e4b6631398f6d47b4fd6c2c2f6ec42c5117c8ebcfaccc'
                : state.specificationHash,
        auditEvents: [
            ...state.auditEvents,
            auditEvent,
        ],
        currentStepIndex:
            Math.min(
                state.currentStepIndex + 1,
                STEPS.length - 1
            ),
    }
}

function isStepCompleted(
    state: PrototypeState,
    step: PrototypeStep
): boolean {
    return state.completedSteps.includes(
        step
    )
}

export function KasiBridgePrototypePage() {
    const [
        state,
        dispatch,
    ] = useReducer(
        prototypeReducer,
        INITIAL_STATE
    )

    const currentStep =
        STEPS[state.currentStepIndex]

    const currentStepCompleted =
        isStepCompleted(
            state,
            currentStep.id
        )

    const isHighRisk =
        state.scenario
        === 'MANIPULATION_RISK'

    const riskResult =
        useMemo(
            () => {
                if (isHighRisk) {
                    return {
                        probability: '94%',
                        level: 'CRITICAL RISK',
                        decision:
                            'BLOCK AWARD AND REQUIRE REVIEW',
                        selectedBid:
                            'BID-BRAVO',
                    }
                }

                return {
                    probability: '18%',
                    level: 'LOW RISK',
                    decision:
                        'ALLOW WITH MONITORING',
                    selectedBid:
                        'BID-CHARLIE',
                }
            },
            [isHighRisk]
        )

    const progress =
        Math.round(
            (
                state.completedSteps.length
                / STEPS.length
            ) * 100
        )

    return (
        <div className="prototype-page">
            <header className="prototype-topbar">
                <div className="prototype-brand">
                    <span className="prototype-brand-mark">
                        KB
                    </span>

                    <div>
                        <strong>
                            KasiBridge
                        </strong>

                        <small>
                            Interactive procurement
                            prototype
                        </small>
                    </div>
                </div>

                <div className="prototype-disclaimer">
                    Synthetic demonstration data
                </div>

                <Link
                    className="prototype-exit"
                    to="/showcase"
                >
                    Return to showcase
                </Link>
            </header>

            <div className="prototype-layout">
                <aside className="prototype-sidebar">
                    <div className="prototype-sidebar-heading">
                        <span>
                            Demo journey
                        </span>

                        <strong>
                            {progress}% complete
                        </strong>
                    </div>

                    <div className="prototype-progress">
                        <span
                            style={{
                                width:
                                    `${progress}%`,
                            }}
                        />
                    </div>

                    <nav
                        className="prototype-step-navigation"
                        aria-label="Prototype steps"
                    >
                        {STEPS.map(
                            (step, index) => {
                                const completed =
                                    isStepCompleted(
                                        state,
                                        step.id
                                    )

                                const active =
                                    index
                                    === state
                                        .currentStepIndex

                                const unlocked =
                                    index
                                    <= state
                                        .completedSteps
                                        .length

                                return (
                                    <button
                                        key={step.id}
                                        type="button"
                                        className={[
                                            completed
                                                ? 'completed'
                                                : '',
                                            active
                                                ? 'active'
                                                : '',
                                        ]
                                            .filter(Boolean)
                                            .join(' ')}
                                        disabled={
                                            !unlocked
                                        }
                                        onClick={() => {
                                            dispatch({
                                                type:
                                                    'GO_TO_STEP',
                                                stepIndex:
                                                index,
                                            })
                                        }}
                                    >
                                        <span>
                                            {completed
                                                ? '✓'
                                                : index + 1}
                                        </span>

                                        <div>
                                            <strong>
                                                {
                                                    step.shortLabel
                                                }
                                            </strong>

                                            <small>
                                                {step.title}
                                            </small>
                                        </div>
                                    </button>
                                )
                            }
                        )}
                    </nav>
                </aside>

                <main className="prototype-main">
                    <section className="prototype-control-panel">
                        <div>
                            <span className="prototype-eyebrow">
                                Demonstration controls
                            </span>

                            <h1>
                                {currentStep.title}
                            </h1>

                            <p>
                                {currentStep.description}
                            </p>
                        </div>

                        <div className="prototype-scenario-control">
                            <span>
                                Award scenario
                            </span>

                            <div>
                                <button
                                    type="button"
                                    className={
                                        !isHighRisk
                                            ? 'active'
                                            : ''
                                    }
                                    onClick={() => {
                                        dispatch({
                                            type:
                                                'SET_SCENARIO',
                                            scenario:
                                                'NORMAL',
                                        })
                                    }}
                                >
                                    Normal
                                </button>

                                <button
                                    type="button"
                                    className={
                                        isHighRisk
                                            ? 'active danger'
                                            : ''
                                    }
                                    onClick={() => {
                                        dispatch({
                                            type:
                                                'SET_SCENARIO',
                                            scenario:
                                                'MANIPULATION_RISK',
                                        })
                                    }}
                                >
                                    Manipulation risk
                                </button>
                            </div>
                        </div>
                    </section>

                    <section className="prototype-workspace">
                        <PrototypeStepContent
                            state={state}
                            riskResult={riskResult}
                        />
                    </section>

                    <section className="prototype-actions">
                        <button
                            type="button"
                            className="prototype-reset"
                            onClick={() => {
                                dispatch({
                                    type: 'RESET',
                                })
                            }}
                        >
                            Reset demo
                        </button>

                        <div>
                            <button
                                type="button"
                                className="prototype-secondary"
                                disabled={
                                    state.currentStepIndex
                                    === 0
                                }
                                onClick={() => {
                                    dispatch({
                                        type:
                                            'PREVIOUS_STEP',
                                    })
                                }}
                            >
                                Previous
                            </button>

                            {!currentStepCompleted && (
                                <button
                                    type="button"
                                    className="prototype-primary"
                                    onClick={() => {
                                        dispatch({
                                            type:
                                                'COMPLETE_CURRENT_STEP',
                                        })
                                    }}
                                >
                                    {
                                        currentStep.actionLabel
                                    }
                                </button>
                            )}

                            {currentStepCompleted && (
                                <button
                                    type="button"
                                    className="prototype-primary"
                                    disabled={
                                        state.currentStepIndex
                                        === STEPS.length - 1
                                    }
                                    onClick={() => {
                                        dispatch({
                                            type:
                                                'NEXT_STEP',
                                        })
                                    }}
                                >
                                    Next step
                                </button>
                            )}
                        </div>
                    </section>
                </main>
            </div>
        </div>
    )
}

interface PrototypeStepContentProps {
    state: PrototypeState
    riskResult: {
        probability: string
        level: string
        decision: string
        selectedBid: string
    }
}

function PrototypeStepContent({
                                  state,
                                  riskResult,
                              }: PrototypeStepContentProps) {
    const step =
        STEPS[state.currentStepIndex].id

    if (
        step === 'TENDER_SETUP'
    ) {
        return (
            <div className="prototype-card-grid">
                <article className="prototype-card">
                    <span className="prototype-eyebrow">
                        Tender information
                    </span>

                    <h2>
                        Road Rehabilitation and
                        Maintenance
                    </h2>

                    <dl className="prototype-details">
                        <div>
                            <dt>
                                Reference
                            </dt>

                            <dd>
                                {
                                    state.tenderReference
                                }
                            </dd>
                        </div>

                        <div>
                            <dt>
                                Budget
                            </dt>

                            <dd>
                                R8,500,000
                            </dd>
                        </div>

                        <div>
                            <dt>
                                Business type
                            </dt>

                            <dd>
                                Road Rehabilitation and
                                Maintenance
                            </dd>
                        </div>

                        <div>
                            <dt>
                                Service area
                            </dt>

                            <dd>
                                Gauteng
                            </dd>
                        </div>
                    </dl>
                </article>

                <article className="prototype-card prototype-note">
                    <span className="prototype-eyebrow">
                        Prototype control
                    </span>

                    <h2>
                        Structured fields prevent vague
                        matching
                    </h2>

                    <p>
                        Traders are matched using an
                        explicit business category and
                        service area rather than loose
                        title keywords.
                    </p>
                </article>
            </div>
        )
    }

    if (
        step === 'PUBLICATION'
    ) {
        return (
            <div className="prototype-card-grid">
                <article className="prototype-card">
                    <span className="prototype-eyebrow">
                        Publication state
                    </span>

                    <h2>
                        {state.specificationHash
                            ? 'Specification locked'
                            : 'Ready to publish'}
                    </h2>

                    <dl className="prototype-details">
                        <div>
                            <dt>
                                Status
                            </dt>

                            <dd>
                                {state.specificationHash
                                    ? 'PUBLISHED'
                                    : 'DRAFT'}
                            </dd>
                        </div>

                        <div>
                            <dt>
                                SHA-256 hash
                            </dt>

                            <dd className="prototype-hash">
                                {state.specificationHash
                                    ?? 'Generated on publication'}
                            </dd>
                        </div>
                    </dl>
                </article>

                <article className="prototype-card prototype-note">
                    <span className="prototype-eyebrow">
                        Integrity protection
                    </span>

                    <h2>
                        Post-publication changes become
                        detectable
                    </h2>

                    <p>
                        The title, evaluation criteria,
                        budget and matching criteria are
                        protected by the specification
                        hash.
                    </p>
                </article>
            </div>
        )
    }

    if (
        step === 'MATCHING'
    ) {
        return (
            <div className="prototype-card-grid">
                <article className="prototype-card">
                    <span className="prototype-eyebrow">
                        Matching results
                    </span>

                    <h2>
                        3 active traders identified
                    </h2>

                    <div className="prototype-list">
                        <DemoStatusRow
                            label="TRADER-ALPHA"
                            value="WhatsApp queued"
                            status="success"
                        />

                        <DemoStatusRow
                            label="TRADER-BRAVO"
                            value="WhatsApp queued"
                            status="success"
                        />

                        <DemoStatusRow
                            label="TRADER-CHARLIE"
                            value="WhatsApp queued"
                            status="success"
                        />
                    </div>
                </article>

                <article className="prototype-phone">
                    <div className="prototype-phone-header">
                        KasiBridge WhatsApp
                    </div>

                    <div className="prototype-message">
                        <strong>
                            New matching tender
                        </strong>

                        <p>
                            A tender matching your business
                            profile has been published.
                        </p>

                        <span>
                            {state.tenderReference}
                        </span>

                        <small>
                            Approved template:
                            matching_tender_published
                        </small>
                    </div>
                </article>
            </div>
        )
    }

    if (
        step === 'BIDDING'
    ) {
        return (
            <article className="prototype-card">
                <span className="prototype-eyebrow">
                    Sealed bid register
                </span>

                <h2>
                    Three bids received before the
                    deadline
                </h2>

                <div className="prototype-table">
                    <div className="prototype-table-header">
                        <span>Alias</span>
                        <span>Amount</span>
                        <span>Submitted</span>
                        <span>Status</span>
                    </div>

                    <DemoTableRow
                        values={[
                            'BID-ALPHA',
                            'R8,100,000',
                            '09:42',
                            'SEALED',
                        ]}
                    />

                    <DemoTableRow
                        values={[
                            'BID-BRAVO',
                            'R7,650,000',
                            '10:07',
                            'SEALED',
                        ]}
                    />

                    <DemoTableRow
                        values={[
                            'BID-CHARLIE',
                            'R8,250,000',
                            '10:21',
                            'SEALED',
                        ]}
                    />
                </div>
            </article>
        )
    }

    if (
        step === 'COMPLIANCE'
    ) {
        return (
            <article className="prototype-card">
                <span className="prototype-eyebrow">
                    Automated compliance result
                </span>

                <h2>
                    Mandatory evidence gate
                </h2>

                <div className="prototype-list">
                    <DemoStatusRow
                        label="BID-ALPHA"
                        value="COMPLIANT"
                        status="success"
                    />

                    <DemoStatusRow
                        label="BID-BRAVO"
                        value="COMPLIANCE FAILED"
                        status="danger"
                    />

                    <DemoStatusRow
                        label="BID-CHARLIE"
                        value="COMPLIANT"
                        status="success"
                    />
                </div>

                <div className="prototype-warning">
                    BID-BRAVO is excluded from the
                    standard evaluation path because
                    mandatory qualification evidence is
                    incomplete.
                </div>
            </article>
        )
    }

    if (
        step === 'EVALUATION'
    ) {
        return (
            <article className="prototype-card">
                <span className="prototype-eyebrow">
                    Double-blind evaluation
                </span>

                <h2>
                    Supplier identities remain hidden
                </h2>

                <div className="prototype-score-grid">
                    <div>
                        <span>
                            BID-ALPHA
                        </span>

                        <strong>
                            86
                        </strong>

                        <small>
                            Validated total score
                        </small>
                    </div>

                    <div className="winner">
                        <span>
                            BID-CHARLIE
                        </span>

                        <strong>
                            91
                        </strong>

                        <small>
                            Highest compliant score
                        </small>
                    </div>
                </div>
            </article>
        )
    }

    if (
        step === 'INTEGRITY'
    ) {
        const highRisk =
            state.scenario
            === 'MANIPULATION_RISK'

        return (
            <div className="prototype-integrity-grid">
                <article className="prototype-card">
                    <span className="prototype-eyebrow">
                        Proposed award
                    </span>

                    <h2>
                        {riskResult.selectedBid}
                    </h2>

                    <div className="prototype-list">
                        <DemoStatusRow
                            label="Compliance"
                            value={
                                highRisk
                                    ? 'FAILED'
                                    : 'PASSED'
                            }
                            status={
                                highRisk
                                    ? 'danger'
                                    : 'success'
                            }
                        />

                        <DemoStatusRow
                            label="Highest score"
                            value={
                                highRisk
                                    ? 'BYPASSED'
                                    : 'SELECTED'
                            }
                            status={
                                highRisk
                                    ? 'danger'
                                    : 'success'
                            }
                        />

                        <DemoStatusRow
                            label="Conflict check"
                            value={
                                highRisk
                                    ? 'UNDECLARED LINK'
                                    : 'CLEAR'
                            }
                            status={
                                highRisk
                                    ? 'danger'
                                    : 'success'
                            }
                        />
                    </div>
                </article>

                <article
                    className={
                        highRisk
                            ? 'prototype-risk-result danger'
                            : 'prototype-risk-result safe'
                    }
                >
                    <span>
                        Prototype AI assessment
                    </span>

                    <strong>
                        {riskResult.probability}
                    </strong>

                    <small>
                        {riskResult.level}
                    </small>

                    <h2>
                        {riskResult.decision}
                    </h2>

                    <p>
                        Deterministic controls remain
                        authoritative. The model supports
                        prioritisation and independent
                        review.
                    </p>
                </article>
            </div>
        )
    }

    return (
        <article className="prototype-card">
            <span className="prototype-eyebrow">
                Immutable audit evidence
            </span>

            <h2>
                Demonstration audit chain
            </h2>

            <div className="prototype-audit-list">
                {state.auditEvents.map(
                    (event) => (
                        <div
                            className="prototype-audit-event"
                            key={event.id}
                        >
                            <div>
                                <span>
                                    {event.timestamp}
                                </span>

                                <strong>
                                    {event.action}
                                </strong>

                                <p>
                                    {event.evidence}
                                </p>
                            </div>

                            <div>
                                <span
                                    className={
                                        event.result
                                        === 'BLOCKED'
                                            ? 'prototype-status danger'
                                            : 'prototype-status success'
                                    }
                                >
                                    {event.result}
                                </span>

                                <small>
                                    {event.actor}
                                </small>

                                <code>
                                    {event.hash}
                                </code>
                            </div>
                        </div>
                    )
                )}
            </div>
        </article>
    )
}

interface DemoStatusRowProps {
    label: string
    value: string
    status: 'success' | 'danger'
}

function DemoStatusRow({
                           label,
                           value,
                           status,
                       }: DemoStatusRowProps) {
    return (
        <div className="prototype-status-row">
            <strong>
                {label}
            </strong>

            <span
                className={
                    `prototype-status ${status}`
                }
            >
                {value}
            </span>
        </div>
    )
}

interface DemoTableRowProps {
    values: string[]
}

function DemoTableRow({
                          values,
                      }: DemoTableRowProps) {
    return (
        <div className="prototype-table-row">
            {values.map(
                (value, index) => (
                    <span
                        key={
                            `${value}-${index}`
                        }
                    >
                        {value}
                    </span>
                )
            )}
        </div>
    )
}