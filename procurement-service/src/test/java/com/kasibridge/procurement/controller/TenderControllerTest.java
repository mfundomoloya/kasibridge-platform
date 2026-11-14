package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.dto.TenderResponse;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.service.TenderService;
import com.kasibridge.procurement.exception.GlobalExceptionHandler;
import com.kasibridge.procurement.exception.TenderStateException;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TenderController.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.security.servlet."
                        + "SecurityAutoConfiguration"
        }
)
class TenderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenderService tenderService;

    @Test
    void shouldStartAdjudication() throws Exception {
        Long tenderId = 11L;

        TenderResponse response =
                TenderResponse.builder()
                        .id(tenderId)
                        .tenderReference("KB-TENDER-00000011")
                        .title("Supply of safety equipment")
                        .status(Tender.TenderStatus.ADJUDICATION)
                        .build();

        when(tenderService.startAdjudication(tenderId))
                .thenReturn(response);

        mockMvc.perform(
                        patch(
                                "/api/v1/tenders/{id}/start-adjudication",
                                tenderId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.tenderReference")
                        .value("KB-TENDER-00000011"))
                .andExpect(jsonPath("$.title")
                        .value("Supply of safety equipment"))
                .andExpect(jsonPath("$.status")
                        .value("ADJUDICATION"));

        verify(tenderService).startAdjudication(tenderId);
    }

    @Test
    void shouldReturnBadRequestWhenEvaluationIsIncomplete()
            throws Exception {

        Long tenderId = 11L;

        String blockingReason =
                "One evaluation score is still outstanding.";

        when(tenderService.startAdjudication(tenderId))
                .thenThrow(
                        new TenderStateException(blockingReason)
                );

        mockMvc.perform(
                        patch(
                                "/api/v1/tenders/{id}/start-adjudication",
                                tenderId
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(blockingReason))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors")
                        .doesNotExist());

        verify(tenderService).startAdjudication(tenderId);
    }
}