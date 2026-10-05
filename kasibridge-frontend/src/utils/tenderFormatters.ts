export function formatCurrency(
    amount: number
): string {
    return new Intl.NumberFormat(
        'en-ZA',
        {
            style: 'currency',
            currency: 'ZAR',
            minimumFractionDigits: 2,
        }
    ).format(amount)
}

export function formatDateTime(
    value: string
): string {
    const parsedDate = new Date(value)

    if (Number.isNaN(parsedDate.getTime())) {
        return 'Date unavailable'
    }

    return new Intl.DateTimeFormat(
        'en-ZA',
        {
            dateStyle: 'medium',
            timeStyle: 'short',
        }
    ).format(parsedDate)
}

export function formatBuyerOrganisation(
    buyerOrgId: string
): string {
    return buyerOrgId
        .split('-')
        .map((part) =>
            part.charAt(0).toUpperCase()
            + part.slice(1)
        )
        .join(' ')
}