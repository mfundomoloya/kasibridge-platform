package com.kasibridge.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.fraud.dto.FraudAssessmentDashboardResponse;
import com.kasibridge.fraud.repository.FraudAssessmentDashboardProjection;
import com.kasibridge.fraud.repository.FraudAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudAssessmentDashboardServiceTest {

    @Mock
    private InvoiceFraudPredictionService
            predictionService;

    @Mock
    private FraudAssessmentRepository
            assessmentRepository;

    @Mock
    private FraudAssessmentDashboardProjection
            dashboardProjection;

    private FraudAssessmentService
            assessmentService;

    @BeforeEach
    void setUp() {
        assessmentService =
                new FraudAssessmentService(
                        predictionService,
                        assessmentRepository,
                        new ObjectMapper()
                );
    }

    @Test
    void getAssessmentDashboard_shouldMapAggregateSummary() {
        LocalDateTime latestAssessedAt =
                LocalDateTime.now()
                        .minusMinutes(10);

        /*
         * The extra five minutes prevents this test
         * from crossing an exact hour boundary while
         * the service generates its current timestamp.
         */
        LocalDateTime oldestPendingAssessedAt =
                LocalDateTime.now()
                        .minusHours(27)
                        .minusMinutes(5);

        when(
                assessmentRepository
                        .getDashboardSummary()
        ).thenReturn(
                dashboardProjection
        );

        when(
                dashboardProjection
                        .getTotalAssessments()
        ).thenReturn(
                12L
        );

        when(
                dashboardProjection
                        .getPendingReview()
        ).thenReturn(
                3L
        );

        when(
                dashboardProjection
                        .getEscalated()
        ).thenReturn(
                1L
        );

        when(
                dashboardProjection
                        .getConfirmedFraud()
        ).thenReturn(
                4L
        );

        when(
                dashboardProjection
                        .getFalsePositive()
        ).thenReturn(
                2L
        );

        when(
                dashboardProjection
                        .getAutoCleared()
        ).thenReturn(
                2L
        );

        when(
                dashboardProjection
                        .getCriticalRisk()
        ).thenReturn(
                2L
        );

        when(
                dashboardProjection
                        .getHighRisk()
        ).thenReturn(
                6L
        );

        when(
                dashboardProjection
                        .getOpenHighOrCriticalRisk()
        ).thenReturn(
                4L
        );

        when(
                dashboardProjection
                        .getOldestPendingAssessedAt()
        ).thenReturn(
                oldestPendingAssessedAt
        );

        when(
                dashboardProjection
                        .getLatestAssessedAt()
        ).thenReturn(
                latestAssessedAt
        );

        LocalDateTime beforeCall =
                LocalDateTime.now();

        FraudAssessmentDashboardResponse response =
                assessmentService
                        .getAssessmentDashboard();

        LocalDateTime afterCall =
                LocalDateTime.now();

        assertEquals(
                12L,
                response.totalAssessments()
        );

        assertEquals(
                3L,
                response.pendingReview()
        );

        assertEquals(
                1L,
                response.escalated()
        );

        assertEquals(
                4L,
                response.confirmedFraud()
        );

        assertEquals(
                2L,
                response.falsePositive()
        );

        assertEquals(
                2L,
                response.autoCleared()
        );

        assertEquals(
                2L,
                response.criticalRisk()
        );

        assertEquals(
                6L,
                response.highRisk()
        );

        assertEquals(
                4L,
                response.openHighOrCriticalRisk()
        );

        assertEquals(
                oldestPendingAssessedAt,
                response.oldestPendingAssessedAt()
        );

        assertEquals(
                27L,
                response.oldestPendingAgeHours()
        );

        assertEquals(
                latestAssessedAt,
                response.latestAssessedAt()
        );

        assertNotNull(
                response.generatedAt()
        );

        assertTrue(
                !response.generatedAt()
                        .isBefore(beforeCall)
        );

        assertTrue(
                !response.generatedAt()
                        .isAfter(afterCall)
        );

        verify(
                assessmentRepository
        ).getDashboardSummary();
    }

    @Test
    void getAssessmentDashboard_shouldReturnEmptyDashboard() {
        when(
                assessmentRepository
                        .getDashboardSummary()
        ).thenReturn(
                dashboardProjection
        );

        when(
                dashboardProjection
                        .getTotalAssessments()
        ).thenReturn(
                0L
        );

        when(
                dashboardProjection
                        .getPendingReview()
        ).thenReturn(
                0L
        );

        when(
                dashboardProjection
                        .getEscalated()
        ).thenReturn(
                0L
        );

        when(
                dashboardProjection
                        .getConfirmedFraud()
        ).thenReturn(
                0L
        );

        when(
                dashboardProjection
                        .getFalsePositive()
        ).thenReturn(
                0L
        );

        when(
                dashboardProjection
                        .getAutoCleared()
        ).thenReturn(
                0L
        );

        when(
                dashboardProjection
                        .getCriticalRisk()
        ).thenReturn(
                0L
        );

        when(
                dashboardProjection
                        .getHighRisk()
        ).thenReturn(
                0L
        );

        when(
                dashboardProjection
                        .getOpenHighOrCriticalRisk()
        ).thenReturn(
                0L
        );

        when(
                dashboardProjection
                        .getOldestPendingAssessedAt()
        ).thenReturn(
                null
        );

        when(
                dashboardProjection
                        .getLatestAssessedAt()
        ).thenReturn(
                null
        );

        FraudAssessmentDashboardResponse response =
                assessmentService
                        .getAssessmentDashboard();

        assertEquals(
                0L,
                response.totalAssessments()
        );

        assertEquals(
                0L,
                response.pendingReview()
        );

        assertEquals(
                0L,
                response.escalated()
        );

        assertEquals(
                0L,
                response.confirmedFraud()
        );

        assertEquals(
                0L,
                response.falsePositive()
        );

        assertEquals(
                0L,
                response.autoCleared()
        );

        assertEquals(
                0L,
                response.criticalRisk()
        );

        assertEquals(
                0L,
                response.highRisk()
        );

        assertEquals(
                0L,
                response.openHighOrCriticalRisk()
        );

        assertNull(
                response.oldestPendingAssessedAt()
        );

        assertNull(
                response.oldestPendingAgeHours()
        );

        assertNull(
                response.latestAssessedAt()
        );

        assertNotNull(
                response.generatedAt()
        );
    }

    @Test
    void getAssessmentDashboard_shouldNormalizeNullCounts() {
        when(
                assessmentRepository
                        .getDashboardSummary()
        ).thenReturn(
                dashboardProjection
        );

        /*
         * Mockito returns null for every unstubbed
         * projection getter. The service must safely
         * normalize all nullable counts to zero.
         */
        FraudAssessmentDashboardResponse response =
                assessmentService
                        .getAssessmentDashboard();

        assertEquals(
                0L,
                response.totalAssessments()
        );

        assertEquals(
                0L,
                response.pendingReview()
        );

        assertEquals(
                0L,
                response.escalated()
        );

        assertEquals(
                0L,
                response.confirmedFraud()
        );

        assertEquals(
                0L,
                response.falsePositive()
        );

        assertEquals(
                0L,
                response.autoCleared()
        );

        assertEquals(
                0L,
                response.criticalRisk()
        );

        assertEquals(
                0L,
                response.highRisk()
        );

        assertEquals(
                0L,
                response.openHighOrCriticalRisk()
        );

        assertNull(
                response.oldestPendingAssessedAt()
        );

        assertNull(
                response.oldestPendingAgeHours()
        );

        assertNull(
                response.latestAssessedAt()
        );
    }

    @Test
    void getAssessmentDashboard_shouldHandleNullProjection() {
        when(
                assessmentRepository
                        .getDashboardSummary()
        ).thenReturn(
                null
        );

        FraudAssessmentDashboardResponse response =
                assessmentService
                        .getAssessmentDashboard();

        assertEquals(
                0L,
                response.totalAssessments()
        );

        assertEquals(
                0L,
                response.pendingReview()
        );

        assertEquals(
                0L,
                response.escalated()
        );

        assertEquals(
                0L,
                response.confirmedFraud()
        );

        assertEquals(
                0L,
                response.falsePositive()
        );

        assertEquals(
                0L,
                response.autoCleared()
        );

        assertEquals(
                0L,
                response.criticalRisk()
        );

        assertEquals(
                0L,
                response.highRisk()
        );

        assertEquals(
                0L,
                response.openHighOrCriticalRisk()
        );

        assertNull(
                response.oldestPendingAssessedAt()
        );

        assertNull(
                response.oldestPendingAgeHours()
        );

        assertNull(
                response.latestAssessedAt()
        );

        assertNotNull(
                response.generatedAt()
        );
    }

    @Test
    void getAssessmentDashboard_shouldPreventNegativePendingAge() {
        LocalDateTime futurePendingAssessment =
                LocalDateTime.now()
                        .plusHours(3);

        when(
                assessmentRepository
                        .getDashboardSummary()
        ).thenReturn(
                dashboardProjection
        );

        when(
                dashboardProjection
                        .getOldestPendingAssessedAt()
        ).thenReturn(
                futurePendingAssessment
        );

        FraudAssessmentDashboardResponse response =
                assessmentService
                        .getAssessmentDashboard();

        assertEquals(
                futurePendingAssessment,
                response.oldestPendingAssessedAt()
        );

        assertEquals(
                0L,
                response.oldestPendingAgeHours()
        );
    }

    @Test
    void dashboardResponse_shouldRejectNegativeCount() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new FraudAssessmentDashboardResponse(
                                        -1L,
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        null,
                                        null,
                                        null,
                                        LocalDateTime.now()
                                )
                );

        assertEquals(
                "Total assessments cannot be negative.",
                exception.getMessage()
        );
    }

    @Test
    void dashboardResponse_shouldRequireGenerationTime() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new FraudAssessmentDashboardResponse(
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        0L,
                                        null,
                                        null,
                                        null,
                                        null
                                )
                );

        assertEquals(
                "Dashboard generation time is required.",
                exception.getMessage()
        );
    }
}