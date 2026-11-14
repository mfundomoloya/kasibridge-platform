package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.EvaluationProgressResponse;
import com.kasibridge.procurement.service.BidService;
import com.kasibridge.procurement.service.EvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BidController.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.security.servlet."
                        + "SecurityAutoConfiguration"
        }
)
class BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluationService evaluationService;

    @MockitoBean
    private BidService bidService;

    @Test
    void shouldReturnEvaluationProgress() throws Exception {
        Long tenderId = 11L;

        EvaluationProgressResponse response =
                EvaluationProgressResponse.builder()
                        .tenderId(tenderId)
                        .tenderReference("KASI-2026-001")
                        .tenderTitle("Supply of safety equipment")
                        .activeEvaluatorCount(1L)
                        .eligibleBidCount(1L)
                        .expectedScoreCount(1L)
                        .submittedScoreCount(1L)
                        .remainingScoreCount(0L)
                        .completionPercentage(new BigDecimal("100.00"))
                        .evaluationComplete(true)
                        .readyForAdjudication(true)
                        .blockingReason(null)
                        .build();

        when(evaluationService.getEvaluationProgress(tenderId))
                .thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/v1/tenders/{tenderId}"
                                        + "/bids/evaluation/progress",
                                tenderId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenderId").value(11))
                .andExpect(jsonPath("$.tenderReference").value("KASI-2026-001"))
                .andExpect(jsonPath("$.tenderTitle").value("Supply of safety equipment"))
                .andExpect(jsonPath("$.activeEvaluatorCount").value(1))
                .andExpect(jsonPath("$.eligibleBidCount").value(1))
                .andExpect(jsonPath("$.expectedScoreCount").value(1))
                .andExpect(jsonPath("$.submittedScoreCount").value(1))
                .andExpect(jsonPath("$.remainingScoreCount").value(0))
                .andExpect(jsonPath("$.completionPercentage").value(100.00))
                .andExpect(jsonPath("$.evaluationComplete").value(true))
                .andExpect(jsonPath("$.readyForAdjudication").value(true))
                .andExpect(jsonPath("$.blockingReason").doesNotExist());

        verify(evaluationService).getEvaluationProgress(tenderId);
    }
}