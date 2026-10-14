package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.AuditChainVerificationResponse;
import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.repository.ProcurementAuditEventRepository;
import com.kasibridge.procurement.testsupport.ProcurementAuditTestCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = {
                "kasibridge.notifications.dispatch.enabled=false",
                "kasibridge.notifications.mock-whatsapp.enabled=true",
                "kasibridge.notifications.whatsapp.enabled=false",
                "kasibridge.ticket-ai.auto-assessment.enabled=false",
                "kasibridge.internal.system.username=test-system",
                "kasibridge.internal.system.password=test-password"
        }
)
@Testcontainers
class ProcurementAuditChainConcurrencyIntegrationTest {

    private static final int CONCURRENT_WRITER_COUNT = 12;
    private static final Long TENDER_ID = 301L;
    @Autowired
    private ProcurementAuditTestCleaner auditTestCleaner;
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "audit_chain_concurrency_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop"
        );
    }

    @Autowired
    private ProcurementAuditService auditService;

    @Autowired
    private ProcurementAuditEventRepository repository;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        auditTestCleaner.clearAuditEvents();

        executorService =
                Executors.newFixedThreadPool(
                        CONCURRENT_WRITER_COUNT
                );
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executorService == null) {
            return;
        }

        executorService.shutdownNow();

        assertTrue(
                executorService.awaitTermination(
                        10,
                        TimeUnit.SECONDS
                )
        );
    }

    @Test
    void concurrentSuccessWrites_shouldCreateSingleValidChain()
            throws Exception {
        CountDownLatch readyLatch =
                new CountDownLatch(
                        CONCURRENT_WRITER_COUNT
                );

        CountDownLatch startLatch =
                new CountDownLatch(1);

        List<Future<AuditWriteResult>> futures =
                createConcurrentWrites(
                        readyLatch,
                        startLatch
                );

        assertTrue(
                readyLatch.await(
                        10,
                        TimeUnit.SECONDS
                ),
                "All audit writers should be ready "
                        + "before release."
        );

        startLatch.countDown();

        List<AuditWriteResult> results =
                futures.stream()
                        .map(this::getResult)
                        .toList();

        assertEquals(
                CONCURRENT_WRITER_COUNT,
                results.size()
        );

        assertTrue(
                results.stream()
                        .allMatch(
                                AuditWriteResult::successful
                        )
        );

        assertTrue(
                results.stream()
                        .allMatch(
                                result ->
                                        result.failure() == null
                        )
        );

        List<ProcurementAuditEvent> events =
                repository
                        .findAllByOrderByIdAsc();

        assertEquals(
                CONCURRENT_WRITER_COUNT,
                events.size()
        );

        assertNull(
                events.getFirst()
                        .getPreviousEventHash()
        );

        for (
                int index = 1;
                index < events.size();
                index++
        ) {
            ProcurementAuditEvent previousEvent =
                    events.get(index - 1);

            ProcurementAuditEvent currentEvent =
                    events.get(index);

            assertEquals(
                    previousEvent.getEventHash(),
                    currentEvent
                            .getPreviousEventHash()
            );
        }

        Set<String> eventHashes =
                new HashSet<>();

        for (
                ProcurementAuditEvent event
                : events
        ) {
            assertNotNull(
                    event.getEventHash()
            );

            assertTrue(
                    eventHashes.add(
                            event.getEventHash()
                    ),
                    "Every audit event hash "
                            + "must be unique."
            );
        }

        Set<String> predecessorHashes =
                new HashSet<>();

        for (
                int index = 1;
                index < events.size();
                index++
        ) {
            String predecessorHash =
                    events.get(index)
                            .getPreviousEventHash();

            assertNotNull(
                    predecessorHash
            );

            assertTrue(
                    predecessorHashes.add(
                            predecessorHash
                    ),
                    "No two committed audit events "
                            + "may claim the same predecessor."
            );
        }

        AuditChainVerificationResponse verification =
                auditService.verifyAuditChain();

        assertTrue(
                verification.isValid()
        );

        assertEquals(
                CONCURRENT_WRITER_COUNT,
                verification.getCheckedEvents()
        );

        assertNull(
                verification.getFailedEventId()
        );

        assertEquals(
                "Procurement audit hash chain verified.",
                verification.getMessage()
        );
    }

    @Test
    void concurrentMixedResults_shouldCreateSingleValidChain()
            throws Exception {
        int writerCount = 9;

        CountDownLatch readyLatch =
                new CountDownLatch(
                        writerCount
                );

        CountDownLatch startLatch =
                new CountDownLatch(1);

        List<Future<AuditWriteResult>> futures =
                java.util.stream.IntStream
                        .range(0, writerCount)
                        .mapToObj(index ->
                                executorService.submit(
                                        () ->
                                                writeMixedAuditEvent(
                                                        index,
                                                        readyLatch,
                                                        startLatch
                                                )
                                )
                        )
                        .toList();

        assertTrue(
                readyLatch.await(
                        10,
                        TimeUnit.SECONDS
                )
        );

        startLatch.countDown();

        List<AuditWriteResult> results =
                futures.stream()
                        .map(this::getResult)
                        .toList();

        assertTrue(
                results.stream()
                        .allMatch(
                                AuditWriteResult::successful
                        )
        );

        List<ProcurementAuditEvent> events =
                repository
                        .findAllByOrderByIdAsc();

        assertEquals(
                writerCount,
                events.size()
        );

        assertEquals(
                3,
                events.stream()
                        .filter(event ->
                                event.getResult()
                                        == ProcurementAuditEvent
                                        .AuditResult
                                        .SUCCESS
                        )
                        .count()
        );

        assertEquals(
                3,
                events.stream()
                        .filter(event ->
                                event.getResult()
                                        == ProcurementAuditEvent
                                        .AuditResult
                                        .FAILED
                        )
                        .count()
        );

        assertEquals(
                3,
                events.stream()
                        .filter(event ->
                                event.getResult()
                                        == ProcurementAuditEvent
                                        .AuditResult
                                        .REJECTED
                        )
                        .count()
        );

        assertNull(
                events.getFirst()
                        .getPreviousEventHash()
        );

        for (
                int index = 1;
                index < events.size();
                index++
        ) {
            assertEquals(
                    events.get(index - 1)
                            .getEventHash(),
                    events.get(index)
                            .getPreviousEventHash()
            );
        }

        AuditChainVerificationResponse verification =
                auditService.verifyAuditChain();

        assertTrue(
                verification.isValid()
        );

        assertEquals(
                writerCount,
                verification.getCheckedEvents()
        );
    }

    private List<Future<AuditWriteResult>>
    createConcurrentWrites(
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) {
        return java.util.stream.IntStream
                .range(
                        0,
                        CONCURRENT_WRITER_COUNT
                )
                .mapToObj(index ->
                        executorService.submit(
                                () ->
                                        writeSuccessAuditEvent(
                                                index,
                                                readyLatch,
                                                startLatch
                                        )
                        )
                )
                .toList();
    }

    private AuditWriteResult writeSuccessAuditEvent(
            int index,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) {
        long actorUserId =
                1_000L + index;

        readyLatch.countDown();

        try {
            if (
                    !startLatch.await(
                            10,
                            TimeUnit.SECONDS
                    )
            ) {
                return AuditWriteResult.failure(
                        actorUserId,
                        new IllegalStateException(
                                "Audit writers were "
                                        + "not released."
                        )
                );
            }

            auditService.recordSuccess(
                    ProcurementAuditEvent
                            .AuditEventType
                            .SUPPORT_TICKET_AI_ASSESSMENT_CREATED,
                    TENDER_ID,
                    null,
                    actorUserId,
                    "Concurrent audit event "
                            + index,
                    "Concurrent audit writer="
                            + index
            );

            return AuditWriteResult.success(
                    actorUserId
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            return AuditWriteResult.failure(
                    actorUserId,
                    exception
            );
        } catch (Exception exception) {
            return AuditWriteResult.failure(
                    actorUserId,
                    exception
            );
        }
    }

    private AuditWriteResult writeMixedAuditEvent(
            int index,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) {
        long actorUserId =
                2_000L + index;

        readyLatch.countDown();

        try {
            if (
                    !startLatch.await(
                            10,
                            TimeUnit.SECONDS
                    )
            ) {
                return AuditWriteResult.failure(
                        actorUserId,
                        new IllegalStateException(
                                "Mixed audit writers "
                                        + "were not released."
                        )
                );
            }

            int resultType =
                    index % 3;

            if (resultType == 0) {
                auditService.recordSuccess(
                        ProcurementAuditEvent
                                .AuditEventType
                                .SUPPORT_TICKET_REVIEW_STARTED,
                        TENDER_ID,
                        null,
                        actorUserId,
                        "Concurrent successful event",
                        "Writer index=" + index
                );
            } else if (resultType == 1) {
                auditService.recordFailure(
                        ProcurementAuditEvent
                                .AuditEventType
                                .SUPPORT_TICKET_REASSIGNMENT_BLOCKED,
                        TENDER_ID,
                        null,
                        actorUserId,
                        "Concurrent failed event",
                        "Writer index=" + index
                );
            } else {
                auditService.recordRejected(
                        ProcurementAuditEvent
                                .AuditEventType
                                .SUPPORT_TICKET_ASSIGNMENT_REJECTED,
                        TENDER_ID,
                        null,
                        actorUserId,
                        "Concurrent rejected event",
                        "Writer index=" + index
                );
            }

            return AuditWriteResult.success(
                    actorUserId
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            return AuditWriteResult.failure(
                    actorUserId,
                    exception
            );
        } catch (Exception exception) {
            return AuditWriteResult.failure(
                    actorUserId,
                    exception
            );
        }
    }

    private AuditWriteResult getResult(
            Future<AuditWriteResult> future
    ) {
        try {
            return future.get(
                    30,
                    TimeUnit.SECONDS
            );
        } catch (Exception exception) {
            return AuditWriteResult.failure(
                    null,
                    exception
            );
        }
    }

    private record AuditWriteResult(
            Long actorUserId,
            boolean successful,
            Exception failure
    ) {
        private static AuditWriteResult success(
                Long actorUserId
        ) {
            return new AuditWriteResult(
                    actorUserId,
                    true,
                    null
            );
        }

        private static AuditWriteResult failure(
                Long actorUserId,
                Exception failure
        ) {
            return new AuditWriteResult(
                    actorUserId,
                    false,
                    failure
            );
        }
    }
}