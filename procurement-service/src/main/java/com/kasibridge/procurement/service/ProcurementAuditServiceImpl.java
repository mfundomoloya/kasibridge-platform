package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AuditChainVerificationResponse;
import com.kasibridge.procurement.dto.ProcurementAuditResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.repository.ProcurementAuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementAuditServiceImpl implements ProcurementAuditService {

    private final ProcurementAuditEventRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(ProcurementAuditEvent.AuditEventType eventType, Long tenderId, Long bidId, Long actorUserId, String message, String details) {
        record(
                eventType,
                tenderId,
                bidId,
                actorUserId,
                ProcurementAuditEvent.AuditResult.SUCCESS,
                message,
                details
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(ProcurementAuditEvent.AuditEventType eventType, Long tenderId, Long bidId, Long actorUserId, String message, String details) {
        record(
                eventType,
                tenderId,
                bidId,
                actorUserId,
                ProcurementAuditEvent.AuditResult.FAILED,
                message,
                details
        );
    }

    private void record(
            ProcurementAuditEvent.AuditEventType eventType,
            Long tenderId,
            Long bidId,
            Long actorUserId,
            ProcurementAuditEvent.AuditResult result,
            String message,
            String details
    ) {
        try {
            String previousHash = repository.findTopByOrderByIdDesc()
                    .map(ProcurementAuditEvent::getEventHash)
                    .orElse(null);

            LocalDateTime createdAt = LocalDateTime.now()
                    .truncatedTo(ChronoUnit.MICROS);

            ProcurementAuditEvent event = ProcurementAuditEvent.builder()
                    .eventType(eventType)
                    .tenderId(tenderId)
                    .bidId(bidId)
                    .actorUserId(actorUserId)
                    .result(result)
                    .message(trimToLength(message, 1000))
                    .details(trimToLength(details, 3000))
                    .createdAt(createdAt)
                    .previousEventHash(previousHash)
                    .eventHash("PENDING")
                    .build();

            String eventHash = generateEventHash(event);
            event.setEventHash(eventHash);

            repository.saveAndFlush(event);

            log.info(
                    "Procurement audit event recorded: type={} tenderId={} bidId={} actorUserId={} result={} hash={}",
                    eventType,
                    tenderId,
                    bidId,
                    actorUserId,
                    result,
                    eventHash
            );
        } catch (Exception ex) {
            log.error(
                    "Failed to record procurement audit event: type={} tenderId={} bidId={} actorUserId={} result={}",
                    eventType,
                    tenderId,
                    bidId,
                    actorUserId,
                    result,
                    ex
            );
        }
    }

    private String trimToLength(String value, int maxLength){
        if(value == null){
            return null;
        }

        if(value.length() <= maxLength){
            return value;
        }

        return value.substring(0, maxLength);
    }

    @Override
    public Page<ProcurementAuditResponse> getAuditEvents(Pageable pageable) {
        return repository.findAll(pageable)
                .map(ProcurementAuditResponse::from);
    }

    @Override
    public Page<ProcurementAuditResponse> getAuditEventsByTender(Long tenderId, Pageable pageable) {
        return repository.findByTenderId(tenderId, pageable)
                .map(ProcurementAuditResponse::from);
    }

    @Override
    public Page<ProcurementAuditResponse> getAuditEventsByBid(Long bidId, Pageable pageable) {
        return repository.findByBidId(bidId, pageable)
                .map(ProcurementAuditResponse::from);
    }

    @Override
    public Page<ProcurementAuditResponse> getAuditEventsByActor(Long actorUserId, Pageable pageable) {
        return repository.findByActorUserId(actorUserId, pageable)
                .map(ProcurementAuditResponse::from);
    }

    @Override
    public AuditChainVerificationResponse verifyAuditChain() {
        List<ProcurementAuditEvent> events = repository.findAllByOrderByIdAsc();

        String expectedPreviousHash = null;

        long checked = 0;

        for (ProcurementAuditEvent event : events) {
            checked++;

            String actualPreviousHash = event.getPreviousEventHash();

            if (!safe(expectedPreviousHash).equals(safe(actualPreviousHash))) {
                return AuditChainVerificationResponse.builder()
                        .valid(false)
                        .checkedEvents(checked)
                        .failedEventId(event.getId())
                        .message("Procurement audit hash chain verification failed. Previous hash mismatch.")
                        .build();
            }

            String storedHash = event.getEventHash();

            String recalculatedHash = generateEventHash(event);

            if (!safe(storedHash).equals(safe(recalculatedHash))) {
                return AuditChainVerificationResponse.builder()
                        .valid(false)
                        .checkedEvents(checked)
                        .failedEventId(event.getId())
                        .message("Procurement audit hash chain verification failed. Event hash mismatch.")
                        .build();
            }
            expectedPreviousHash = storedHash;
        }

        return AuditChainVerificationResponse.builder()
                .valid(true)
                .checkedEvents(checked)
                .failedEventId(null)
                .message("Procurement audit hash chain verified.")
                .build();
    }

    private String generateEventHash(ProcurementAuditEvent event) {
        String source = String.join("|",
                safe(event.getEventType() != null ? event.getEventType().name() : null),
                safe(event.getTenderId()),
                safe(event.getBidId()),
                safe(event.getActorUserId()),
                safe(event.getResult() != null ? event.getResult().name() : null),
                safe(event.getMessage()),
                safe(event.getDetails()),
                safe(event.getPreviousEventHash()),
                safe(event.getCreatedAt() != null
                        ? event.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
        );

        return sha256(source);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate audit event hash", ex);
        }
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
