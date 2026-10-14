package com.kasibridge.procurement.service;

import com.kasibridge.procurement.entity.ProcurementAuditEvent;
import com.kasibridge.procurement.repository.ProcurementAuditEventRepository;
import com.kasibridge.procurement.testsupport.ProcurementAuditTestCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
class ProcurementAuditImmutabilityIntegrationTest {

    private static final Long TENDER_ID = 301L;
    private static final Long ACTOR_USER_ID = 73L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "audit_immutability_test"
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

    @Autowired
    private ProcurementAuditTestCleaner auditTestCleaner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        auditTestCleaner.clearAuditEvents();

        auditService.recordSuccess(
                ProcurementAuditEvent
                        .AuditEventType
                        .SUPPORT_TICKET_AI_ASSESSMENT_CREATED,
                TENDER_ID,
                null,
                ACTOR_USER_ID,
                "Initial immutable audit event",
                "Audit immutability integration test"
        );
    }

    @Test
    void normalAuditInsert_shouldRemainAllowed() {
        auditService.recordRejected(
                ProcurementAuditEvent
                        .AuditEventType
                        .SUPPORT_TICKET_ASSIGNMENT_REJECTED,
                TENDER_ID,
                null,
                ACTOR_USER_ID,
                "Rejected test event",
                "Append operation remains permitted"
        );

        assertEquals(
                2L,
                repository.count()
        );
    }

    @Test
    void nativeUpdate_shouldBeRejected() {
        DataAccessException exception =
                assertThrows(
                        DataAccessException.class,
                        () -> jdbcTemplate.update(
                                """
                                UPDATE procurement_audit_events
                                SET message = ?
                                """,
                                "Tampered audit message"
                        )
                );

        assertContainsAppendOnlyMessage(exception);

        assertEquals(
                "Initial immutable audit event",
                repository
                        .findAllByOrderByIdAsc()
                        .getFirst()
                        .getMessage()
        );
    }

    @Test
    void nativeDelete_shouldBeRejected() {
        DataAccessException exception =
                assertThrows(
                        DataAccessException.class,
                        () -> jdbcTemplate.update(
                                """
                                DELETE FROM
                                    procurement_audit_events
                                """
                        )
                );

        assertContainsAppendOnlyMessage(exception);

        assertEquals(
                1L,
                repository.count()
        );
    }

    @Test
    void nativeTruncate_shouldBeRejected() {
        DataAccessException exception =
                assertThrows(
                        DataAccessException.class,
                        () -> jdbcTemplate.execute(
                                """
                                TRUNCATE TABLE
                                    procurement_audit_events
                                """
                        )
                );

        assertContainsAppendOnlyMessage(exception);

        assertEquals(
                1L,
                repository.count()
        );
    }

    @Test
    void repositoryDeleteAll_shouldBeRejected() {
        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> {
                            repository.deleteAll();
                            repository.flush();
                        }
                );

        assertContainsAppendOnlyMessage(exception);

        assertEquals(
                1L,
                repository.count()
        );
    }

    private void assertContainsAppendOnlyMessage(
            Throwable exception
    ) {
        Throwable current = exception;
        boolean found = false;

        while (current != null) {
            String message = current.getMessage();

            if (
                    message != null
                            && message.contains(
                            "Procurement audit events "
                                    + "are append-only"
                    )
            ) {
                found = true;
                break;
            }

            current = current.getCause();
        }

        assertEquals(
                true,
                found,
                "Expected append-only trigger message"
        );
    }
}