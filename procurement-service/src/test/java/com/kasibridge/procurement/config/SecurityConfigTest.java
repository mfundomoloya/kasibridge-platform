package com.kasibridge.procurement.config;

import com.kasibridge.procurement.controller.TenderController;
import com.kasibridge.procurement.dto.TenderResponse;
import com.kasibridge.procurement.entity.Tender;
import com.kasibridge.procurement.service.TenderService;
import com.kasibridge.security.JwtAuthenticationFilter;
import com.kasibridge.procurement.controller.AdjudicationController;
import com.kasibridge.procurement.service.AdjudicationService;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.kasibridge.procurement.dto.AwardTenderRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TenderController.class,
        AdjudicationController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final Long TENDER_ID = 11L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenderService tenderService;

    @MockitoBean
    private AdjudicationService adjudicationService;

    /*
     * SecurityConfig requires the shared JWT filter.
     *
     * The mock is configured as a pass-through filter so that requests
     * continue into Spring Security's authorization filters and the
     * TenderController. Authentication is supplied by @WithMockUser.
     */
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void allowRequestsThroughMockJwtFilter() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain filterChain = invocation.getArgument(2);

            filterChain.doFilter(request, response);

            return null;
        })
                .when(jwtAuthenticationFilter)
                .doFilter(
                        any(ServletRequest.class),
                        any(ServletResponse.class),
                        any(FilterChain.class)
                );
    }

    @Test
    @WithMockUser(
            username = "adjudicator@kasibridge.test",
            roles = "ADJUDICATOR"
    )
    void adjudicatorShouldStartAdjudication() throws Exception {
        TenderResponse response =
                TenderResponse.builder()
                        .id(TENDER_ID)
                        .tenderReference("KB-TENDER-00000011")
                        .title("Supply of safety equipment")
                        .status(Tender.TenderStatus.ADJUDICATION)
                        .build();

        when(tenderService.startAdjudication(TENDER_ID))
                .thenReturn(response);

        mockMvc.perform(
                        patch(
                                "/api/v1/tenders/{id}/start-adjudication",
                                TENDER_ID
                        )
                )
                .andExpect(status().isOk());

        verify(tenderService).startAdjudication(TENDER_ID);
    }

    @Test
    @WithMockUser(
            username = "evaluator@kasibridge.test",
            roles = "EVALUATOR"
    )
    void evaluatorShouldNotStartAdjudication() throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/v1/tenders/{id}/start-adjudication",
                                TENDER_ID
                        )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(tenderService);
    }

    @Test
    @WithMockUser(
            username = "specification@kasibridge.test",
            roles = "SPECIFICATION_OFFICER"
    )
    void specificationOfficerShouldNotStartAdjudication()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/v1/tenders/{id}/start-adjudication",
                                TENDER_ID
                        )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(tenderService);
    }

    @Test
    @WithMockUser(
            username = "admin@kasibridge.test",
            roles = "PLATFORM_ADMIN"
    )
    void platformAdminShouldNotStartAdjudication() throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/v1/tenders/{id}/start-adjudication",
                                TENDER_ID
                        )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(tenderService);
    }

    @Test
    void unauthenticatedUserShouldNotStartAdjudication()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/v1/tenders/{id}/start-adjudication",
                                TENDER_ID
                        )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(tenderService);
    }


    @Test
    @WithMockUser(
            username = "adjudicator@kasibridge.test",
            roles = "ADJUDICATOR"
    )
    void adjudicatorShouldAccessAwardEndpoint() throws Exception {
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
                                        """
                                        {
                                          "winningBidId": 101,
                                          "awardReason":
                                            "Highest evaluated bid based on the published criteria."
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated());

        verify(adjudicationService)
                .awardTender(
                        eq(TENDER_ID),
                        any(AwardTenderRequest.class)
                );
    }

    @Test
    @WithMockUser(
            username = "evaluator@kasibridge.test",
            roles = "EVALUATOR"
    )
    void evaluatorShouldNotAccessAwardEndpoint() throws Exception {
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
                                        """
                                        {
                                          "winningBidId": 101,
                                          "awardReason":
                                            "Highest evaluated bid based on the published criteria."
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adjudicationService);
    }

    @Test
    @WithMockUser(
            username = "admin@kasibridge.test",
            roles = "PLATFORM_ADMIN"
    )
    void platformAdminShouldNotAccessAwardEndpoint() throws Exception {
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
                                        """
                                        {
                                          "winningBidId": 101,
                                          "awardReason":
                                            "Highest evaluated bid based on the published criteria."
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adjudicationService);
    }
}