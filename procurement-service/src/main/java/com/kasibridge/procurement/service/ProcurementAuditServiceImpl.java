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
public class ProcurementAuditServiceImpl
        implements ProcurementAuditService {

    private static final long AUDIT_CHAIN_LOCK_KEY =
            4_917_331_001L;

    private final ProcurementAuditEventRepository repository;

    @Override
    @Transactional
    public void recordSuccess(
            ProcurementAuditEvent.AuditEventType eventType,
            Long tenderId,
            Long bidId,
            Long actorUserId,
            String message,
            String details
    ) {
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
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void recordFailure(
            ProcurementAuditEvent.AuditEventType eventType,
            Long tenderId,
            Long bidId,
            Long actorUserId,
            String message,
            String details
    ) {
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

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void recordRejected(
            ProcurementAuditEvent.AuditEventType eventType,
            Long tenderId,
            Long bidId,
            Long actorUserId,
            String summary,
            String details
    ) {
        record(
                eventType,
                tenderId,
                bidId,
                actorUserId,
                ProcurementAuditEvent.AuditResult.REJECTED,
                summary,
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
            repository.acquireAuditChainLock(
                    AUDIT_CHAIN_LOCK_KEY
            );

            String previousHash =
                    repository
                            .findTopByOrderByIdDesc()
                            .map(
                                    ProcurementAuditEvent
                                            ::getEventHash
                            )
                            .orElse(null);

            LocalDateTime createdAt =
                    LocalDateTime.now()
                            .truncatedTo(
                                    ChronoUnit.MICROS
                            );

            ProcurementAuditEvent event =
                    ProcurementAuditEvent.builder()
                            .eventType(eventType)
                            .tenderId(tenderId)
                            .bidId(bidId)
                            .actorUserId(actorUserId)
                            .result(result)
                            .message(
                                    trimToLength(
                                            message,
                                            1000
                                    )
                            )
                            .details(
                                    trimToLength(
                                            details,
                                            3000
                                    )
                            )
                            .createdAt(createdAt)
                            .previousEventHash(
                                    previousHash
                            )
                            .eventHash("PENDING")
                            .build();

            String eventHash =
                    generateEventHash(event);

            event.setEventHash(eventHash);

            repository.saveAndFlush(event);

            log.info(
                    "Procurement audit event recorded: "
                            + "type={} tenderId={} bidId={} "
                            + "actorUserId={} result={} hash={}",
                    eventType,
                    tenderId,
                    bidId,
                    actorUserId,
                    result,
                    eventHash
            );
        } catch (Exception exception) {
            log.error(
                    "Failed to record procurement audit event: "
                            + "type={} tenderId={} bidId={} "
                            + "actorUserId={} result={}",
                    eventType,
                    tenderId,
                    bidId,
                    actorUserId,
                    result,
                    exception
            );

            throw exception;
        }
    }

    private String trimToLength(
            String value,
            int maximumLength
    ) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maximumLength) {
            return value;
        }

        return value.substring(
                0,
                maximumLength
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementAuditResponse>
    getAuditEvents(
            Pageable pageable
    ) {
        return repository
                .findAll(pageable)
                .map(
                        ProcurementAuditResponse::from
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementAuditResponse>
    getAuditEventsByTender(
            Long tenderId,
            Pageable pageable
    ) {
        return repository
                .findByTenderId(
                        tenderId,
                        pageable
                )
                .map(
                        ProcurementAuditResponse::from
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementAuditResponse>
    getAuditEventsByBid(
            Long bidId,
            Pageable pageable
    ) {
        return repository
                .findByBidId(
                        bidId,
                        pageable
                )
                .map(
                        ProcurementAuditResponse::from
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementAuditResponse>
    getAuditEventsByActor(
            Long actorUserId,
            Pageable pageable
    ) {
        return repository
                .findByActorUserId(
                        actorUserId,
                        pageable
                )
                .map(
                        ProcurementAuditResponse::from
                );
    }

    @Override
    @Transactional(readOnly = true)
    public AuditChainVerificationResponse
    verifyAuditChain() {
        List<ProcurementAuditEvent> events =
                repository
                        .findAllByOrderByIdAsc();

        String expectedPreviousHash = null;
        long checkedEvents = 0;

        for (
                ProcurementAuditEvent event
                : events
        ) {
            checkedEvents++;

            String actualPreviousHash =
                    event.getPreviousEventHash();

            if (
                    !safe(expectedPreviousHash)
                            .equals(
                                    safe(
                                            actualPreviousHash
                                    )
                            )
            ) {
                return AuditChainVerificationResponse
                        .builder()
                        .valid(false)
                        .checkedEvents(
                                checkedEvents
                        )
                        .failedEventId(
                                event.getId()
                        )
                        .message(
                                "Procurement audit hash "
                                        + "chain verification "
                                        + "failed. Previous "
                                        + "hash mismatch."
                        )
                        .build();
            }

            String storedHash =
                    event.getEventHash();

            String recalculatedHash =
                    generateEventHash(event);

            if (
                    !safe(storedHash)
                            .equals(
                                    safe(
                                            recalculatedHash
                                    )
                            )
            ) {
                return AuditChainVerificationResponse
                        .builder()
                        .valid(false)
                        .checkedEvents(
                                checkedEvents
                        )
                        .failedEventId(
                                event.getId()
                        )
                        .message(
                                "Procurement audit hash "
                                        + "chain verification "
                                        + "failed. Event hash "
                                        + "mismatch."
                        )
                        .build();
            }

            expectedPreviousHash =
                    storedHash;
        }

        return AuditChainVerificationResponse
                .builder()
                .valid(true)
                .checkedEvents(
                        checkedEvents
                )
                .failedEventId(null)
                .message(
                        "Procurement audit hash "
                                + "chain verified."
                )
                .build();
    }

    private String generateEventHash(
            ProcurementAuditEvent event
    ) {
        String source =
                String.join(
                        "|",
                        safe(
                                event.getEventType()
                                        != null
                                        ? event
                                        .getEventType()
                                        .name()
                                        : null
                        ),
                        safe(
                                event.getTenderId()
                        ),
                        safe(
                                event.getBidId()
                        ),
                        safe(
                                event.getActorUserId()
                        ),
                        safe(
                                event.getResult()
                                        != null
                                        ? event
                                        .getResult()
                                        .name()
                                        : null
                        ),
                        safe(
                                event.getMessage()
                        ),
                        safe(
                                event.getDetails()
                        ),
                        safe(
                                event.getPreviousEventHash()
                        ),
                        safe(
                                event.getCreatedAt()
                                        != null
                                        ? event
                                        .getCreatedAt()
                                        .format(
                                                DateTimeFormatter
                                                        .ISO_LOCAL_DATE_TIME
                                        )
                                        : null
                        )
                );

        return sha256(source);
    }

    private String sha256(
            String input
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] encodedHash =
                    digest.digest(
                            input.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hexadecimal =
                    new StringBuilder();

            for (byte value : encodedHash) {
                String hex =
                        Integer.toHexString(
                                0xff & value
                        );

                if (hex.length() == 1) {
                    hexadecimal.append('0');
                }

                hexadecimal.append(hex);
            }

            return hexadecimal.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to generate audit "
                            + "event hash",
                    exception
            );
        }
    }

    private String safe(
            Object value
    ) {
        return value == null
                ? ""
                : String.valueOf(value).trim();
    }
}