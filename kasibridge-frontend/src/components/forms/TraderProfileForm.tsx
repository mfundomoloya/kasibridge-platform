import type {
    FormEvent,
} from 'react'

import type {
    TraderProfileFormValues,
} from '../../types/traderProfile'

interface TraderProfileFormProps {
    values: TraderProfileFormValues
    isCreating: boolean
    isSubmitting: boolean
    onChange: (
        values: TraderProfileFormValues
    ) => void
    onSubmit: (
        event: FormEvent<HTMLFormElement>
    ) => void
    onCancel?: () => void
}

export function TraderProfileForm({
                                      values,
                                      isCreating,
                                      isSubmitting,
                                      onChange,
                                      onSubmit,
                                      onCancel,
                                  }: TraderProfileFormProps) {
    function updateField(
        field: keyof TraderProfileFormValues,
        value: string | boolean
    ): void {
        onChange({
            ...values,
            [field]: value,
    })
    }

    return (
        <form
            className="profile-form"
            onSubmit={onSubmit}
        >
            <section className="form-section">
                <div className="form-section-heading">
                    <h2>Personal and contact details</h2>

                    <p>
                        These details identify the trader
                        linked to the authenticated account.
                    </p>
                </div>

                <div className="form-grid">
                    <div className="form-field">
                        <label htmlFor="fullName">
                            Full name
                        </label>

                        <input
                            id="fullName"
                            type="text"
                            value={values.fullName}
                            onChange={(event) =>
                                updateField(
                                    'fullName',
                                    event.target.value
                                )
                            }
                            disabled={
                                isSubmitting || !isCreating
                            }
                            required
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="phoneNumber">
                            Phone number
                        </label>

                        <input
                            id="phoneNumber"
                            type="tel"
                            placeholder="+27XXXXXXXXX"
                            value={values.phoneNumber}
                            onChange={(event) =>
                                updateField(
                                    'phoneNumber',
                                    event.target.value
                                )
                            }
                            disabled={
                                isSubmitting || !isCreating
                            }
                            pattern="^\+27[0-9]{9}$"
                            required
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="email">
                            Email address
                        </label>

                        <input
                            id="email"
                            type="email"
                            value={values.email}
                            onChange={(event) =>
                                updateField(
                                    'email',
                                    event.target.value
                                )
                            }
                            disabled={
                                isSubmitting || !isCreating
                            }
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="idNumber">
                            South African ID number
                        </label>

                        <input
                            id="idNumber"
                            type="password"
                            inputMode="numeric"
                            autoComplete="off"
                            value={values.idNumber}
                            onChange={(event) =>
                                updateField(
                                    'idNumber',
                                    event.target.value
                                )
                            }
                            disabled={
                                isSubmitting || !isCreating
                            }
                            pattern="^[0-9]{13}$"
                            maxLength={13}
                        />

                        <small>
                            Optional. Existing ID numbers remain
                            masked and cannot be edited here.
                        </small>
                    </div>
                </div>
            </section>

            <section className="form-section">
                <div className="form-section-heading">
                    <h2>Business details</h2>

                    <p>
                        Maintain the information used to
                        describe the business.
                    </p>
                </div>

                <div className="form-grid">
                    <div className="form-field">
                        <label htmlFor="businessName">
                            Business name
                        </label>

                        <input
                            id="businessName"
                            type="text"
                            value={values.businessName}
                            onChange={(event) =>
                                updateField(
                                    'businessName',
                                    event.target.value
                                )
                            }
                            disabled={isSubmitting}
                            required
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="businessType">
                            Business type
                        </label>

                        <input
                            id="businessType"
                            type="text"
                            placeholder="Example: Road maintenance"
                            value={values.businessType}
                            onChange={(event) =>
                                updateField(
                                    'businessType',
                                    event.target.value
                                )
                            }
                            disabled={isSubmitting}
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="tradingArea">
                            Trading area
                        </label>

                        <input
                            id="tradingArea"
                            type="text"
                            value={values.tradingArea}
                            onChange={(event) =>
                                updateField(
                                    'tradingArea',
                                    event.target.value
                                )
                            }
                            disabled={isSubmitting}
                            required
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="onboardingChannel">
                            Onboarding channel
                        </label>

                        <select
                            id="onboardingChannel"
                            value={values.onboardingChannel}
                            onChange={(event) =>
                                updateField(
                                    'onboardingChannel',
                                    event.target.value
                                )
                            }
                            disabled={
                                isSubmitting || !isCreating
                            }
                        >
                            <option value="WEB">
                                Web
                            </option>

                            <option value="WHATSAPP">
                                WhatsApp
                            </option>
                        </select>
                    </div>

                    <div className="form-field form-field-wide">
                        <label htmlFor="businessDescription">
                            Business description
                        </label>

                        <textarea
                            id="businessDescription"
                            rows={5}
                            maxLength={1000}
                            value={values.businessDescription}
                            onChange={(event) =>
                                updateField(
                                    'businessDescription',
                                    event.target.value
                                )
                            }
                            disabled={isSubmitting}
                        />

                        <small>
                            Maximum 1,000 characters.
                        </small>
                    </div>
                </div>
            </section>

            <section className="form-section">
                <div className="form-section-heading">
                    <h2>Registration and banking</h2>

                    <p>
                        Record business registration and
                        banking availability.
                    </p>
                </div>

                <div className="form-grid">
                    <label className="checkbox-field">
                        <input
                            type="checkbox"
                            checked={
                                values.hasBusinessRegistration
                            }
                            onChange={(event) =>
                                updateField(
                                    'hasBusinessRegistration',
                                    event.target.checked
                                )
                            }
                            disabled={isSubmitting}
                        />

                        <span>
              The business is registered
            </span>
                    </label>

                    <div className="form-field">
                        <label htmlFor="cipcNumber">
                            CIPC number
                        </label>

                        <input
                            id="cipcNumber"
                            type="text"
                            value={values.cipcNumber}
                            onChange={(event) =>
                                updateField(
                                    'cipcNumber',
                                    event.target.value
                                )
                            }
                            disabled={
                                isSubmitting
                                || !values
                                    .hasBusinessRegistration
                            }
                        />
                    </div>

                    <label className="checkbox-field">
                        <input
                            type="checkbox"
                            checked={values.hasBankAccount}
                            onChange={(event) =>
                                updateField(
                                    'hasBankAccount',
                                    event.target.checked
                                )
                            }
                            disabled={isSubmitting}
                        />

                        <span>
              The business has a bank account
            </span>
                    </label>

                    <div className="form-field">
                        <label htmlFor="bankName">
                            Bank name
                        </label>

                        <input
                            id="bankName"
                            type="text"
                            value={values.bankName}
                            onChange={(event) =>
                                updateField(
                                    'bankName',
                                    event.target.value
                                )
                            }
                            disabled={
                                isSubmitting
                                || !values.hasBankAccount
                            }
                        />
                    </div>
                </div>
            </section>

            <div className="form-actions">
                {onCancel && (
                    <button
                        className="secondary-button"
                        type="button"
                        onClick={onCancel}
                        disabled={isSubmitting}
                    >
                        Cancel
                    </button>
                )}

                <button
                    className="primary-button"
                    type="submit"
                    disabled={isSubmitting}
                >
                    {isSubmitting
                        ? 'Saving...'
                        : isCreating
                            ? 'Create business profile'
                            : 'Save changes'}
                </button>
            </div>
        </form>
    )
}