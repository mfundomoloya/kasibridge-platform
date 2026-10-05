interface TenderStatusBadgeProps {
    status: 'PUBLISHED'
}

export function TenderStatusBadge({
                                      status,
                                  }: TenderStatusBadgeProps) {
    const label =
        status === 'PUBLISHED'
            ? 'Open for bidding'
            : status

    return (
        <span className="tender-status-badge">
      {label}
    </span>
    )
}