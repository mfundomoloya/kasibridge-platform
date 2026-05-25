package com.kasibridge.transaction.service;


import com.kasibridge.transaction.dto.RecordTransactionRequest;
import com.kasibridge.transaction.dto.TransactionResponse;
import com.kasibridge.transaction.dto.TransactionSummary;
import com.kasibridge.transaction.dto.UpdateTransactionNotesRequest;
import com.kasibridge.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    //record a new transaction
    TransactionResponse recordTransaction(RecordTransactionRequest request);

    //get a single transaction by ID
    TransactionResponse getTransactionById(Long id);

    //get a transaction by reference number
    TransactionResponse getTransactionByReference(String referenceNumber);

    //get all transactions for a trader
    Page<TransactionResponse> getTransactionsByTrader(Long traderId, Pageable pageable);

    // get transactions for a trader filtered by type
    Page<TransactionResponse> getTransactionsByType(Long traderId, Transaction.TransactionType type, Pageable pageable);

    // get transactions for a trader filtered by status
    Page<TransactionResponse> getTransactionsByStatus(Long traderId, Transaction.TransactionStatus status, Pageable pageable);

    //get transactions for a trader filtered by channel
    Page<TransactionResponse> getTransactionsByChannel(Long traderId, Transaction.TransactionChannel channel, Pageable pageable);

    //get transaction for a trader filtered within a date range
    Page<TransactionResponse> getTransactionsByDateRange(Long traderId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    //update notes only (the only mutable field)
    TransactionResponse updateNotes(Long id, UpdateTransactionNotesRequest request);

    // these are the ONLY ways to change status
    TransactionResponse completeTransaction(Long id);
    TransactionResponse cancelTransaction(Long id);
    TransactionResponse flagTransaction(Long id);

    //get trader summary (for credibility scoring)
    TransactionSummary getTraderSummary(Long traderId);
}
