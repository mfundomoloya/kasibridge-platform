import {
    type ChangeEvent,
    type FormEvent,
    useCallback,
    useState,
} from 'react'

import {
    assignCommitteeMember,
    getCommitteeAssignmentApiErrorMessage,
    getTenderCommitteeAssignments,
} from '../api/committeeAssignmentApi'

import {
    AdministrationLayout,
} from '../components/layout/AdministrationLayout'

import type {
    AdministrativeCommitteeAssignment,
    CommitteeAssignmentFormErrors,
    CommitteeAssignmentFormState,
    CommitteeRole,
} from '../types/committeeAssignment'

import {
    COMMITTEE_ROLE_LABELS,
    EMPTY_COMMITTEE_ASSIGNMENT_FORM,
} from '../types/committeeAssignment'

import './AdminCommitteeAssignmentPage.css'

const dateTimeFormatter =
    new Intl.DateTimeFormat(
        'en-ZA',
        {
            dateStyle: 'medium',
            timeStyle: 'short',
        }
    )

const committeeRoles:
    CommitteeRole[] = [
    'SPECIFICATION_OFFICER',
    'EVALUATOR',
    'ADJUDICATOR',
]

export function AdminCommitteeAssignmentPage() {
    const [
        form,
        setForm,
    ] =
        useState<
            CommitteeAssignmentFormState
        >({
            ...EMPTY_COMMITTEE_ASSIGNMENT_FORM,
        })

    const [
        errors,
        setErrors,
    ] =
        useState<
            CommitteeAssignmentFormErrors
        >({})

    const [
        assignments,
        setAssignments,
    ] =
        useState<
            AdministrativeCommitteeAssignment[]
        >([])

    const [
        loadedTenderId,
        setLoadedTenderId,
    ] =
        useState<number | null>(
            null
        )

    const [
        isLoadingAssignments,
        setIsLoadingAssignments,
    ] = useState(false)

    const [
        isSubmitting,
        setIsSubmitting,
    ] = useState(false)

    const [
        successMessage,
        setSuccessMessage,
    ] =
        useState<string | null>(
            null
        )

    const loadAssignments =
        useCallback(
            async (
                tenderId: number,
            ): Promise<void> => {
                setIsLoadingAssignments(
                    true
                )

                setErrors(
                    (currentErrors) => ({
                        ...currentErrors,
                        form: undefined,
                    })
                )

                try {
                    const response =
                        await getTenderCommitteeAssignments(
                            tenderId
                        )

                    setAssignments(
                        response
                    )

                    setLoadedTenderId(
                        tenderId
                    )
                } catch (error) {
                    setAssignments([])

                    setLoadedTenderId(
                        tenderId
                    )

                    setErrors(
                        (currentErrors) => ({
                            ...currentErrors,

                            form:
                                getCommitteeAssignmentApiErrorMessage(
                                    error,
                                    'Unable to load '
                                    + 'committee '
                                    + 'assignments.'
                                ),
                        })
                    )
                } finally {
                    setIsLoadingAssignments(
                        false
                    )
                }
            },
            []
        )

    function handleTextChange(
        event:
        ChangeEvent<
            HTMLInputElement
            | HTMLTextAreaElement
        >,
    ): void {
        const {
            name,
            value,
        } = event.target

        const field =
            name as keyof
                CommitteeAssignmentFormState

        setForm(
            (currentForm) => ({
                ...currentForm,
                [field]: value,
            })
        )

        clearFieldError(field)

        if (
            field === 'tenderId'
            && loadedTenderId !== null
            && Number(value)
            !== loadedTenderId
        ) {
            setAssignments([])
            setLoadedTenderId(null)
        }
    }

    function handleRoleChange(
        event:
        ChangeEvent<HTMLSelectElement>,
    ): void {
        const committeeRole =
            event.target
                .value as CommitteeRole

        setForm(
            (currentForm) => ({
                ...currentForm,
                committeeRole,
            })
        )

        setErrors(
            (currentErrors) => ({
                ...currentErrors,
                form: undefined,
            })
        )

        setSuccessMessage(null)
    }

    function clearFieldError(
        field:
        keyof CommitteeAssignmentFormState,
    ): void {
        setErrors(
            (currentErrors) => {
                const nextErrors = {
                    ...currentErrors,
                }

                if (
                    field === 'tenderId'
                ) {
                    delete nextErrors
                        .tenderId
                }

                if (field === 'userId') {
                    delete nextErrors
                        .userId
                }

                if (
                    field
                    === 'assignedByUserId'
                ) {
                    delete nextErrors
                        .assignedByUserId
                }

                if (field === 'reason') {
                    delete nextErrors
                        .reason
                }

                delete nextErrors.form

                return nextErrors
            }
        )

        setSuccessMessage(null)
    }

    function validateForm():
        CommitteeAssignmentFormErrors {
        const validationErrors:
            CommitteeAssignmentFormErrors = {}

        const tenderId =
            Number(
                form.tenderId
            )

        const userId =
            Number(
                form.userId
            )

        const assignedByUserId =
            Number(
                form.assignedByUserId
            )

        const reason =
            form.reason.trim()

        if (
            !Number.isInteger(tenderId)
            || tenderId <= 0
        ) {
            validationErrors.tenderId =
                'Enter a valid positive '
                + 'tender ID.'
        }

        if (
            !Number.isInteger(userId)
            || userId <= 0
        ) {
            validationErrors.userId =
                'Enter a valid positive '
                + 'committee member user ID.'
        }

        if (
            !Number.isInteger(
                assignedByUserId
            )
            || assignedByUserId <= 0
        ) {
            validationErrors
                .assignedByUserId =
                'Enter a valid positive '
                + 'assigning user ID.'
        }

        if (reason.length < 10) {
            validationErrors.reason =
                'The assignment reason must '
                + 'contain at least '
                + '10 characters.'
        } else if (
            reason.length > 1000
        ) {
            validationErrors.reason =
                'The assignment reason cannot '
                + 'exceed 1000 characters.'
        }

        return validationErrors
    }

    async function handleSubmit(
        event:
        FormEvent<HTMLFormElement>,
    ): Promise<void> {
        event.preventDefault()

        const validationErrors =
            validateForm()

        if (
            Object.keys(
                validationErrors
            ).length > 0
        ) {
            setErrors(
                validationErrors
            )

            return
        }

        const tenderId =
            Number(
                form.tenderId
            )

        /*
         * Require the administrator to load and
         * review the tender's current committee
         * assignments before creating a new one.
         */
        if (
            loadedTenderId === null
            || loadedTenderId !== tenderId
        ) {
            setErrors({
                form:
                    'Load the selected tender and '
                    + 'review its current committee '
                    + 'assignments before creating '
                    + 'a new assignment.',
            })

            return
        }

        setIsSubmitting(true)
        setErrors({})
        setSuccessMessage(null)

        try {
            const assignment =
                await assignCommitteeMember(
                    tenderId,
                    {
                        userId:
                            Number(
                                form.userId
                            ),

                        committeeRole:
                        form.committeeRole,

                        assignedByUserId:
                            Number(
                                form
                                    .assignedByUserId
                            ),

                        reason:
                            form.reason.trim(),
                    }
                )

            setSuccessMessage(
                'User '
                + assignment.userId
                + ' was assigned as '
                + formatRole(
                    assignment
                        .committeeRole
                )
                + ' for tender '
                + assignment.tenderId
                + '.'
            )

            setForm(
                (currentForm) => ({
                    ...currentForm,
                    userId: '',
                    reason: '',
                })
            )

            await loadAssignments(
                tenderId
            )
        } catch (error) {
            setErrors({
                form:
                    getCommitteeAssignmentApiErrorMessage(
                        error,
                        'Unable to create the '
                        + 'committee assignment.'
                    ),
            })
        } finally {
            setIsSubmitting(false)
        }
    }

    async function handleLoadTender():
        Promise<void> {
        const tenderId =
            Number(
                form.tenderId
            )

        if (
            !Number.isInteger(tenderId)
            || tenderId <= 0
        ) {
            setErrors(
                (currentErrors) => ({
                    ...currentErrors,

                    tenderId:
                        'Enter a valid positive '
                        + 'tender ID before loading '
                        + 'assignments.',
                })
            )

            return
        }

        setSuccessMessage(null)

        await loadAssignments(
            tenderId
        )
    }

    return (
        <AdministrationLayout>
            <section className="page-heading">
                <div>
                    <span className="eyebrow">
                        Committee management
                    </span>

                    <h1>
                        Committee assignments
                    </h1>

                    <p>
                        Assign authorised users to
                        tender committees while
                        preserving segregation of
                        duties. A user may not hold
                        multiple active committee
                        roles on the same tender.
                    </p>
                </div>

            </section>

            {successMessage !== null && (
                <p
                    className={
                        'success-message '
                        + 'page-message'
                    }
                    role="status"
                    aria-live="polite"
                >
                    {successMessage}
                </p>
            )}

            {errors.form !== undefined && (
                <p
                    className={
                        'form-error-message '
                        + 'page-message'
                    }
                    role="alert"
                >
                    {errors.form}
                </p>
            )}

            <div
                className={
                    'admin-committee-grid'
                }
            >
                <section
                    className={
                        'content-card '
                        + 'admin-committee-form-card'
                    }
                    aria-labelledby={
                        'committee-assignment-form-heading'
                    }
                >
                    <span className="eyebrow">
                        New assignment
                    </span>

                    <h2
                        id={
                            'committee-assignment-form-heading'
                        }
                    >
                        Assign a committee member
                    </h2>

                    <p>
                        Enter the tender, committee
                        member and assignment reason.
                        The backend prevents multiple
                        active roles for the same user
                        on one tender.
                    </p>

                    <form
                        className={
                            'admin-committee-form'
                        }
                        onSubmit={
                            handleSubmit
                        }
                        noValidate
                    >

                        <div className="form-field">
                            <label htmlFor="tenderId">
                                Tender ID
                            </label>

                            <div
                                className={
                                    'admin-tender-lookup'
                                }
                            >
                                <input
                                    id="tenderId"
                                    name="tenderId"
                                    type="number"
                                    min="1"
                                    step="1"
                                    inputMode="numeric"
                                    value={form.tenderId}
                                    onChange={
                                        handleTextChange
                                    }
                                    disabled={
                                        isLoadingAssignments
                                        || isSubmitting
                                    }
                                    aria-invalid={
                                        errors.tenderId
                                        !== undefined
                                    }
                                    aria-describedby={
                                        errors.tenderId
                                            ? 'tenderId-error'
                                            : 'tenderId-help'
                                    }
                                    placeholder={
                                        'Enter tender ID'
                                    }
                                />

                                <button
                                    className={
                                        'admin-tender-load-button '
                                        + (
                                            form.tenderId
                                                .trim()
                                                .length > 0
                                                ? 'has-value'
                                                : ''
                                        )
                                    }
                                    type="button"
                                    onClick={
                                        () => {
                                            void handleLoadTender()
                                        }
                                    }
                                    disabled={
                                        isLoadingAssignments
                                        || isSubmitting
                                        || form
                                            .tenderId
                                            .trim()
                                            .length === 0
                                    }
                                >
                                    {isLoadingAssignments
                                        ? 'Loading...'
                                        : 'Load assignments'}
                                </button>
                            </div>

                            <small id="tenderId-help">
                                Enter the ID of the tender
                                whose committee membership
                                you want to manage.
                            </small>

                            {errors.tenderId && (
                                <p
                                    id="tenderId-error"
                                    className="field-error"
                                    role="alert"
                                >
                                    {errors.tenderId}
                                </p>
                            )}
                        </div>

                        <div className="form-field">
                            <label
                                htmlFor="userId"
                            >
                                Committee member
                                user ID
                            </label>

                            <input
                                id="userId"
                                name="userId"
                                type="number"
                                min="1"
                                step="1"
                                inputMode="numeric"
                                value={
                                    form.userId
                                }
                                onChange={
                                    handleTextChange
                                }
                                disabled={
                                    isSubmitting
                                }
                                aria-invalid={
                                    errors.userId
                                    !== undefined
                                }
                                aria-describedby={
                                    errors.userId
                                        ? (
                                            'userId-error'
                                        )
                                        : (
                                            'userId-help'
                                        )
                                }
                                placeholder="Enter user ID"
                            />

                            <small
                                id="userId-help"
                            >
                                Enter the user ID of the
                                person being assigned to
                                the selected committee role.
                            </small>

                            {errors.userId && (
                                <p
                                    id={
                                        'userId-error'
                                    }
                                    className={
                                        'field-error'
                                    }
                                    role="alert"
                                >
                                    {errors.userId}
                                </p>
                            )}
                        </div>

                        <div className="form-field">
                            <label
                                htmlFor={
                                    'committeeRole'
                                }
                            >
                                Committee role
                            </label>

                            <select
                                id="committeeRole"
                                name="committeeRole"
                                value={
                                    form
                                        .committeeRole
                                }
                                onChange={
                                    handleRoleChange
                                }
                                disabled={
                                    isSubmitting
                                }
                            >
                                {
                                    committeeRoles
                                        .map(
                                            (
                                                role,
                                            ) => (
                                                <option
                                                    key={
                                                        role
                                                    }
                                                    value={
                                                        role
                                                    }
                                                >
                                                    {
                                                        COMMITTEE_ROLE_LABELS[
                                                            role
                                                            ]
                                                    }
                                                </option>
                                            )
                                        )
                                }
                            </select>
                        </div>

                        <div className="form-field">
                            <label
                                htmlFor="assignedByUserId"
                            >
                                Assigning administrator
                                user ID
                            </label>

                            <input
                                id="assignedByUserId"
                                name="assignedByUserId"
                                type="number"
                                min="1"
                                step="1"
                                inputMode="numeric"
                                value={
                                    form.assignedByUserId
                                }
                                onChange={
                                    handleTextChange
                                }
                                disabled={
                                    isSubmitting
                                }
                                aria-invalid={
                                    errors.assignedByUserId
                                    !== undefined
                                }
                                aria-describedby={
                                    errors.assignedByUserId
                                        ? 'assignedByUserId-error'
                                        : 'assignedByUserId-help'
                                }
                                placeholder={
                                    'Enter administrator user ID'
                                }
                            />

                            <small
                                id="assignedByUserId-help"
                            >
                                Enter the authenticated
                                administrator’s user ID.
                                The backend should later
                                derive this value directly
                                from the signed-in account.
                            </small>

                            {
                                errors.assignedByUserId
                                && (
                                    <p
                                        id={
                                            'assignedByUserId-error'
                                        }
                                        className="field-error"
                                        role="alert"
                                    >
                                        {
                                            errors
                                                .assignedByUserId
                                        }
                                    </p>
                                )
                            }
                        </div>

                        <div className="form-field">
                            <label
                                htmlFor="reason"
                            >
                                Assignment reason
                            </label>

                            <textarea
                                id="reason"
                                name="reason"
                                rows={5}
                                minLength={10}
                                maxLength={1000}
                                value={
                                    form.reason
                                }
                                onChange={
                                    handleTextChange
                                }
                                disabled={
                                    isSubmitting
                                }
                                aria-invalid={
                                    errors.reason
                                    !== undefined
                                }
                                aria-describedby={
                                    errors.reason
                                        ? (
                                            'reason-error'
                                        )
                                        : (
                                            'reason-help'
                                        )
                                }
                                placeholder={
                                    'Assigned to '
                                    + 'perform blind '
                                    + 'technical '
                                    + 'evaluation for '
                                    + 'the ICT equipment '
                                    + 'tender.'
                                }
                            />

                            <div
                                className={
                                    'admin-committee-'
                                    + 'field-help'
                                }
                            >
                                <small
                                    id="reason-help"
                                >
                                    Explain why the
                                    member is assigned.
                                </small>

                                <small>
                                    {
                                        form.reason
                                            .length
                                    }
                                    {' / 1000'}
                                </small>
                            </div>

                            {errors.reason && (
                                <p
                                    id={
                                        'reason-error'
                                    }
                                    className={
                                        'field-error'
                                    }
                                    role="alert"
                                >
                                    {errors.reason}
                                </p>
                            )}
                        </div>

                        <div className="form-actions">
                            <button
                                className={
                                    'primary-button'
                                }
                                type="submit"
                                disabled={
                                    isSubmitting
                                }
                            >
                                {isSubmitting
                                    ? 'Assigning...'
                                    : (
                                        'Assign committee '
                                        + 'member'
                                    )}
                            </button>

                            <button
                                className={
                                    'secondary-button '
                                    + 'admin-clear-button'
                                }
                                type="button"
                                onClick={
                                    () => {
                                        setForm({
                                            ...EMPTY_COMMITTEE_ASSIGNMENT_FORM,
                                        })

                                        setErrors({})
                                        setSuccessMessage(null)
                                        setAssignments([])
                                        setLoadedTenderId(null)
                                    }
                                }
                                disabled={
                                    isSubmitting
                                }
                            >
                                Clear
                            </button>
                        </div>
                    </form>
                </section>

                <section
                    className={
                        'content-card '
                        + 'admin-committee-list-card'
                    }
                    aria-labelledby={
                        'committee-assignment-list-heading'
                    }
                >
                    <div
                        className={
                            'admin-committee-list-header'
                        }
                    >
                        <div>
                            <span className="eyebrow">
                                Current membership
                            </span>

                            <h2
                                id={
                                    'committee-assignment-list-heading'
                                }
                            >
                                Tender assignments
                            </h2>

                            <p>
                                Active and historical
                                assignments returned
                                for the selected
                                tender.
                            </p>
                        </div>

                        {loadedTenderId !== null && (
                            <span
                                className={
                                    'admin-committee-'
                                    + 'tender-badge'
                                }
                            >
                                Tender
                                {' '}
                                {loadedTenderId}
                            </span>
                        )}
                    </div>

                    {isLoadingAssignments ? (
                        <p
                            className={
                                'admin-committee-'
                                + 'loading'
                            }
                            aria-busy="true"
                        >
                            Loading committee
                            assignments...
                        </p>
                    ) : loadedTenderId === null ? (
                        <div className="empty-state">
                            <h3>
                                Select a tender
                            </h3>

                            <p>
                                Enter a tender ID and
                                select
                                {' '}
                                <strong>
                                    Load tender
                                    assignments
                                </strong>
                                {' '}
                                to view its current
                                committee membership.
                            </p>
                        </div>
                    ) : assignments.length === 0 ? (
                        <div className="empty-state">
                            <h3>
                                No assignments found
                            </h3>

                            <p>
                                Tender
                                {' '}
                                {loadedTenderId}
                                {' '}
                                does not currently
                                have a committee
                                assignment.
                            </p>
                        </div>
                    ) : (
                        <div
                            className={
                                'admin-committee-'
                                + 'assignment-list'
                            }
                        >
                            {assignments.map(
                                (currentAssignment) => (
                                    <CommitteeAssignmentItem
                                        key={
                                            currentAssignment
                                                .id
                                        }
                                        assignment={
                                            currentAssignment
                                        }
                                    />
                                )
                            )}
                        </div>
                    )}
                </section>
            </div>

            <section
                className={
                    'content-card '
                    + 'admin-committee-guidance'
                }
                aria-labelledby={
                    'committee-guidance-heading'
                }
            >
                <div>
                    <span className="eyebrow">
                        Segregation of duties
                    </span>

                    <h2
                        id={
                            'committee-guidance-heading'
                        }
                    >
                        Committee controls
                    </h2>
                </div>

                <ul>
                    <li>
                        A user may hold only one
                        active committee role on the
                        same tender.
                    </li>

                    <li>
                        Evaluators review anonymised
                        bids and do not record the
                        final award.
                    </li>

                    <li>
                        Adjudicators review evaluation
                        recommendations and do not
                        alter evaluator scores.
                    </li>

                    <li>
                        Platform administration does
                        not automatically grant
                        evaluation or adjudication
                        authority.
                    </li>
                </ul>
            </section>
        </AdministrationLayout>
    )
}

interface CommitteeAssignmentItemProps {
    assignment:
        AdministrativeCommitteeAssignment
}

function CommitteeAssignmentItem({
                                     assignment,
                                 }: CommitteeAssignmentItemProps) {
    return (
        <article
            className={
                assignment.active
                    ? (
                        'admin-committee-assignment '
                        + 'is-active'
                    )
                    : (
                        'admin-committee-assignment '
                        + 'is-inactive'
                    )
            }
        >
            <header>
                <div>
                    <span>
                        Assignment
                        {' '}
                        {assignment.id}
                    </span>

                    <h3>
                        User
                        {' '}
                        {assignment.userId}
                    </h3>
                </div>

                <span
                    className={
                        'admin-committee-role-badge'
                    }
                >
                    {
                        COMMITTEE_ROLE_LABELS[
                            assignment
                                .committeeRole
                            ]
                    }
                </span>
            </header>

            <dl>
                <div>
                    <dt>Status</dt>

                    <dd>
                        {assignment.active
                            ? 'Active'
                            : 'Inactive'}
                    </dd>
                </div>

                <div>
                    <dt>Assigned by</dt>

                    <dd>
                        User
                        {' '}
                        {
                            assignment
                                .assignedByUserId
                        }
                    </dd>
                </div>

                <div>
                    <dt>Assigned</dt>

                    <dd>
                        {
                            formatDateTime(
                                assignment
                                    .assignedAt
                            )
                        }
                    </dd>
                </div>
            </dl>

            <div
                className={
                    'admin-committee-reason'
                }
            >
                <span>
                    Assignment reason
                </span>

                <p>
                    {
                        assignment.reason
                        ?? (
                            'No assignment reason '
                            + 'was recorded.'
                        )
                    }
                </p>
            </div>
        </article>
    )
}

function formatRole(
    role: CommitteeRole,
): string {
    return COMMITTEE_ROLE_LABELS[
        role
        ]
}

function formatDateTime(
    value: string,
): string {
    const parsedDate =
        new Date(value)

    if (
        Number.isNaN(
            parsedDate.getTime()
        )
    ) {
        return 'Unavailable'
    }

    return dateTimeFormatter.format(
        parsedDate
    )
}