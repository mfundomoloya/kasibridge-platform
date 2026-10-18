package com.kasibridge.procurement.service;

import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.validate-on-migrate=true",
                "spring.flyway.clean-disabled=true",
                "kasibridge.notifications.dispatch.enabled=false",
                "kasibridge.notifications.mock-whatsapp.enabled=true",
                "kasibridge.notifications.whatsapp.enabled=false",
                "kasibridge.ticket-ai.auto-assessment.enabled=false",
                "kasibridge.internal.system.username=test-system",
                "kasibridge.internal.system.password=test-password"
        }
)
@Testcontainers
@SuppressWarnings({
        "resource",
        "SpringBootApplicationProperties"
})
class NotificationDispatchClaimConcurrencyIntegrationTest {

    private static final int MAX_RETRIES = 3;

    private static final int LEASE_SECONDS = 60;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "notification_claim_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private NotificationDispatchClaimService claimService;

    @Autowired
    private NotificationOutboxRepository repository;

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
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.flush();
    }

    @Test
    void concurrentWorkers_shouldNotClaimSameNotification()
            throws Exception {
        NotificationOutbox notification =
                savePendingNotification(
                        "KB-NOTIF-CLAIM01",
                        81L,
                        "+27610000001"
                );

        CountDownLatch readyLatch =
                new CountDownLatch(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<List<NotificationOutbox>> workerOne =
                    executor.submit(
                            claimTask(
                                    readyLatch,
                                    startLatch,
                                    "worker-one",
                                    1
                            )
                    );

            Future<List<NotificationOutbox>> workerTwo =
                    executor.submit(
                            claimTask(
                                    readyLatch,
                                    startLatch,
                                    "worker-two",
                                    1
                            )
                    );

            assertTrue(
                    readyLatch.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "Workers did not become ready"
            );

            startLatch.countDown();

            List<NotificationOutbox> firstResult =
                    workerOne.get(
                            15,
                            TimeUnit.SECONDS
                    );

            List<NotificationOutbox> secondResult =
                    workerTwo.get(
                            15,
                            TimeUnit.SECONDS
                    );

            int totalClaimed =
                    firstResult.size()
                            + secondResult.size();

            assertEquals(
                    1,
                    totalClaimed
            );

            Set<Long> claimedIds =
                    Stream.concat(
                                    firstResult.stream(),
                                    secondResult.stream()
                            )
                            .map(
                                    NotificationOutbox::getId
                            )
                            .collect(
                                    Collectors.toSet()
                            );

            assertEquals(
                    Set.of(
                            notification.getId()
                    ),
                    claimedIds
            );

            NotificationOutbox stored =
                    repository.findById(
                                    notification.getId()
                            )
                            .orElseThrow();

            assertEquals(
                    NotificationOutbox
                            .NotificationStatus
                            .PROCESSING,
                    stored.getStatus()
            );

            assertEquals(
                    1,
                    stored.getRetryCount()
            );

            assertNotNull(
                    stored.getClaimedAt()
            );

            assertNotNull(
                    stored.getClaimedBy()
            );

            assertNotNull(
                    stored.getClaimExpiresAt()
            );

            assertTrue(
                    Set.of(
                            "worker-one",
                            "worker-two"
                    ).contains(
                            stored.getClaimedBy()
                    )
            );

            assertTrue(
                    stored.getClaimExpiresAt()
                            .isAfter(
                                    stored.getClaimedAt()
                            )
            );
        } finally {
            shutdownExecutor(
                    executor
            );
        }
    }

    @Test
    void concurrentWorkers_shouldClaimDifferentNotifications()
            throws Exception {
        NotificationOutbox firstNotification =
                savePendingNotification(
                        "KB-NOTIF-CLAIM02",
                        82L,
                        "+27610000002"
                );

        Thread.sleep(5);

        NotificationOutbox secondNotification =
                savePendingNotification(
                        "KB-NOTIF-CLAIM03",
                        83L,
                        "+27610000003"
                );

        CountDownLatch readyLatch =
                new CountDownLatch(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<List<NotificationOutbox>> workerOne =
                    executor.submit(
                            claimTask(
                                    readyLatch,
                                    startLatch,
                                    "worker-one",
                                    1
                            )
                    );

            Future<List<NotificationOutbox>> workerTwo =
                    executor.submit(
                            claimTask(
                                    readyLatch,
                                    startLatch,
                                    "worker-two",
                                    1
                            )
                    );

            assertTrue(
                    readyLatch.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "Workers did not become ready"
            );

            startLatch.countDown();

            List<NotificationOutbox> firstResult =
                    workerOne.get(
                            15,
                            TimeUnit.SECONDS
                    );

            List<NotificationOutbox> secondResult =
                    workerTwo.get(
                            15,
                            TimeUnit.SECONDS
                    );

            assertEquals(
                    1,
                    firstResult.size()
            );

            assertEquals(
                    1,
                    secondResult.size()
            );

            Long firstClaimedId =
                    firstResult.getFirst()
                            .getId();

            Long secondClaimedId =
                    secondResult.getFirst()
                            .getId();

            assertNotEquals(
                    firstClaimedId,
                    secondClaimedId
            );

            assertEquals(
                    Set.of(
                            firstNotification.getId(),
                            secondNotification.getId()
                    ),
                    Set.of(
                            firstClaimedId,
                            secondClaimedId
                    )
            );

            List<NotificationOutbox> stored =
                    repository.findAll();

            assertEquals(
                    2,
                    stored.size()
            );

            assertTrue(
                    stored.stream()
                            .allMatch(notification ->
                                    notification.getStatus()
                                            == NotificationOutbox
                                            .NotificationStatus
                                            .PROCESSING
                            )
            );

            assertTrue(
                    stored.stream()
                            .allMatch(notification ->
                                    notification.getRetryCount()
                                            == 1
                            )
            );

            assertTrue(
                    stored.stream()
                            .allMatch(notification ->
                                    notification.getClaimedAt()
                                            != null
                            )
            );

            assertTrue(
                    stored.stream()
                            .allMatch(notification ->
                                    notification
                                            .getClaimExpiresAt()
                                            != null
                            )
            );

            Set<String> workers =
                    stored.stream()
                            .map(
                                    NotificationOutbox
                                            ::getClaimedBy
                            )
                            .collect(
                                    Collectors.toSet()
                            );

            assertEquals(
                    Set.of(
                            "worker-one",
                            "worker-two"
                    ),
                    workers
            );
        } finally {
            shutdownExecutor(
                    executor
            );
        }
    }

    @Test
    void activeClaim_shouldNotBeClaimedAgain() {
        NotificationOutbox notification =
                savePendingNotification(
                        "KB-NOTIF-CLAIM04",
                        84L,
                        "+27610000004"
                );

        List<NotificationOutbox> firstClaim =
                claimService.claimBatch(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP,
                        MAX_RETRIES,
                        1,
                        "worker-one",
                        LEASE_SECONDS
                );

        List<NotificationOutbox> secondClaim =
                claimService.claimBatch(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP,
                        MAX_RETRIES,
                        1,
                        "worker-two",
                        LEASE_SECONDS
                );

        assertEquals(
                1,
                firstClaim.size()
        );

        assertTrue(
                secondClaim.isEmpty()
        );

        NotificationOutbox stored =
                repository.findById(
                                notification.getId()
                        )
                        .orElseThrow();

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .PROCESSING,
                stored.getStatus()
        );

        assertEquals(
                "worker-one",
                stored.getClaimedBy()
        );

        assertEquals(
                1,
                stored.getRetryCount()
        );
    }

    @Test
    void expiredClaim_shouldBeReclaimed() {
        LocalDateTime now =
                LocalDateTime.now();

        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .notificationReference(
                                "KB-NOTIF-CLAIM05"
                        )
                        .channel(
                                NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_RECEIVED
                        )
                        .status(
                                NotificationOutbox
                                        .NotificationStatus
                                        .PROCESSING
                        )
                        .recipientUserId(85L)
                        .recipientPhone(
                                "+27610000005"
                        )
                        .message(
                                "Expired dispatch claim test."
                        )
                        .retryCount(1)
                        .claimedAt(
                                now.minusMinutes(5)
                        )
                        .claimedBy(
                                "failed-worker"
                        )
                        .claimExpiresAt(
                                now.minusMinutes(4)
                        )
                        .createdAt(
                                now.minusMinutes(10)
                        )
                        .updatedAt(
                                now.minusMinutes(5)
                        )
                        .build();

        NotificationOutbox saved =
                repository.saveAndFlush(
                        notification
                );

        List<NotificationOutbox> claimed =
                claimService.claimBatch(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP,
                        MAX_RETRIES,
                        1,
                        "recovery-worker",
                        LEASE_SECONDS
                );

        assertEquals(
                1,
                claimed.size()
        );

        assertEquals(
                saved.getId(),
                claimed.getFirst()
                        .getId()
        );

        NotificationOutbox stored =
                repository.findById(
                                saved.getId()
                        )
                        .orElseThrow();

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .PROCESSING,
                stored.getStatus()
        );

        assertEquals(
                "recovery-worker",
                stored.getClaimedBy()
        );

        assertEquals(
                2,
                stored.getRetryCount()
        );

        assertNotNull(
                stored.getClaimedAt()
        );

        assertNotNull(
                stored.getClaimExpiresAt()
        );

        assertTrue(
                stored.getClaimExpiresAt()
                        .isAfter(
                                LocalDateTime.now()
                        )
        );
    }

    @Test
    void failedNotificationBeforeNextAttempt_shouldNotBeClaimed() {
        LocalDateTime now =
                LocalDateTime.now();

        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .notificationReference(
                                "KB-NOTIF-CLAIM06"
                        )
                        .channel(
                                NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_COMPLIANCE_FAILED
                        )
                        .status(
                                NotificationOutbox
                                        .NotificationStatus
                                        .FAILED
                        )
                        .recipientUserId(86L)
                        .recipientPhone(
                                "+27610000006"
                        )
                        .message(
                                "Deferred retry claim test."
                        )
                        .retryCount(1)
                        .nextAttemptAt(
                                now.plusMinutes(10)
                        )
                        .failureReason(
                                "Temporary provider failure."
                        )
                        .createdAt(
                                now.minusMinutes(2)
                        )
                        .updatedAt(now)
                        .build();

        NotificationOutbox saved =
                repository.saveAndFlush(
                        notification
                );

        List<NotificationOutbox> claimed =
                claimService.claimBatch(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP,
                        MAX_RETRIES,
                        5,
                        "worker-one",
                        LEASE_SECONDS
                );

        assertTrue(
                claimed.isEmpty()
        );

        NotificationOutbox stored =
                repository.findById(
                                saved.getId()
                        )
                        .orElseThrow();

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .FAILED,
                stored.getStatus()
        );

        assertEquals(
                1,
                stored.getRetryCount()
        );

        assertNull(
                stored.getClaimedBy()
        );

        assertNotNull(
                stored.getNextAttemptAt()
        );

        assertNotNull(
                stored.getNextAttemptAt()
        );

        long timestampDifferenceMicros =
                Math.abs(
                        ChronoUnit.MICROS.between(
                                notification.getNextAttemptAt(),
                                stored.getNextAttemptAt()
                        )
                );

        assertTrue(
                timestampDifferenceMicros <= 1,
                () ->
                        "Expected nextAttemptAt to remain "
                                + "unchanged within PostgreSQL "
                                + "microsecond precision, but "
                                + "difference was "
                                + timestampDifferenceMicros
                                + " microseconds"
        );
    }

    @Test
    void failedNotificationAtRetryTime_shouldBeClaimed() {
        LocalDateTime now =
                LocalDateTime.now();

        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .notificationReference(
                                "KB-NOTIF-CLAIM07"
                        )
                        .channel(
                                NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_COMPLIANCE_FAILED
                        )
                        .status(
                                NotificationOutbox
                                        .NotificationStatus
                                        .FAILED
                        )
                        .recipientUserId(87L)
                        .recipientPhone(
                                "+27610000007"
                        )
                        .message(
                                "Eligible retry claim test."
                        )
                        .retryCount(1)
                        .nextAttemptAt(
                                now.minusSeconds(1)
                        )
                        .failureReason(
                                "Temporary provider failure."
                        )
                        .createdAt(
                                now.minusMinutes(2)
                        )
                        .updatedAt(now)
                        .build();

        NotificationOutbox saved =
                repository.saveAndFlush(
                        notification
                );

        List<NotificationOutbox> claimed =
                claimService.claimBatch(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP,
                        MAX_RETRIES,
                        5,
                        "worker-one",
                        LEASE_SECONDS
                );

        assertEquals(
                1,
                claimed.size()
        );

        assertEquals(
                saved.getId(),
                claimed.getFirst()
                        .getId()
        );

        NotificationOutbox stored =
                repository.findById(
                                saved.getId()
                        )
                        .orElseThrow();

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .PROCESSING,
                stored.getStatus()
        );

        assertEquals(
                2,
                stored.getRetryCount()
        );

        assertEquals(
                "worker-one",
                stored.getClaimedBy()
        );

        assertNull(
                stored.getNextAttemptAt()
        );
    }

    @Test
    void maximumRetryNotification_shouldNotBeClaimed() {
        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .notificationReference(
                                "KB-NOTIF-CLAIM08"
                        )
                        .channel(
                                NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_COMPLIANCE_FAILED
                        )
                        .status(
                                NotificationOutbox
                                        .NotificationStatus
                                        .FAILED
                        )
                        .recipientUserId(88L)
                        .recipientPhone(
                                "+27610000008"
                        )
                        .message(
                                "Maximum retry test."
                        )
                        .retryCount(
                                MAX_RETRIES
                        )
                        .createdAt(
                                LocalDateTime.now()
                                        .minusMinutes(1)
                        )
                        .updatedAt(
                                LocalDateTime.now()
                        )
                        .build();

        NotificationOutbox saved =
                repository.saveAndFlush(
                        notification
                );

        List<NotificationOutbox> claimed =
                claimService.claimBatch(
                        NotificationOutbox
                                .NotificationChannel
                                .WHATSAPP,
                        MAX_RETRIES,
                        5,
                        "worker-one",
                        LEASE_SECONDS
                );

        assertTrue(
                claimed.isEmpty()
        );

        NotificationOutbox stored =
                repository.findById(
                                saved.getId()
                        )
                        .orElseThrow();

        assertEquals(
                MAX_RETRIES,
                stored.getRetryCount()
        );

        assertEquals(
                NotificationOutbox
                        .NotificationStatus
                        .FAILED,
                stored.getStatus()
        );
    }

    private Callable<List<NotificationOutbox>> claimTask(
            CountDownLatch readyLatch,
            CountDownLatch startLatch,
            String workerId,
            int batchSize
    ) {
        return () -> {
            readyLatch.countDown();

            assertTrue(
                    startLatch.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "Worker did not receive start signal"
            );

            return claimService.claimBatch(
                    NotificationOutbox
                            .NotificationChannel
                            .WHATSAPP,
                    MAX_RETRIES,
                    batchSize,
                    workerId,
                    LEASE_SECONDS
            );
        };
    }

    private NotificationOutbox savePendingNotification(
            String reference,
            Long recipientUserId,
            String phoneNumber
    ) {
        LocalDateTime now =
                LocalDateTime.now();

        NotificationOutbox notification =
                NotificationOutbox.builder()
                        .notificationReference(
                                reference
                        )
                        .channel(
                                NotificationOutbox
                                        .NotificationChannel
                                        .WHATSAPP
                        )
                        .templateType(
                                NotificationOutbox
                                        .NotificationTemplateType
                                        .BID_RECEIVED
                        )
                        .status(
                                NotificationOutbox
                                        .NotificationStatus
                                        .PENDING
                        )
                        .recipientUserId(
                                recipientUserId
                        )
                        .recipientPhone(
                                phoneNumber
                        )
                        .message(
                                "Notification dispatch "
                                        + "claim integration test."
                        )
                        .retryCount(0)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        return repository.saveAndFlush(
                notification
        );
    }

    private void shutdownExecutor(
            ExecutorService executor
    ) throws InterruptedException {
        executor.shutdownNow();

        assertTrue(
                executor.awaitTermination(
                        10,
                        TimeUnit.SECONDS
                ),
                "Executor did not terminate"
        );
    }
}