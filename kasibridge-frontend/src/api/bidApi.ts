import axios from "axios";

import { procurementApiClient } from "./procurementApiClient";
import type {
    ApiErrorResponse,
    Bid,
} from "../types/bid";

export interface SubmitBidRequest {
    technicalProposal: string;
    priceAmount: number;
}

export interface BidApiError {
    status: number;
    message: string;
    fieldErrors: Record<string, string> | null;
}

// Submits a bid for the authenticated trader.
// POST /api/v1/tenders/{tenderId}/bids
export async function submitBid(
    tenderId: number,
    request: SubmitBidRequest,
): Promise<Bid> {
    const response = await procurementApiClient.post<Bid>(
        `/api/v1/tenders/${tenderId}/bids`,
        request,
    );

    return response.data;
}

// Withdraws a bid belonging to the authenticated trader.
// PATCH /api/v1/bids/{bidId}/withdraw
export async function withdrawBid(
    bidId: number,
): Promise<Bid> {
    const response = await procurementApiClient.patch<Bid>(
        `/api/v1/bids/${bidId}/withdraw`,
    );

    return response.data;
}

// Converts an API or network failure into a predictable
// error structure that frontend components can display.
export function getBidApiError(
    error: unknown,
): BidApiError {
    if (axios.isAxiosError<ApiErrorResponse>(error)) {
        if (error.response?.data) {
            return {
                status: error.response.data.status,
                message:
                    error.response.data.message
                    || "The bid request could not be completed.",
                fieldErrors:
                    error.response.data.fieldErrors ?? null,
            };
        }

        if (error.code === "ECONNABORTED") {
            return {
                status: 0,
                message:
                    "The bid request timed out. "
                    + "Please try again.",
                fieldErrors: null,
            };
        }

        if (!error.response) {
            return {
                status: 0,
                message:
                    "Unable to connect to the procurement service.",
                fieldErrors: null,
            };
        }

        return {
            status: error.response.status,
            message:
                error.message
                || "The bid request could not be completed.",
            fieldErrors: null,
        };
    }

    if (error instanceof Error) {
        return {
            status: 0,
            message: error.message,
            fieldErrors: null,
        };
    }

    return {
        status: 0,
        message: "An unexpected bid error occurred.",
        fieldErrors: null,
    };
}