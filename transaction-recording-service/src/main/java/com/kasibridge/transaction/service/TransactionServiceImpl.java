package com.kasibridge.transaction.service;

import com.kasibridge.transaction.dto.RecordTransactionRequest;
import com.kasibridge.transaction.dto.TransactionResponse;
import com.kasibridge.transaction.dto.TransactionSummary;
import com.kasibridge.transaction.dto.UpdateTransactionNotesRequest;
import com.kasibridge.transaction.entity.Transaction;
import com.kasibridge.transaction.exception.InvalidTransactionStateException;
import com.kasibridge.transaction.exception.TransactionNotFoundException;
import com.kasibridge.transaction.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService{

    //repository
    private final TransactionRepository repository;

    public TransactionServiceImpl(TransactionRepository repository) {
        this.repository = repository;
    }


    @Override
    @Transactional
    public TransactionResponse recordTransaction(RecordTransactionRequest request) {
        log.info("Recording transaction for trader ID: {}", request.getTraderId());

        Transaction transaction = Transaction.create(
                request.getTraderId(),
                request.getAmount(),
                request.getType(),
                request.getPaymentMethod(),
                request.getChannel() != null ? request.getChannel() : Transaction.TransactionChannel.WEB,
                request.getDescription(),
                request.getCustomerName(),
                request.getCustomerPhone(),
                request.getOccurredAt(),
                request.getNotes()
        );

        Transaction saved = repository.save(transaction);
        log.info("Transaction recorded with reference: {}", saved.getReferenceNumber());
        return TransactionResponse.from(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        log.info("Fetching transaction ID: {}", id);
        return TransactionResponse.from(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String referenceNumber) {
        log.info("Fetching transaction by reference: {}", referenceNumber);
        Transaction transaction = repository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "No transaction found with reference: " + referenceNumber
                ));
        return TransactionResponse.from(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByTrader(Long traderId, Pageable pageable) {
        log.info("Fetching all transactions for trader ID: {}", traderId);
            return repository.findByTraderId(traderId, pageable)
                .map(TransactionResponse::from);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByType(
            Long traderId, Transaction.TransactionType type, Pageable pageable) {
        log.info("Fetching {} transactions for trader ID: {}", type, traderId);
        return repository.findByTraderIdAndType(traderId, type, pageable)
                .map(TransactionResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByStatus(
            Long traderId, Transaction.TransactionStatus status, Pageable pageable) {
        log.info("Fetching {} transactions for trader ID: {}", status, traderId);
        return repository.findByTraderIdAndStatus(traderId, status, pageable)
                .map(TransactionResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByChannel(
            Long traderId, Transaction.TransactionChannel channel, Pageable pageable) {
        log.info("Fetching {} transactions for trader ID: {}", channel, traderId);
        return repository.findByTraderIdAndChannel(traderId, channel, pageable)
                .map(TransactionResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByDateRange(
            Long traderId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        log.info("Fetching transactions for trader ID: {} between {} and {}", traderId, from, to);
        return repository.findByTraderIdAndOccurredAtBetween(traderId, from, to, pageable)
                .map(TransactionResponse::from);
    }

    // ── Update Notes (controlled mutation) ────────────────────────────────

    @Override
    @Transactional
    public TransactionResponse updateNotes(Long id, UpdateTransactionNotesRequest request) {
        log.info("Updating notes for transaction ID: {}", id);
        Transaction transaction = findByIdOrThrow(id);

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            transaction.updateNotes(request.getNotes());
        }

        return TransactionResponse.from(transaction);
    }

    // ── Explicit Status Actions ───────────────────────────────────────────

    @Override
    @Transactional
    public TransactionResponse completeTransaction(Long id) {
        log.info("Completing transaction ID: {}", id);
        Transaction transaction = findByIdOrThrow(id);

        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException(
                    "Only PENDING transactions can be completed. Current status: "
                            + transaction.getStatus()
            );
        }

        transaction.changeStatus(Transaction.TransactionStatus.COMPLETED);
        return TransactionResponse.from(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse cancelTransaction(Long id) {
        log.info("Cancelling transaction ID: {}", id);
        Transaction transaction = findByIdOrThrow(id);

        if (transaction.getStatus() == Transaction.TransactionStatus.COMPLETED ||
                transaction.getStatus() == Transaction.TransactionStatus.FLAGGED) {

            throw new InvalidTransactionStateException(
                    "Cannot cancel a " + transaction.getStatus() + " transaction."
            );
        }

        transaction.changeStatus(Transaction.TransactionStatus.CANCELLED);
        return TransactionResponse.from(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse flagTransaction(Long id) {
        log.info("Flagging transaction ID: {}", id);
        Transaction transaction = findByIdOrThrow(id);

        if (transaction.getStatus() == Transaction.TransactionStatus.CANCELLED) {
            throw new InvalidTransactionStateException(
                    "Cannot flag a CANCELLED transaction."
            );
        }

        transaction.changeStatus(Transaction.TransactionStatus.FLAGGED);
        return TransactionResponse.from(transaction);
    }

    // ── Summary ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TransactionSummary getTraderSummary(Long traderId) {
        log.info("Building transaction summary for trader ID: {}", traderId);

        BigDecimal totalSales = repository.getTotalSalesForTrader(traderId);
        BigDecimal totalExpenses = repository.getTotalExpensesForTrader(traderId);
        BigDecimal netCashFlow = totalSales.subtract(totalExpenses);

        long completed = repository.countByTraderIdAndStatus(
                traderId, Transaction.TransactionStatus.COMPLETED);
        long pending = repository.countByTraderIdAndStatus(
                traderId, Transaction.TransactionStatus.PENDING);
        long flagged = repository.countByTraderIdAndStatus(
                traderId, Transaction.TransactionStatus.FLAGGED);
        long cancelled = repository.countByTraderIdAndStatus(
                traderId, Transaction.TransactionStatus.CANCELLED);

        long webCount = repository.countByTraderIdAndChannel(
                traderId, Transaction.TransactionChannel.WEB);
        long whatsappCount = repository.countByTraderIdAndChannel(
                traderId, Transaction.TransactionChannel.WHATSAPP);

        long cashCount = repository.countByTraderIdAndPaymentMethod(
                traderId, Transaction.PaymentMethod.CASH);
        long eftCount = repository.countByTraderIdAndPaymentMethod(
                traderId, Transaction.PaymentMethod.EFT);
        long qrCount = repository.countByTraderIdAndPaymentMethod(
                traderId, Transaction.PaymentMethod.QR);
        long whatsappPayments = repository.countByTraderIdAndPaymentMethod(
                traderId, Transaction.PaymentMethod.WHATSAPP);

        // Compute average capture delay in Java using existing findByTraderId
        List<Transaction> allTransactions = repository.findByTraderId(traderId);
        double avgDelay = allTransactions.stream()
                .filter(t -> t.getOccurredAt() != null && t.getRecordedAt() != null)
                .mapToLong(t -> java.time.Duration.between(
                        t.getOccurredAt(),
                        t.getRecordedAt()
                ).toMinutes())
                .average()
                .orElse(0.0);

        // Backdated = recorded more than 60 minutes after it occurred
        long backdatedCount = repository.findBackdatedTransactions(traderId)
                .stream()
                .filter(t -> java.time.Duration.between(
                        t.getOccurredAt(),
                        t.getRecordedAt()
                ).toMinutes() > 60)
                .count();

        return TransactionSummary.builder()
                .traderId(traderId)
                .totalTransactions(completed + pending + flagged + cancelled)
                .completedTransactions(completed)
                .pendingTransactions(pending)
                .flaggedTransactions(flagged)
                .totalSalesAmount(totalSales)
                .totalExpensesAmount(totalExpenses)
                .netCashFlow(netCashFlow)
                .webTransactions(webCount)
                .whatsappTransactions(whatsappCount)
                .cashTransactions(cashCount)
                .eftTransactions(eftCount)
                .qrTransactions(qrCount)
                .whatsappPayments(whatsappPayments)
                .averageCaptureDelayMinutes((long) avgDelay)
                .backdatedTransactionCount(backdatedCount)
                .build();
    }
    // ── Private Helper ────────────────────────────────────────────────────

    private Transaction findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new TransactionNotFoundException(
                                "Transaction not found with ID: " + id
                        )
                );
    }

}
