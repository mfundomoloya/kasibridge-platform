package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.config.SecurityConfig;
import com.kasibridge.procurement.dto.BidResponse;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.service.BidService;
import com.kasibridge.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TraderBidController.class)
@Import(SecurityConfig.class)
class TraderBidControllerTest {

    private static final Long BID_ID = 15L;
    private static final Long TENDER_ID = 7L;
    private static final Long TRADER_PROFILE_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BidService bidService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUpJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain filterChain =
                    invocation.getArgument(2);

            filterChain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );

            return null;
        })
                .when(jwtAuthenticationFilter)
                .doFilter(
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    @WithMockUser(
            username = "bidder-a",
            roles = "TRADER"
    )
    void traderShouldRetrieveOwnBidHistory()
            throws Exception {

        when(bidService.getCurrentTraderBids())
                .thenReturn(
                        List.of(activeBidResponse())
                );

        mockMvc.perform(
                        get("/api/v1/bids/my")
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(BID_ID)
                )
                .andExpect(
                        jsonPath("$[0].bidReference")
                                .value("KB-BID-623BEF93")
                )
                .andExpect(
                        jsonPath("$[0].tenderId")
                                .value(TENDER_ID)
                )
                .andExpect(
                        jsonPath("$[0].traderId")
                                .value(TRADER_PROFILE_ID)
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("COMPLIANT")
                );

        verify(bidService)
                .getCurrentTraderBids();
    }

    @Test
    @WithMockUser(
            username = "bidder-a",
            roles = "TRADER"
    )
    void traderShouldWithdrawOwnedBid()
            throws Exception {

        when(
                bidService.withdrawCurrentTraderBid(
                        BID_ID
                )
        ).thenReturn(
                withdrawnBidResponse()
        );

        mockMvc.perform(
                        patch(
                                "/api/v1/bids/{bidId}/withdraw",
                                BID_ID
                        )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(BID_ID)
                )
                .andExpect(
                        jsonPath("$.bidReference")
                                .value("KB-BID-623BEF93")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("WITHDRAWN")
                );

        verify(bidService)
                .withdrawCurrentTraderBid(
                        BID_ID
                );
    }

    @Test
    @WithMockUser(
            username = "evaluator-a",
            roles = "EVALUATOR"
    )
    void evaluatorShouldNotRetrieveTraderBidHistory()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/bids/my")
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                bidService,
                never()
        ).getCurrentTraderBids();
    }

    @Test
    @WithMockUser(
            username = "evaluator-a",
            roles = "EVALUATOR"
    )
    void evaluatorShouldNotWithdrawBid()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/v1/bids/{bidId}/withdraw",
                                BID_ID
                        )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                bidService,
                never()
        ).withdrawCurrentTraderBid(
                eq(BID_ID)
        );
    }

    @Test
    void unauthenticatedUserShouldNotRetrieveTraderBidHistory()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/bids/my")
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().is4xxClientError()
                );

        verify(
                bidService,
                never()
        ).getCurrentTraderBids();
    }

    @Test
    void unauthenticatedUserShouldNotWithdrawBid()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/v1/bids/{bidId}/withdraw",
                                BID_ID
                        )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().is4xxClientError()
                );

        verify(
                bidService,
                never()
        ).withdrawCurrentTraderBid(
                eq(BID_ID)
        );
    }

    private BidResponse activeBidResponse() {
        return BidResponse.builder()
                .id(BID_ID)
                .bidReference("KB-BID-623BEF93")
                .tenderId(TENDER_ID)
                .traderId(TRADER_PROFILE_ID)
                .bidderAlias("Bidder A")
                .technicalProposal(
                        "Technical proposal for delivery and commissioning."
                )
                .priceAmount(
                        new BigDecimal("690000.00")
                )
                .status(
                        Bid.BidStatus.COMPLIANT
                )
                .submittedAt(
                        LocalDateTime.of(
                                2026,
                                10,
                                6,
                                23,
                                52
                        )
                )
                .updatedAt(
                        LocalDateTime.of(
                                2026,
                                10,
                                6,
                                23,
                                52
                        )
                )
                .compliance(null)
                .build();
    }

    private BidResponse withdrawnBidResponse() {
        return BidResponse.builder()
                .id(BID_ID)
                .bidReference("KB-BID-623BEF93")
                .tenderId(TENDER_ID)
                .traderId(TRADER_PROFILE_ID)
                .bidderAlias("Bidder A")
                .technicalProposal(
                        "Technical proposal for delivery and commissioning."
                )
                .priceAmount(
                        new BigDecimal("690000.00")
                )
                .status(
                        Bid.BidStatus.WITHDRAWN
                )
                .submittedAt(
                        LocalDateTime.of(
                                2026,
                                10,
                                6,
                                23,
                                52
                        )
                )
                .updatedAt(
                        LocalDateTime.of(
                                2026,
                                10,
                                8,
                                21,
                                32
                        )
                )
                .compliance(null)
                .build();
    }
}