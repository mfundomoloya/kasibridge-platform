package com.kasibridge.procurement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasibridge.procurement.dto.AwardTenderRequest;
import com.kasibridge.procurement.dto.AwardTenderResponse;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.exception.AdjudicationException;
import com.kasibridge.procurement.exception.GlobalExceptionHandler;
import com.kasibridge.procurement.exception.ProcurementAuthorizationException;
import com.kasibridge.procurement.service.AdjudicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdjudicationController.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.security.servlet."
                        + "SecurityAutoConfiguration"
        }
)
@Import(GlobalExceptionHandler.class)
class AdjudicationControllerTest {

    private static final Long TENDER_ID = 11L;
    private static final Long WINNING_BID_ID = 101L;
    private static final Long ADJUDICATOR_USER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdjudicationService adjudicationService;

    @Test
    void shouldAwardTender() throws Exception {
        AwardTenderRequest request =
                validAwardRequest();

        LocalDateTime awardedAt =
                LocalDateTime.of(
                        2026,
                        11,
                        15,
                        20,
                        0
                );

        AwardTenderResponse response =
                AwardTenderResponse.builder()
                        .tenderId(TENDER_ID)
                        .tenderReference(
                                "KB-TENDER-00000011"
                        )
                        .tenderStatus(
                                Tender.TenderStatus.AWARDED
                        )
                        .winningBidId(
                                WINNING_BID_ID
                        )
                        .winningBidReference(
                                "KB-BID-00000101"
                        )
                        .winningBidderAlias(
                                "Bidder Alpha"
                        )
                        .winningBidStatus(
                                Bid.BidStatus.AWARDED
                        )
                        .adjudicatorUserId(
                                ADJUDICATOR_USER_ID
                        )
                        .awardReason(
                                request.getAwardReason()
                        )
                        .awardedAt(awardedAt)
                        .build();

        when(
                adjudicationService.awardTender(
                        eq(TENDER_ID),
                        argThat(
                                suppliedRequest ->
                                        suppliedRequest != null
                                                && WINNING_BID_ID.equals(
                                                suppliedRequest
                                                        .getWinningBidId()
                                        )
                                                && request
                                                .getAwardReason()
                                                .equals(
                                                        suppliedRequest
                                                                .getAwardReason()
                                                )
                        )
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post(
                                "/api/v1/tenders/{tenderId}"
                                        + "/adjudication/award",
                                TENDER_ID
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
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.tenderId")
                                .value(11)
                )
                .andExpect(
                        jsonPath("$.tenderReference")
                                .value(
                                        "KB-TENDER-00000011"
                                )
                )
                .andExpect(
                        jsonPath("$.tenderStatus")
                                .value("AWARDED")
                )
                .andExpect(
                        jsonPath("$.winningBidId")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.winningBidReference")
                                .value(
                                        "KB-BID-00000101"
                                )
                )
                .andExpect(
                        jsonPath("$.winningBidderAlias")
                                .value("Bidder Alpha")
                )
                .andExpect(
                        jsonPath("$.winningBidStatus")
                                .value("AWARDED")
                )
                .andExpect(
                        jsonPath("$.adjudicatorUserId")
                                .value(42)
                )
                .andExpect(
                        jsonPath("$.awardReason")
                                .value(
                                        request.getAwardReason()
                                )
                )
                .andExpect(
                        jsonPath("$.awardedAt")
                                .value(
                                        "2026-11-15T20:00:00"
                                )
                );

        verify(adjudicationService)
                .awardTender(
                        eq(TENDER_ID),
                        argThat(
                                suppliedRequest ->
                                        suppliedRequest != null
                                                && WINNING_BID_ID.equals(
                                                suppliedRequest
                                                        .getWinningBidId()
                                        )
                                                && request
                                                .getAwardReason()
                                                .equals(
                                                        suppliedRequest
                                                                .getAwardReason()
                                                )
                        )
                );
    }

    @Test
    void shouldReturnBadRequestForInvalidAwardRequest()
            throws Exception {

        AwardTenderRequest request =
                new AwardTenderRequest();

        request.setAwardReason("");

        mockMvc.perform(
                        post(
                                "/api/v1/tenders/{tenderId}"
                                        + "/adjudication/award",
                                TENDER_ID
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
                                "$.fieldErrors.winningBidId"
                        ).value(
                                "Winning bid ID is required"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.fieldErrors.awardReason"
                        ).value(
                                "Award reason is required"
                        )
                )
                .andExpect(
                        jsonPath("$.timestamp").exists()
                );

        verify(
                adjudicationService,
                never()
        ).awardTender(
                eq(TENDER_ID),
                org.mockito.ArgumentMatchers
                        .any(AwardTenderRequest.class)
        );
    }

    @Test
    void shouldReturnBadRequestWhenAwardIsRejected()
            throws Exception {

        AwardTenderRequest request =
                validAwardRequest();

        String rejectionReason =
                "Tender has already been awarded.";

        when(
                adjudicationService.awardTender(
                        eq(TENDER_ID),
                        org.mockito.ArgumentMatchers
                                .any(AwardTenderRequest.class)
                )
        ).thenThrow(
                new AdjudicationException(
                        rejectionReason
                )
        );

        mockMvc.perform(
                        post(
                                "/api/v1/tenders/{tenderId}"
                                        + "/adjudication/award",
                                TENDER_ID
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
                                .value(rejectionReason)
                )
                .andExpect(
                        jsonPath("$.timestamp").exists()
                );
    }

    @Test
    void shouldReturnForbiddenWhenAdjudicatorIsNotAssigned()
            throws Exception {

        AwardTenderRequest request =
                validAwardRequest();

        String rejectionReason =
                "User is not assigned as "
                        + "adjudicator to this tender.";

        when(
                adjudicationService.awardTender(
                        eq(TENDER_ID),
                        org.mockito.ArgumentMatchers
                                .any(AwardTenderRequest.class)
                )
        ).thenThrow(
                new ProcurementAuthorizationException(
                        rejectionReason
                )
        );

        mockMvc.perform(
                        post(
                                "/api/v1/tenders/{tenderId}"
                                        + "/adjudication/award",
                                TENDER_ID
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
                                .value(rejectionReason)
                )
                .andExpect(
                        jsonPath("$.timestamp").exists()
                );
    }

    private AwardTenderRequest validAwardRequest() {
        AwardTenderRequest request =
                new AwardTenderRequest();

        request.setWinningBidId(
                WINNING_BID_ID
        );

        request.setAwardReason(
                "Highest evaluated bid based "
                        + "on the published criteria."
        );

        return request;
    }
}