package com.kasibridge.transaction.controller;

import com.kasibridge.transaction.dto.RecordTransactionRequest;
import com.kasibridge.transaction.dto.TransactionResponse;
import com.kasibridge.transaction.dto.TransactionSummary;
import com.kasibridge.transaction.dto.UpdateTransactionNotesRequest;
import com.kasibridge.transaction.entity.Transaction;
import com.kasibridge.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService service;


    //record a new transaction
    @PostMapping
    public ResponseEntity<TransactionResponse> recordTransaction(@Valid @RequestBody RecordTransactionRequest request){
            log.info("Creating transaction for traderId={}", request.getTraderId());
            TransactionResponse response = service.recordTransaction(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // /{id}
    //get a single transaction by id
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long id){
        log.info("Fetching transaction id={}", id);
        return ResponseEntity.ok(service.getTransactionById(id));
    }

    // /reference/{referenceNumber
    @GetMapping("/reference/{referenceNumber}")
    public ResponseEntity<TransactionResponse> getTransactionByReferenceNumber(@PathVariable String referenceNumber){
        log.info("Fetching transaction by referenceNumber={}", referenceNumber);
        return ResponseEntity.ok(service.getTransactionByReference(referenceNumber));
    }

    //get all transaction for a trader
    @GetMapping("/trader/{traderId}")
    public ResponseEntity<Page<TransactionResponse>> getTransactionByTrader(@PathVariable Long traderId,
                                                                            @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC)
                                                                            Pageable pageable){
        log.info("Fetching transactions by traderId={}", traderId);
        return ResponseEntity.ok(service.getTransactionsByTrader(traderId, pageable));
    }

    //get transactions for a trader filtered by type
    @GetMapping("/trader/{traderId}/type/{type}")
    public ResponseEntity<Page<TransactionResponse>> getTransactionByType(@PathVariable Long traderId, @PathVariable Transaction.TransactionType type,
                                                                          @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC)
                                                                          Pageable pageable){
        log.info("Fetching transactions by traderId={} and type={}", traderId, type);
        return ResponseEntity.ok(service.getTransactionsByType(traderId, type, pageable));
    }

    //get transaction for a trader filtered by status
    @GetMapping("/trader/{traderId}/status/{status}")
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByStatus(@PathVariable Long traderId,@PathVariable Transaction.TransactionStatus status,
                                                                             @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC)
                                                                             Pageable pageable){
        log.info("Fetching transactions by traderId{} and status={}", traderId, status);
        return ResponseEntity.ok(service.getTransactionsByStatus(traderId, status, pageable));
    }

    //get transactions for a trader filtered by channel
    @GetMapping("/trader/{traderId}/channel/{channel}")
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByChannel(@PathVariable Long traderId, @PathVariable Transaction.TransactionChannel channel,
                                                                              @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC)
                                                                              Pageable pageable){
        log.info("Fetching transactions by traderId{} and channel={}", traderId, channel);
        return ResponseEntity.ok(service.getTransactionsByChannel(traderId, channel,  pageable));
    }

    //get transaction for a trader within a date range
    @GetMapping("/trader/{traderId}/range")
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByDateRange(@PathVariable Long traderId,
                                                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                                                @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC)
                                                                                    Pageable pageable){

        if(from.isAfter(to)){
            throw new IllegalArgumentException("Invalid date range: from must be before to");
        }

        return ResponseEntity.ok(service.getTransactionsByDateRange(traderId, from, to, pageable));
    }

    //get trader summary (for credibility scoring)
    @GetMapping("/trader/{traderId}/summary")
    public ResponseEntity<TransactionSummary> getTraderSummary(@PathVariable Long traderId){
        log.info("Fetching transaction summary for traderId={}", traderId);
        return ResponseEntity.ok(service.getTraderSummary(traderId));
    }

    //update notes only
    @PatchMapping("/{id}/notes")
    public ResponseEntity<TransactionResponse> updatesNotes(@PathVariable Long id, @Valid @RequestBody UpdateTransactionNotesRequest request){
        log.info("Updating notes for traderId={}", id);
        return ResponseEntity.ok(service.updateNotes(id, request));
    }

    //explicitly complete a PENDING transaction
    @PatchMapping("/{id}/complete")
    public ResponseEntity<TransactionResponse> completeTransaction(@PathVariable Long id){
        log.info("Completing transaction for traderId={}", id);
        return ResponseEntity.ok(service.completeTransaction(id));
    }

    //explicitly cancel a transaction
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TransactionResponse> cancelTransaction(@PathVariable Long id){
        log.info("Canceling transaction for traderId={}", id);
        return ResponseEntity.ok(service.cancelTransaction(id));
    }

    //flag a transaction for AI fraud review
    @PatchMapping("/{id}/flag")
    public ResponseEntity<TransactionResponse> flagTransaction(@PathVariable Long id){
        log.info("Flagging transaction for traderId={}", id);
        return ResponseEntity.ok(service.flagTransaction(id));
    }
}
