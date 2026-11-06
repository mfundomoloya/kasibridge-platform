import type {
    BidRanking,
} from '../../types/adjudication'

interface AdjudicationRankingCardProps {
    ranking: BidRanking
    selected: boolean
    disabled: boolean
    onSelect: (
        bidId: number,
    ) => void
}

export function AdjudicationRankingCard({
                                            ranking,
                                            selected,
                                            disabled,
                                            onSelect,
                                        }: AdjudicationRankingCardProps) {
    const selectionId =
        'adjudication-bid-'
        + ranking.bidId

    return (
        <article
            className={
                selected
                    ? (
                        'adjudication-ranking-card '
                        + 'is-selected'
                    )
                    : (
                        'adjudication-ranking-card'
                    )
            }
        >
            <header
                className={
                    'adjudication-ranking-header'
                }
            >
                <div
                    className={
                        'adjudication-rank'
                    }
                    aria-label={
                        'Rank '
                        + ranking.rank
                    }
                >
                    <span>
                        Rank
                    </span>

                    <strong>
                        {ranking.rank}
                    </strong>
                </div>

                <div
                    className={
                        'adjudication-bid-heading'
                    }
                >
                    <span
                        className={
                            'eyebrow'
                        }
                    >
                        Anonymised bid
                    </span>

                    <h2>
                        {
                            ranking
                                .bidderAlias
                        }
                    </h2>

                    <p>
                        Bid ID:
                        {' '}
                        <strong>
                            {
                                ranking
                                    .bidId
                            }
                        </strong>
                    </p>
                </div>

                <label
                    className={
                        selected
                            ? (
                                'adjudication-selection '
                                + 'is-selected'
                            )
                            : (
                                'adjudication-selection'
                            )
                    }
                    htmlFor={
                        selectionId
                    }
                >
                    <input
                        id={
                            selectionId
                        }
                        name={
                            'winningBidId'
                        }
                        type="radio"
                        value={
                            ranking.bidId
                        }
                        checked={
                            selected
                        }
                        disabled={
                            disabled
                        }
                        onChange={
                            () => {
                                onSelect(
                                    ranking
                                        .bidId
                                )
                            }
                        }
                    />

                    <span>
                        {selected
                            ? 'Selected'
                            : (
                                'Select for '
                                + 'award'
                            )}
                    </span>
                </label>
            </header>

            <dl
                className={
                    'adjudication-score-grid'
                }
            >
                <ScoreItem
                    label={
                        'Average technical '
                        + 'score'
                    }
                    value={
                        formatScore(
                            ranking
                                .averageTechnicalScore
                        )
                    }
                    maximum="100"
                    tone="technical"
                />

                <ScoreItem
                    label={
                        'Average price '
                        + 'score'
                    }
                    value={
                        formatScore(
                            ranking
                                .averagePriceScore
                        )
                    }
                    maximum="20"
                    tone="price"
                />

                <ScoreItem
                    label={
                        'Average total '
                        + 'score'
                    }
                    value={
                        formatScore(
                            ranking
                                .averageTotalScore
                        )
                    }
                    maximum="100"
                    tone="total"
                />

                <ScoreItem
                    label={
                        'Evaluator scores'
                    }
                    value={
                        ranking
                            .scoreCount
                            .toString()
                    }
                    maximum={null}
                    tone="count"
                />
            </dl>

            <div
                className={
                    'adjudication-ranking-footer'
                }
            >
                <p>
                    Scores are calculated from
                    submitted Evaluator records.
                    The bidder remains anonymous
                    during adjudication.
                </p>

                {
                    ranking.rank === 1
                    && (
                        <span
                            className={
                                'adjudication-leading-badge'
                            }
                        >
                            Highest evaluated total
                        </span>
                    )
                }
            </div>
        </article>
    )
}

interface ScoreItemProps {
    label: string
    value: string
    maximum: string | null
    tone:
        | 'technical'
        | 'price'
        | 'total'
        | 'count'
}

function ScoreItem({
                       label,
                       value,
                       maximum,
                       tone,
                   }: ScoreItemProps) {
    return (
        <div
            className={
                'adjudication-score-item '
                + 'is-'
                + tone
            }
        >
            <dt>
                {label}
            </dt>

            <dd>
                <strong>
                    {value}
                </strong>

                {maximum !== null && (
                    <span>
                        {' / '}
                        {maximum}
                    </span>
                )}
            </dd>
        </div>
    )
}

function formatScore(
    value: number,
): string {
    if (
        !Number.isFinite(
            value
        )
    ) {
        return 'Unavailable'
    }

    return value.toFixed(2)
}