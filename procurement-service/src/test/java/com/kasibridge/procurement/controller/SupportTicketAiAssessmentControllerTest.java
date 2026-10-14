package com.kasibridge.procurement.controller;

import com.kasibridge.procurement.config.SecurityConfig;
import com.kasibridge.procurement.dto.ApproveTicketAiAssessmentRequest;
import com.kasibridge.procurement.dto.SupportTicketAiAssessmentResponse;
import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import com.kasibridge.procurement.exception.TicketAiAssessmentStateException;
import com.kasibridge.procurement.service.SupportTicketAiAssessmentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers =
                SupportTicketAiAssessmentController.class
)
@Import(SecurityConfig.class)
class SupportTicketAiAssessmentControllerTest {

    private static final Long TICKET_ID = 42L;
    private static final Long ASSESSMENT_ID = 71L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupportTicketAiAssessmentService service;

    @MockitoBean
    private JwtAuthenticationFilter
            jwtAuthenticationFilter;

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
            username = "specification-officer",
            roles = "SPECIFICATION_OFFICER"
    )
    void specificationOfficerShouldGenerateAssessment()
            throws Exception {
        when(service.generateAssessment(TICKET_ID))
                .thenReturn(
                        generatedAssessmentResponse()
                );

        mockMvc.perform(
                        post(
                                "/api/v1/tickets/"
                                        + "{ticketId}/ai-assessment",
                                TICKET_ID
                        )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(ASSESSMENT_ID)
                )
                .andExpect(
                        jsonPath("$.ticketId")
                                .value(TICKET_ID)
                )
                .andExpect(
                        jsonPath("$.assessmentStatus")
                                .value("GENERATED")
                )
                .andExpect(
                        jsonPath("$.resolutionMode")
                                .value(
                                        "DRAFT_FOR_APPROVAL"
                                )
                );

        verify(service)
                .generateAssessment(TICKET_ID);
    }

    @Test
    @WithMockUser(
            username = "platform-admin",
            roles = "PLATFORM_ADMIN"
    )
    void platformAdminShouldApproveAssessment()
            throws Exception {
        when(service.approveAssessment(
                eq(ASSESSMENT_ID),
                any(
                        ApproveTicketAiAssessmentRequest
                                .class
                )
        )).thenReturn(
                approvedAssessmentResponse()
        );

        mockMvc.perform(
                        patch(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/approve",
                                ASSESSMENT_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "approvedResponse":
                                            "Phased delivery is permitted.",
                                          "responseRelevanceConfirmed":
                                            true,
                                          "responseRelevanceConfirmationNotes":
                                            "The response directly addresses the delivery question."
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(ASSESSMENT_ID)
                )
                .andExpect(
                        jsonPath("$.assessmentStatus")
                                .value(
                                        "EDITED_AND_APPROVED"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.responseRelevanceConfirmed"
                        ).value(true)
                );

        verify(service)
                .approveAssessment(
                        eq(ASSESSMENT_ID),
                        argThat(request ->
                                Boolean.TRUE.equals(
                                        request
                                                .getResponseRelevanceConfirmed()
                                )
                                        && "Phased delivery is permitted."
                                        .equals(
                                                request
                                                        .getApprovedResponse()
                                        )
                                        && (
                                        "The response directly "
                                                + "addresses the "
                                                + "delivery question."
                                ).equals(
                                        request
                                                .getResponseRelevanceConfirmationNotes()
                                )
                        )
                );
    }

    @Test
    @WithMockUser(
            username = "specification-officer",
            roles = "SPECIFICATION_OFFICER"
    )
    void specificationOfficerShouldPublishApprovedResponse()
            throws Exception {
        when(service.publishApprovedResponse(
                ASSESSMENT_ID
        )).thenReturn(
                publishedAssessmentResponse()
        );

        mockMvc.perform(
                        post(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/"
                                        + "publish-response",
                                ASSESSMENT_ID
                        )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(ASSESSMENT_ID)
                )
                .andExpect(
                        jsonPath("$.assessmentStatus")
                                .value(
                                        "RESPONSE_PUBLISHED"
                                )
                )
                .andExpect(
                        jsonPath("$.publishedByUserId")
                                .value(73L)
                );

        verify(service)
                .publishApprovedResponse(
                        ASSESSMENT_ID
                );
    }

    @Test
    @WithMockUser(
            username = "trader",
            roles = "TRADER"
    )
    void traderShouldNotApproveAssessment()
            throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/approve",
                                ASSESSMENT_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(validApprovalJson())
                )
                .andExpect(status().isForbidden());

        verify(
                service,
                never()
        ).approveAssessment(
                eq(ASSESSMENT_ID),
                any(
                        ApproveTicketAiAssessmentRequest
                                .class
                )
        );
    }

    @Test
    @WithMockUser(
            username = "trader",
            roles = "TRADER"
    )
    void traderShouldNotPublishAssessment()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/"
                                        + "publish-response",
                                ASSESSMENT_ID
                        )
                )
                .andExpect(status().isForbidden());

        verify(
                service,
                never()
        ).publishApprovedResponse(
                ASSESSMENT_ID
        );
    }

    @Test
    @WithMockUser(
            username = "adjudicator",
            roles = "ADJUDICATOR"
    )
    void adjudicatorShouldNotGenerateAssessment()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/tickets/"
                                        + "{ticketId}/ai-assessment",
                                TICKET_ID
                        )
                )
                .andExpect(status().isForbidden());

        verify(
                service,
                never()
        ).generateAssessment(TICKET_ID);
    }

    @Test
    @WithMockUser(
            username = "adjudicator",
            roles = "ADJUDICATOR"
    )
    void adjudicatorShouldNotPublishAssessment()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/"
                                        + "publish-response",
                                ASSESSMENT_ID
                        )
                )
                .andExpect(status().isForbidden());

        verify(
                service,
                never()
        ).publishApprovedResponse(
                ASSESSMENT_ID
        );
    }

    @Test
    void unauthenticatedUserShouldNotGenerateAssessment()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/tickets/"
                                        + "{ticketId}/ai-assessment",
                                TICKET_ID
                        )
                )
                .andExpect(
                        status().is4xxClientError()
                );

        verify(
                service,
                never()
        ).generateAssessment(TICKET_ID);
    }

    @Test
    void unauthenticatedUserShouldNotPublishAssessment()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/"
                                        + "publish-response",
                                ASSESSMENT_ID
                        )
                )
                .andExpect(
                        status().is4xxClientError()
                );

        verify(
                service,
                never()
        ).publishApprovedResponse(
                ASSESSMENT_ID
        );
    }

    @Test
    @WithMockUser(
            username = "specification-officer",
            roles = "SPECIFICATION_OFFICER"
    )
    void approvalShouldRejectMissingRelevanceConfirmation()
            throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/approve",
                                ASSESSMENT_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "approvedResponse":
                                            "Phased delivery is permitted.",
                                          "responseRelevanceConfirmationNotes":
                                            "The response was reviewed."
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Validation failed"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.fieldErrors."
                                        + "responseRelevanceConfirmed"
                        ).value(
                                "Response relevance "
                                        + "confirmation is required"
                        )
                );

        verify(
                service,
                never()
        ).approveAssessment(
                eq(ASSESSMENT_ID),
                any(
                        ApproveTicketAiAssessmentRequest
                                .class
                )
        );
    }

    @Test
    @WithMockUser(
            username = "specification-officer",
            roles = "SPECIFICATION_OFFICER"
    )
    void approvalShouldRejectBlankConfirmationNotes()
            throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/approve",
                                ASSESSMENT_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "approvedResponse":
                                            "Phased delivery is permitted.",
                                          "responseRelevanceConfirmed":
                                            true,
                                          "responseRelevanceConfirmationNotes":
                                            "   "
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.fieldErrors."
                                        + "responseRelevanceConfirmationNotes"
                        ).value(
                                "Response relevance "
                                        + "confirmation notes "
                                        + "are required"
                        )
                );

        verify(
                service,
                never()
        ).approveAssessment(
                eq(ASSESSMENT_ID),
                any(
                        ApproveTicketAiAssessmentRequest
                                .class
                )
        );
    }

    @Test
    @WithMockUser(
            username = "specification-officer",
            roles = "SPECIFICATION_OFFICER"
    )
    void approvalShouldRejectResponseOverMaximumLength()
            throws Exception {
        String oversizedResponse =
                "a".repeat(4001);

        String requestBody =
                """
                {
                  "approvedResponse": "%s",
                  "responseRelevanceConfirmed": true,
                  "responseRelevanceConfirmationNotes":
                    "The response is relevant."
                }
                """.formatted(oversizedResponse);

        mockMvc.perform(
                        patch(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/approve",
                                ASSESSMENT_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.fieldErrors.approvedResponse"
                        ).value(
                                "Approved response cannot "
                                        + "exceed 4000 characters"
                        )
                );

        verify(
                service,
                never()
        ).approveAssessment(
                eq(ASSESSMENT_ID),
                any(
                        ApproveTicketAiAssessmentRequest
                                .class
                )
        );
    }

    @Test
    @WithMockUser(
            username = "specification-officer",
            roles = "SPECIFICATION_OFFICER"
    )
    void rejectedAssessmentShouldRequireReason()
            throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/reject",
                                ASSESSMENT_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "rejectionReason": " "
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.fieldErrors.rejectionReason"
                        ).value(
                                "Rejection reason is required"
                        )
                );

        verify(
                service,
                never()
        ).rejectAssessment(
                eq(ASSESSMENT_ID),
                any()
        );
    }

    @Test
    @WithMockUser(
            username = "platform-admin",
            roles = "PLATFORM_ADMIN"
    )
    void escalatedAssessmentShouldRequireReason()
            throws Exception {
        mockMvc.perform(
                        patch(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/escalate",
                                ASSESSMENT_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "escalationReason": ""
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.fieldErrors.escalationReason"
                        ).value(
                                "Escalation reason is required"
                        )
                );

        verify(
                service,
                never()
        ).escalateAssessment(
                eq(ASSESSMENT_ID),
                any()
        );
    }

    @Test
    @WithMockUser(
            username = "specification-officer",
            roles = "SPECIFICATION_OFFICER"
    )
    void publicationStateFailureShouldReturnConflict()
            throws Exception {
        when(service.publishApprovedResponse(
                ASSESSMENT_ID
        )).thenThrow(
                new TicketAiAssessmentStateException(
                        "The user who approved the "
                                + "clarification response "
                                + "cannot publish the same "
                                + "response."
                )
        );

        mockMvc.perform(
                        post(
                                "/api/v1/"
                                        + "ticket-ai-assessments/"
                                        + "{assessmentId}/"
                                        + "publish-response",
                                ASSESSMENT_ID
                        )
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "The user who approved "
                                                + "the clarification "
                                                + "response cannot "
                                                + "publish the same "
                                                + "response."
                                )
                );

        verify(service)
                .publishApprovedResponse(
                        ASSESSMENT_ID
                );
    }

    private String validApprovalJson() {
        return """
                {
                  "approvedResponse":
                    "Phased delivery is permitted.",
                  "responseRelevanceConfirmed": true,
                  "responseRelevanceConfirmationNotes":
                    "The response directly addresses the delivery question."
                }
                """;
    }

    private SupportTicketAiAssessmentResponse
    generatedAssessmentResponse() {
        LocalDateTime now =
                LocalDateTime.now();

        return SupportTicketAiAssessmentResponse
                .builder()
                .id(ASSESSMENT_ID)
                .assessmentReference(
                        "KB-AI-ASSESSMENT-000071"
                )
                .ticketId(TICKET_ID)
                .detectedCategory(
                        SupportTicketAiAssessment
                                .DetectedCategory
                                .CLARIFICATION_REQUEST
                )
                .priority(
                        SupportTicketAiAssessment
                                .TicketPriority
                                .MEDIUM
                )
                .resolutionMode(
                        SupportTicketAiAssessment
                                .AiResolutionMode
                                .DRAFT_FOR_APPROVAL
                )
                .assessmentStatus(
                        SupportTicketAiAssessment
                                .AiAssessmentStatus
                                .GENERATED
                )
                .summary(
                        "The trader requests clarification "
                                + "on phased delivery."
                )
                .suggestedResponse(
                        "Phased delivery is permitted."
                )
                .confidenceScore(
                        new BigDecimal("0.94")
                )
                .modelVersion("controller-test-model")
                .responseRelevanceConfirmed(false)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
    }

    private SupportTicketAiAssessmentResponse
    approvedAssessmentResponse() {
        LocalDateTime now =
                LocalDateTime.now();

        return SupportTicketAiAssessmentResponse
                .builder()
                .id(ASSESSMENT_ID)
                .assessmentReference(
                        "KB-AI-ASSESSMENT-000071"
                )
                .ticketId(TICKET_ID)
                .detectedCategory(
                        SupportTicketAiAssessment
                                .DetectedCategory
                                .CLARIFICATION_REQUEST
                )
                .priority(
                        SupportTicketAiAssessment
                                .TicketPriority
                                .MEDIUM
                )
                .resolutionMode(
                        SupportTicketAiAssessment
                                .AiResolutionMode
                                .DRAFT_FOR_APPROVAL
                )
                .assessmentStatus(
                        SupportTicketAiAssessment
                                .AiAssessmentStatus
                                .EDITED_AND_APPROVED
                )
                .summary(
                        "The trader requests clarification "
                                + "on phased delivery."
                )
                .suggestedResponse(
                        "Phased delivery is permitted."
                )
                .approvedResponse(
                        "Phased delivery is permitted."
                )
                .approvedByUserId(51L)
                .approvedAt(now)
                .responseRelevanceConfirmed(true)
                .responseRelevanceConfirmedByUserId(
                        51L
                )
                .responseRelevanceConfirmedAt(now)
                .responseRelevanceConfirmationNotes(
                        "The response directly addresses "
                                + "the delivery question."
                )
                .confidenceScore(
                        new BigDecimal("0.94")
                )
                .modelVersion("controller-test-model")
                .createdAt(
                        now.minusMinutes(5)
                )
                .updatedAt(now)
                .version(1L)
                .build();
    }

    private SupportTicketAiAssessmentResponse
    publishedAssessmentResponse() {
        LocalDateTime now =
                LocalDateTime.now();

        return SupportTicketAiAssessmentResponse
                .builder()
                .id(ASSESSMENT_ID)
                .assessmentReference(
                        "KB-AI-ASSESSMENT-000071"
                )
                .ticketId(TICKET_ID)
                .detectedCategory(
                        SupportTicketAiAssessment
                                .DetectedCategory
                                .CLARIFICATION_REQUEST
                )
                .priority(
                        SupportTicketAiAssessment
                                .TicketPriority
                                .MEDIUM
                )
                .resolutionMode(
                        SupportTicketAiAssessment
                                .AiResolutionMode
                                .DRAFT_FOR_APPROVAL
                )
                .assessmentStatus(
                        SupportTicketAiAssessment
                                .AiAssessmentStatus
                                .RESPONSE_PUBLISHED
                )
                .summary(
                        "The trader requests clarification "
                                + "on phased delivery."
                )
                .suggestedResponse(
                        "Phased delivery is permitted."
                )
                .approvedResponse(
                        "Phased delivery is permitted."
                )
                .approvedByUserId(51L)
                .approvedAt(
                        now.minusMinutes(5)
                )
                .responseRelevanceConfirmed(true)
                .responseRelevanceConfirmedByUserId(
                        51L
                )
                .responseRelevanceConfirmedAt(
                        now.minusMinutes(5)
                )
                .responseRelevanceConfirmationNotes(
                        "The response directly addresses "
                                + "the delivery question."
                )
                .publishedByUserId(73L)
                .publishedAt(now)
                .confidenceScore(
                        new BigDecimal("0.94")
                )
                .modelVersion("controller-test-model")
                .createdAt(
                        now.minusMinutes(10)
                )
                .updatedAt(now)
                .version(2L)
                .build();
    }
}