CREATE UNIQUE INDEX IF NOT EXISTS uq_active_bid_per_trader_tender
    ON bids (
             tender_id,
             trader_profile_id
        )
    WHERE status <> 'WITHDRAWN';