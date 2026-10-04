package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.TicketAiTriageResult;
import com.kasibridge.procurement.entity.SupportTicket;
import com.kasibridge.procurement.entity.SupportTicketAiAssessment;
import com.kasibridge.procurement.exception.SupportTicketException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@Slf4j
public class RuleBasedTicketAiTriageService implements  TicketAiTriageService {

    private static final String MODEL_VERSION = "RULE-BASED-TRIAGE-V1";

    @Override
    public TicketAiTriageResult assessTicket(SupportTicket ticket) {

        if(ticket == null) {
            throw new SupportTicketException("Support ticket is required for AI assessment.");
        }

        String combinedText = normalize(
                value(ticket.getSubject())
                + " "
                + value(ticket.getDescription())
        );

        TicketAiTriageResult result;

        if (containsCorruptionAllegation(combinedText)) {
            result = corruptionAllegationResult(ticket);

        } else if (containsAwardDispute(combinedText)) {
            result = awardDisputeResult(ticket);

        } else if (containsScoringDispute(combinedText)) {
            result = scoringDisputeResult(ticket);

        } else if (containsComplianceOverrideRequest(combinedText)) {
            result = complianceOverrideResult(ticket);

        } else if (ticket.getTicketType()
                == SupportTicket.TicketType.CLARIFICATION_REQUEST) {
            result = clarificationResult(ticket, combinedText);

        } else if (ticket.getTicketType()
                == SupportTicket.TicketType.COMPLIANCE_APPEAL) {
            result = complianceAppealResult(ticket);

        } else if (ticket.getTicketType()
                == SupportTicket.TicketType.UPLOAD_ISSUE) {
            result = uploadIssueResult(ticket, combinedText);

        } else if (containsBidStatusQuery(combinedText)) {
            result = bidStatusQueryResult(ticket);

        } else if (containsDocumentQuery(combinedText)) {
            result = documentRequirementResult(ticket);

        } else if (containsDeadlineQuery(combinedText)) {
            result = deadlineQueryResult(ticket);

        } else {
            result = generalSupportResult(ticket);
        }

        log.info("Rule-based ticket assessment generated: ticketId={} category={} priority={} resolutionMode={} confidence={}",
                ticket.getId(),
                result.getDetectedCategory(),
                result.getPriority(),
                result.getResolutionMode(),
                result.getConfidenceScore()
        );

        return result;
    }

    private TicketAiTriageResult corruptionAllegationResult(
            SupportTicket ticket
    ) {
        return result(
                SupportTicketAiAssessment.DetectedCategory.CORRUPTION_ALLEGATION,
                SupportTicketAiAssessment.TicketPriority.CRITICAL,
                SupportTicketAiAssessment.AiResolutionMode.HUMAN_ONLY,
                summary(
                        ticket,
                        "The ticket contains a possible corruption, bribery, favouritism, or procurement misconduct allegation."
                ),
                null,
                "The allegation requires investigation by an authorised procurement oversight official. AI must not determine the outcome.",
                "99.00"
        );
    }

    private TicketAiTriageResult awardDisputeResult(
            SupportTicket ticket
    ) {
        return result(
                SupportTicketAiAssessment.DetectedCategory.AWARD_DISPUTE,
                SupportTicketAiAssessment.TicketPriority.HIGH,
                SupportTicketAiAssessment.AiResolutionMode.HUMAN_ONLY,
                summary(
                        ticket,
                        "The trader is disputing or questioning a tender award outcome."
                ),
                null,
                "Award disputes may have contractual, legal, or procurement-governance consequences and require human review.",
                "97.00"
        );

    }

    private TicketAiTriageResult scoringDisputeResult(
            SupportTicket ticket
    ) {
        return result(
                SupportTicketAiAssessment.DetectedCategory.SCORING_DISPUTE,
                SupportTicketAiAssessment.TicketPriority.HIGH,
                SupportTicketAiAssessment.AiResolutionMode.HUMAN_ONLY,
                summary(
                        ticket,
                        "The trader is disputing or questioning bid evaluation scores."
                ),
                null,
                "Evaluation scoring disputes require review by an authorised official. AI must not alter or validate evaluator scores.",
                "97.00"
);

    }

    private TicketAiTriageResult complianceOverrideResult(
            SupportTicket ticket
    ) {
        return result(
                SupportTicketAiAssessment.DetectedCategory.COMPLIANCE_OVERRIDE_REQUEST,
                SupportTicketAiAssessment.TicketPriority.HIGH,
                SupportTicketAiAssessment.AiResolutionMode.HUMAN_ONLY,
                summary(
                        ticket,
                        "The ticket appears to request an override or bypass of a mandatory compliance decision."
                ),
                null,
                "Mandatory compliance controls cannot be overridden by the AI ticket assistant.",
                "98.00"
        );

    }

    private TicketAiTriageResult clarificationResult(
            SupportTicket ticket,
            String combinedText
    ) {

        SupportTicketAiAssessment.TicketPriority priority = containsUrgentDeadlineLanguage(combinedText)
                ? SupportTicketAiAssessment.TicketPriority.HIGH
                : SupportTicketAiAssessment.TicketPriority.MEDIUM;

        return result(
                SupportTicketAiAssessment.DetectedCategory.CLARIFICATION_REQUEST,
                priority,
                SupportTicketAiAssessment.AiResolutionMode.DRAFT_FOR_APPROVAL,
                summary(
                        ticket,
                        "The trader is requesting clarification about the tender requirements or scope."
                ),

                "Thank you for your clarification request. The question has been recorded for review by the authorised " +
                        "procurement official. Any approved clarification will be published to all participating bidders at the same time.",
                "Official tender clarification requires approval before publication and must be made available equally " +
                        "to all participating bidders.",
                "95.00"
        );
    }

    private TicketAiTriageResult complianceAppealResult(
            SupportTicket ticket
    ) {
        return result(
                SupportTicketAiAssessment.DetectedCategory
                        .COMPLIANCE_APPEAL,
                SupportTicketAiAssessment.TicketPriority.HIGH,
                SupportTicketAiAssessment.AiResolutionMode.DRAFT_FOR_APPROVAL,
                summary(
                        ticket,
                        "The trader is requesting review of a failed compliance decision."
                ),

                "Your compliance appeal has been recorded for review. Please ensure that all supporting documents are " +
                        "attached and remain valid. An authorised official must review the evidence before any decision is made.",
                "A human reviewer must assess the supporting evidence. AI cannot approve an appeal or override the compliance gate.",
                "96.00"
        );
    }

    private TicketAiTriageResult uploadIssueResult(
            SupportTicket ticket,
            String combinedText
    ) {
        boolean urgent = containsUrgentDeadlineLanguage(combinedText);

        return result(
                SupportTicketAiAssessment.DetectedCategory.UPLOAD_ISSUE,
                urgent
                        ? SupportTicketAiAssessment.TicketPriority.HIGH
                        : SupportTicketAiAssessment.TicketPriority.MEDIUM,
                SupportTicketAiAssessment.AiResolutionMode.DRAFT_FOR_APPROVAL,
        summary(
                ticket,
                urgent
                        ? "The trader reports an upload problem close to a submission deadline."
                        : "The trader reports a problem uploading or submitting tender information."
),
        "Your upload issue has been recorded with its submission timestamp. Please retain the ticket reference and " +
                "provide any screenshots or error messages. A support official will review the technical evidence and tender deadline.",
        "A human reviewer must confirm whether the technical issue affected a tender deadline or bid eligibility.",

        urgent ? "96.00" : "92.00"

);

    }

    private TicketAiTriageResult bidStatusQueryResult(
            SupportTicket ticket
    ) {
        return result(
                SupportTicketAiAssessment.DetectedCategory
                        .BID_STATUS_QUERY,
                SupportTicketAiAssessment.TicketPriority.LOW,
                SupportTicketAiAssessment.AiResolutionMode.AUTO_RESOLVE,
                summary(
                        ticket,
                        "The trader is requesting information about the current status of a submitted bid."
                ),

                "You can view the current bid status from the KasiBridge trader dashboard using your bid reference. " +
                        "If the status is COMPLIANCE_FAILED, review the recorded failure reason and available appeal options.",
                null,

        "90.00"
        );

    }

    private TicketAiTriageResult documentRequirementResult(
            SupportTicket ticket
    ) {
        return result(
                SupportTicketAiAssessment.DetectedCategory.DOCUMENT_REQUIREMENT_QUERY,
                SupportTicketAiAssessment.TicketPriority.MEDIUM,
                SupportTicketAiAssessment.AiResolutionMode.DRAFT_FOR_APPROVAL,
                summary(
                        ticket,
                        "The trader is asking about required tender or compliance documents."
                ),
                "Please review the mandatory document checklist attached to the tender. Documents marked as mandatory " +
                        "must be valid and uploaded before final submission. An authorised official should confirm any tender-specific interpretation.",
                "Tender-specific document interpretations require confirmation from an authorised procurement official.",
                "91.00"
        );
    }

    private TicketAiTriageResult deadlineQueryResult(
            SupportTicket ticket
    ) {

        return result(
                SupportTicketAiAssessment.DetectedCategory.DEADLINE_QUERY,
                SupportTicketAiAssessment.TicketPriority.MEDIUM,
                SupportTicketAiAssessment.AiResolutionMode
                        .DRAFT_FOR_APPROVAL,
                summary(
                        ticket,
                        "The trader is requesting information or clarification about a tender deadline."
                ),
                "Please use the official deadline published on the tender record. If the deadline appears unclear or" +
                        " has changed, an authorised official must confirm the correct date through a public clarification.",
                "AI should not independently interpret or change an official tender deadline.",
                "93.00"
        );
    }

    private TicketAiTriageResult generalSupportResult(
            SupportTicket ticket
    ) {
        return result(
                SupportTicketAiAssessment.DetectedCategory.GENERAL_SUPPORT,
                SupportTicketAiAssessment.TicketPriority.LOW,
                SupportTicketAiAssessment.AiResolutionMode.AUTO_RESOLVE,
                summary(
                        ticket,
                        "The ticket contains a general platform support request."
                ),
                "Your support request has been recorded. Please use the ticket reference to track progress. " +
                        "If the issue affects a tender deadline, compliance decision, evaluation score, or award outcome, " +
                        "the ticket will be escalated to a human reviewer.",
                null,
                "80.00"
        );

    }

    private TicketAiTriageResult result(
            SupportTicketAiAssessment.DetectedCategory category,
            SupportTicketAiAssessment.TicketPriority priority,
            SupportTicketAiAssessment.AiResolutionMode resolutionMode,
            String summary,
            String suggestedResponse,
            String escalationReason,
            String confidenceScore
    ) {
        return TicketAiTriageResult.builder()
                .detectedCategory(category)
                .priority(priority)
                .resolutionMode(resolutionMode)
                .summary(summary)
                .suggestedResponse(suggestedResponse)
                .escalationReason(escalationReason)
                .confidenceScore(new BigDecimal(confidenceScore))
                .modelVersion(MODEL_VERSION)
                .build();
    }

    private String summary(
            SupportTicket ticket,
            String classificationSummary
    ) {
        return "Ticket "
                + displayTicketReference(ticket)
                + ": "
                + classificationSummary;
    }

    private String displayTicketReference(
            SupportTicket ticket
    ) {
        return hasText(ticket.getTicketReference())
                ? ticket.getTicketReference().trim()
                : String.valueOf(ticket.getId());

    }

    private boolean containsCorruptionAllegation(String text) {
        return containsAny(
                text,
                "corruption",
                "bribe",
                "bribery",
                "kickback",
                "favouritism",
                "favoritism",
                "insider",
                "collusion",
                "fraud",
                "official asked for money"
        );
    }

    private boolean containsAwardDispute(String text) {
        return containsAny(
                text,
                "award dispute",
                "wrong bidder won",
                "wrong bidder was awarded",
                "challenge the award",
                "contest the award",
                "award decision",
                "unfair award",
                "why did i not win",
                "why was i not awarded"
        );
    }

    private boolean containsScoringDispute(String text) {
        return containsAny(
                text,
                "score dispute",
                "scoring dispute",
                "wrong score",
                "evaluation score",
                "unfair score",
                "change my score",
                "review my score",
                "score breakdown"
        );
    }

    private boolean containsComplianceOverrideRequest(String text) {
        return containsAny(
                text,
                "override compliance",
                "bypass compliance",
                "ignore compliance",
                "approve without documents",
                "pass without documents",
                "manually approve",
                "remove disqualification"
        );
    }

    private boolean containsBidStatusQuery(String text) {
        return containsAny(
                text,
                "bid status",
                "status of my bid",
                "track my bid",
                "where is my bid",
                "what happened to my bid",
                "view submitted bid"
        );
    }

    private boolean containsDocumentQuery(String text) {
        return containsAny(
                text,
                "required document",
                "mandatory document",
                "missing document",
                "document checklist",
                "tax document",
                "tax clearance",
                "sars",
                "csd",
                "b-bbee",
                "bbbee",
                "proof of address"
        );
    }

    private boolean containsDeadlineQuery(String text) {
        return containsAny(
                text,
                "deadline",
                "closing date",
                "closing time",
                "submission date",
                "submission time",
                "when does the tender close",
                "extension"
        );
    }

    private boolean containsUrgentDeadlineLanguage(String text) {
        return containsAny(
                text,
                "today",
                "tomorrow",
                "minutes remaining",
                "hours remaining",
                "before closing",
                "close to deadline",
                "deadline is near",
                "deadline",
                "closing time"
        );
    }

    private boolean containsAny(
            String text,
            String... keywords
    ) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String value) {
        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}