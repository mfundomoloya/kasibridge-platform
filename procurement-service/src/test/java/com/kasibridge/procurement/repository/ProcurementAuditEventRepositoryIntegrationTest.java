package com.kasibridge.procurement.repository;

import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.testsupport.ProcurementAuditTestCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class ProcurementAuditEventRepositoryIntegrationTest {
    @Autowired
    private ProcurementAuditTestCleaner auditTestCleaner;
    private static final long AUDIT_CHAIN_LOCK_KEY =
            4_917_331_001L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "audit_repository_test"
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

        registry.add(
                "spring.flyway.enabled",
                () -> "false"
        );
    }

    @Autowired
    private ProcurementAuditEventRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        auditTestCleaner.clearAuditEvents();
    }

    @Test
    void acquireAuditChainLock_shouldExecuteInsideTransaction() {
        Boolean completed =
                transactionTemplate.execute(status -> {
                    repository.acquireAuditChainLock(
                            AUDIT_CHAIN_LOCK_KEY
                    );

                    return true;
                });

        assertEquals(
                Boolean.TRUE,
                completed
        );
    }

    @Test
    void findTopByOrderByIdDesc_shouldReturnLatestEvent() {
        ProcurementAuditEvent first =
                repository.saveAndFlush(
                        buildEvent(
                                "first-event-hash",
                                null,
                                "First audit event"
                        )
                );

        ProcurementAuditEvent second =
                repository.saveAndFlush(
                        buildEvent(
                                "second-event-hash",
                                first.getEventHash(),
                                "Second audit event"
                        )
                );

        Optional<ProcurementAuditEvent> latest =
                repository.findTopByOrderByIdDesc();

        assertTrue(latest.isPresent());

        assertEquals(
                second.getId(),
                latest.orElseThrow().getId()
        );

        assertEquals(
                "second-event-hash",
                latest.orElseThrow().getEventHash()
        );

        assertEquals(
                "first-event-hash",
                latest.orElseThrow()
                        .getPreviousEventHash()
        );
    }

    @Test
    void findAllByOrderByIdAsc_shouldPreserveChainOrder() {
        ProcurementAuditEvent first =
                repository.saveAndFlush(
                        buildEvent(
                                "hash-001",
                                null,
                                "First event"
                        )
                );

        ProcurementAuditEvent second =
                repository.saveAndFlush(
                        buildEvent(
                                "hash-002",
                                first.getEventHash(),
                                "Second event"
                        )
                );

        ProcurementAuditEvent third =
                repository.saveAndFlush(
                        buildEvent(
                                "hash-003",
                                second.getEventHash(),
                                "Third event"
                        )
                );

        var events =
                repository.findAllByOrderByIdAsc();

        assertEquals(
                3,
                events.size()
        );

        assertEquals(
                first.getId(),
                events.get(0).getId()
        );

        assertEquals(
                second.getId(),
                events.get(1).getId()
        );

        assertEquals(
                third.getId(),
                events.get(2).getId()
        );

        assertEquals(
                events.get(0).getEventHash(),
                events.get(1)
                        .getPreviousEventHash()
        );

        assertEquals(
                events.get(1).getEventHash(),
                events.get(2)
                        .getPreviousEventHash()
        );

        assertNotNull(
                events.get(2).getCreatedAt()
        );
    }

    private ProcurementAuditEvent buildEvent(
            String eventHash,
            String previousEventHash,
            String message
    ) {
        return ProcurementAuditEvent.builder()
                .eventType(
                        ProcurementAuditEvent
                                .AuditEventType
                                .SUPPORT_TICKET_AI_ASSESSMENT_CREATED
                )
                .tenderId(301L)
                .bidId(null)
                .actorUserId(73L)
                .result(
                        ProcurementAuditEvent
                                .AuditResult
                                .SUCCESS
                )
                .message(message)
                .details(
                        "Repository integration test"
                )
                .previousEventHash(
                        previousEventHash
                )
                .eventHash(eventHash)
                .createdAt(
                        LocalDateTime.now()
                                .truncatedTo(
                                        ChronoUnit.MICROS
                                )
                )
                .build();
    }
}