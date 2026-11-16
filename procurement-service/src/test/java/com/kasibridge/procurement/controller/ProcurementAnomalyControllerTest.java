package com.kasibridge.procurement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.procurement.dto.ProcurementAnomalyRecordResponse;
import com.kasibridge.procurement.dto.ReviewProcurementAnomalyRequest;
import com.kasibridge.procurement.entity.ProcurementAnomaly;
import com.kasibridge.procurement.exception.AnomalyStateException;
import com.kasibridge.procurement.exception.GlobalExceptionHandler;
import com.kasibridge.procurement.exception.ProcurementAuthorizationException;
import com.kasibridge.procurement.service.ProcurementAnomalyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProcurementAnomalyController.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.security.servlet."
                        + "SecurityAutoConfiguration"
        }
)
@Import(GlobalExceptionHandler.class)
class ProcurementAnomalyControllerTest {

    private static final Long ANOMALY_ID = 91L;
    private static final Long TENDER_ID = 11L;
    private static final Long BID_ID = 101L;
    private static final Long ADJUDICATOR_USER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProcurementAnomalyService anomalyService;

    @Test
    void shouldMarkAnomalyReviewed() throws Exception {
        ReviewProcurementAnomalyRequest request =
                reviewRequest(
                        "Evidence confirmed against "
                                + "the immutable score record."
                );

        ProcurementAnomalyRecordResponse response =
                anomalyResponse(
                        ProcurementAnomaly
                                .AnomalyStatus
                                .REVIEWED,
                        request.getReviewNotes()
                );

        when(
                anomalyService.markReviewed(
                        eq(ANOMALY_ID),
                        argThat(
                                supplied ->
                                        supplied != null
                                                && request
                                                .getReviewNotes()
                                                .equals(
                                                        supplied
                                                                .getReviewNotes()
                                                )
                        )
                )
        ).thenReturn(response);

        mockMvc.perform(
                        patch(
                                "/api/v1/procurement/anomalies/"
                                        + "{anomalyId}/review",
                                ANOMALY_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(91)
                )
                .andExpect(
                        jsonPath("$.anomalyReference")
                                .value(
                                        "KB-ANOMALY-00000091"
                                )
                )
                .andExpect(
                        jsonPath("$.tenderId")
                                .value(11)
                )
                .andExpect(
                        jsonPath("$.bidId")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.severity")
                                .value("HIGH")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("REVIEWED")
                )
                .andExpect(
                        jsonPath("$.reviewedByUserId")
                                .value(42)
                )
                .andExpect(
                        jsonPath("$.reviewNotes")
                                .value(
                                        request.getReviewNotes()
                                )
                )
                .andExpect(
                        jsonPath("$.reviewedAt")
                                .exists()
                );

        verify(anomalyService)
                .markReviewed(
                        eq(ANOMALY_ID),
                        any(
                                ReviewProcurementAnomalyRequest.class
                        )
                );
    }

    @Test
    void shouldDismissAnomaly() throws Exception {
        ReviewProcurementAnomalyRequest request =
                reviewRequest(
                        "Evidence supports the score "
                                + "difference and no "
                                + "improper conduct was found."
                );

        ProcurementAnomalyRecordResponse response =
                anomalyResponse(
                        ProcurementAnomaly
                                .AnomalyStatus
                                .DISMISSED,
                        request.getReviewNotes()
                );

        when(
                anomalyService.dismiss(
                        eq(ANOMALY_ID),
                        any(
                                ReviewProcurementAnomalyRequest.class
                        )
                )
        ).thenReturn(response);

        mockMvc.perform(
                        patch(
                                "/api/v1/procurement/anomalies/"
                                        + "{anomalyId}/dismiss",
                                ANOMALY_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("DISMISSED")
                )
                .andExpect(
                        jsonPath("$.reviewNotes")
                                .value(
                                        request.getReviewNotes()
                                )
                );
    }

    @Test
    void shouldReturnBadRequestForBlankReviewNotes()
            throws Exception {

        ReviewProcurementAnomalyRequest request =
                reviewRequest("   ");

        mockMvc.perform(
                        patch(
                                "/api/v1/procurement/anomalies/"
                                        + "{anomalyId}/review",
                                ANOMALY_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Validation failed")
                )
                .andExpect(
                        jsonPath(
                                "$.fieldErrors.reviewNotes"
                        ).value(
                                "Review notes are required"
                        )
                );

        verify(
                anomalyService,
                never()
        ).markReviewed(
                eq(ANOMALY_ID),
                any(
                        ReviewProcurementAnomalyRequest.class
                )
        );
    }

    @Test
    void shouldReturnConflictWhenAnomalyIsNotOpen()
            throws Exception {

        ReviewProcurementAnomalyRequest request =
                reviewRequest(
                        "Attempted repeated review."
                );

        when(
                anomalyService.dismiss(
                        eq(ANOMALY_ID),
                        any(
                                ReviewProcurementAnomalyRequest.class
                        )
                )
        ).thenThrow(
                new AnomalyStateException(
                        "Only OPEN anomalies can be "
                                + "reviewed or dismissed."
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/v1/procurement/anomalies/"
                                        + "{anomalyId}/dismiss",
                                ANOMALY_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Only OPEN anomalies "
                                                + "can be reviewed "
                                                + "or dismissed."
                                )
                );
    }

    @Test
    void shouldReturnForbiddenForUnassignedAdjudicator()
            throws Exception {

        ReviewProcurementAnomalyRequest request =
                reviewRequest(
                        "Attempted review without "
                                + "a tender assignment."
                );

        when(
                anomalyService.markReviewed(
                        eq(ANOMALY_ID),
                        any(
                                ReviewProcurementAnomalyRequest.class
                        )
                )
        ).thenThrow(
                new ProcurementAuthorizationException(
                        "User is not assigned as "
                                + "adjudicator to this "
                                + "anomaly's tender."
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/v1/procurement/anomalies/"
                                        + "{anomalyId}/review",
                                ANOMALY_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "User is not assigned "
                                                + "as adjudicator "
                                                + "to this anomaly's "
                                                + "tender."
                                )
                );
    }

    private ReviewProcurementAnomalyRequest reviewRequest(
            String reviewNotes
    ) {
        ReviewProcurementAnomalyRequest request =
                new ReviewProcurementAnomalyRequest();

        request.setReviewNotes(
                reviewNotes
        );

        return request;
    }

    private ProcurementAnomalyRecordResponse anomalyResponse(
            ProcurementAnomaly.AnomalyStatus status,
            String reviewNotes
    ) {
        return ProcurementAnomalyRecordResponse
                .builder()
                .id(ANOMALY_ID)
                .anomalyReference(
                        "KB-ANOMALY-00000091"
                )
                .tenderId(TENDER_ID)
                .bidId(BID_ID)
                .type(
                        ProcurementAnomaly
                                .AnomalyType
                                .LARGE_SCORE_GAP
                )
                .severity(
                        ProcurementAnomaly
                                .Severity
                                .HIGH
                )
                .status(status)
                .message(
                        "A large score gap requires "
                                + "adjudicator review."
                )
                .evidence(
                        "Highest score=92.00, "
                                + "next score=58.00"
                )
                .detectedAt(
                        LocalDateTime.of(
                                2026,
                                11,
                                16,
                                18,
                                0
                        )
                )
                .reviewedByUserId(
                        ADJUDICATOR_USER_ID
                )
                .reviewNotes(reviewNotes)
                .reviewedAt(
                        LocalDateTime.of(
                                2026,
                                11,
                                16,
                                20,
                                0
                        )
                )
                .build();
    }
}