import {
    useEffect,
    useMemo,
    useState,
} from 'react'

import axios from 'axios'

import {
    getOpenTenders,
} from '../api/tenderApi'

import {
    AppLayout,
} from '../components/layout/AppLayout'

import {
    TenderCard,
} from '../components/tenders/TenderCard'

import type {
    PageMetadata,
    TraderTender,
} from '../types/tender'

const initialPage: PageMetadata = {
    size: 10,
    number: 0,
    totalElements: 0,
    totalPages: 0,
}

function getTenderErrorMessage(
    error: unknown
): string {
    if (!axios.isAxiosError(error)) {
        return (
            'An unexpected tender error occurred.'
        )
    }

    if (error.response?.status === 403) {
        return (
            'The signed-in account is not authorised '
            + 'to browse trader tenders.'
        )
    }

    if (!error.response) {
        return (
            'The Procurement Service could not '
            + 'be reached.'
        )
    }

    return (
        'The open tenders could not be loaded.'
    )
}

export function OpenTendersPage() {
    const [tenders, setTenders] =
        useState<TraderTender[]>([])

    const [pageMetadata, setPageMetadata] =
        useState<PageMetadata>(initialPage)

    const [currentPage, setCurrentPage] =
        useState(0)

    const [searchTerm, setSearchTerm] =
        useState('')

    const [isLoading, setIsLoading] =
        useState(true)

    const [errorMessage, setErrorMessage] =
        useState('')

    useEffect(() => {
        let isActive = true

        getOpenTenders({
            page: currentPage,
            size: 10,
        })
            .then((response) => {
                if (!isActive) {
                    return
                }

                setTenders(response.content)
                setPageMetadata(response.page)
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (!isActive) {
                    return
                }

                setTenders([])
                setErrorMessage(
                    getTenderErrorMessage(error)
                )
            })
            .finally(() => {
                if (isActive) {
                    setIsLoading(false)
                }
            })

        return () => {
            isActive = false
        }
    }, [currentPage])

    const filteredTenders = useMemo(
        () => {
            const normalizedSearch =
                searchTerm.trim().toLowerCase()

            if (!normalizedSearch) {
                return tenders
            }

            return tenders.filter((tender) => {
                return [
                    tender.tenderReference,
                    tender.title,
                    tender.description,
                    tender.buyerOrgId,
                ].some((value) =>
                    value
                        .toLowerCase()
                        .includes(normalizedSearch)
                )
            })
        },
        [
            searchTerm,
            tenders,
        ]
    )

    function goToPreviousPage(): void {
        setIsLoading(true)

        setCurrentPage((page) =>
            Math.max(0, page - 1)
        )
    }

    function goToNextPage(): void {
        setIsLoading(true)

        setCurrentPage((page) =>
            Math.min(
                pageMetadata.totalPages - 1,
                page + 1
            )
        )
    }

    return (
        <AppLayout>
            <section className="page-heading">
                <div>
          <span className="eyebrow">
            Procurement opportunities
          </span>

                    <h1>Open tenders</h1>

                    <p>
                        Browse published procurement
                        opportunities that are currently
                        open for bidding.
                    </p>
                </div>
            </section>

            <section className="tender-toolbar">
                <div className="tender-search">
                    <label htmlFor="tenderSearch">
                        Search open tenders
                    </label>

                    <input
                        id="tenderSearch"
                        type="search"
                        placeholder={
                            'Search by title, reference, '
                            + 'buyer or description'
                        }
                        value={searchTerm}
                        onChange={(event) =>
                            setSearchTerm(
                                event.target.value
                            )
                        }
                    />
                </div>

                <div className="tender-result-count">
                    <strong>
                        {pageMetadata.totalElements}
                    </strong>

                    <span>
            open tender
                        {pageMetadata.totalElements === 1
                            ? ''
                            : 's'}
          </span>
                </div>
            </section>

            {errorMessage && (
                <div
                    className="error-message page-message"
                    role="alert"
                >
                    {errorMessage}
                </div>
            )}

            {isLoading ? (
                <section className="content-card">
                    <p>Loading open tenders...</p>
                </section>
            ) : filteredTenders.length === 0 ? (
                <section className="content-card empty-state">
                    <h2>
                        No open tenders found
                    </h2>

                    <p>
                        {searchTerm
                            ? 'No open tender matches '
                            + 'the current search.'
                            : 'No procurement opportunities '
                            + 'are currently published.'}
                    </p>
                </section>
            ) : (
                <section className="tender-list">
                    {filteredTenders.map((tender) => (
                        <TenderCard
                            key={tender.id}
                            tender={tender}
                        />
                    ))}
                </section>
            )}

            {pageMetadata.totalPages > 1 && (
                <nav
                    className="pagination"
                    aria-label="Tender pages"
                >
                    <button
                        className="secondary-button"
                        type="button"
                        onClick={goToPreviousPage}
                        disabled={
                            currentPage === 0
                            || isLoading
                        }
                    >
                        Previous
                    </button>

                    <span>
            Page {pageMetadata.number + 1}
                        {' '}of{' '}
                        {pageMetadata.totalPages}
          </span>

                    <button
                        className="secondary-button"
                        type="button"
                        onClick={goToNextPage}
                        disabled={
                            currentPage
                            >= pageMetadata.totalPages - 1
                            || isLoading
                        }
                    >
                        Next
                    </button>
                </nav>
            )}
        </AppLayout>
    )
}