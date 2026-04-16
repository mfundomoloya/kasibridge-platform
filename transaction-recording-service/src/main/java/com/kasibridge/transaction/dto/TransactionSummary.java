package com.kasibridge.transaction.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionSummary {

    private Long traderId;

    //volume
    private Long totalTransactions;
    private Long completedTransactions;
    private long pendingTransactions;
    private long flaggedTransactions;

    // financials totals
    //sum of all sales transactions
    private BigDecimal totalSalesAmount;

    //sum of all expense transactions
    private BigDecimal totalExpensesAmount;

    // derived sales - expense  net cash flow
    private BigDecimal netCashFlow;

    // channel breakdown
    private long webTransactions;
    private long whatsappTransactions;

    // Payment method breakdown
    private long cashTransactions;
    private long eftTransactions;
    private long qrTransactions;
    private long whatsappPayments;

    // Credibility signals (used by AI module later)
    private long averageCaptureDelayMinutes;  // Low delay = more trustworthy capture behavior
    private long backdatedTransactionCount;   // High count = suspicious, count of transactions where captureDelay exceeds threshold (e.g. > 24h)


    // (Optional but powerful)
    // Number of distinct days with recorded activity
    // private Long activeDays;

}
