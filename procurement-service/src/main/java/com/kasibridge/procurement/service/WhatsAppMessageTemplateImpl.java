package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.WhatsAppTemplateContext;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.exception.NotificationOutboxException;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppMessageTemplateImpl implements WhatsAppMessageTemplateService {

    @Override
    public String generateMessage(NotificationOutbox.NotificationTemplateType templateType, WhatsAppTemplateContext context) {
        if (templateType == null) {

            throw new NotificationOutboxException("Notification template type is required.");
        }

        if (context == null) {
            throw new NotificationOutboxException("WhatsApp template context is required.");
        }

        return switch (templateType) {

            case SUPPORT_TICKET_CREATED ->
                    buildSupportTicketCreated(context);

            case SUPPORT_TICKET_RESPONDED ->
                    buildSupportTicketResponded(context);

            case SUPPORT_TICKET_CLOSED ->
                    buildSupportTicketClosed(context);

            case OFFICIAL_CLARIFICATION_PUBLISHED ->
                    buildOfficialClarificationPublished(context);

            case BID_RECEIVED ->
                    buildBidReceived(context);

            case BID_COMPLIANCE_PASSED ->
                    buildBidCompliancePassed(context);

            case BID_COMPLIANCE_FAILED ->
                    buildBidComplianceFailed(context);

            case TENDER_AWARDED ->
                    buildTenderAwarded(context);

            case PROCUREMENT_ANOMALY_DETECTED ->
                    buildProcurementAnomalyDetected(context);
        };
    }

    private String buildSupportTicketCreated(WhatsAppTemplateContext context) {

        requireValue(
                context.getTicketReference(),
                "Ticket reference is required for SUPPORT_TICKET_CREATED."
        );

        String recipientName = greetingName(context.getRecipientName());
        String ticketType = displayValue(
                context.getTicketType(),
                "support"
        );

        return "Hi "
                + recipientName
                + ", your ticket "
                + context.getTicketReference()
                + " has been created. We received your "
                + ticketType
                + " request and will notify you when there is an update.";
    }

    private String buildSupportTicketResponded(WhatsAppTemplateContext context) {

        requireValue(
                context.getTicketReference(),
                "Ticket reference is required for SUPPORT_TICKET_RESPONDED."
        );

        String recipientName = greetingName(context.getRecipientName());

        return "Hi "
                + recipientName
                + ", ticket "
                + context.getTicketReference()
                + " has received a response. Log in to KasiBridge to view the full official response.";
    }

    private String buildSupportTicketClosed(WhatsAppTemplateContext context) {

        requireValue(

                context.getTicketReference(),
                "Ticket reference is required for SUPPORT_TICKET_CLOSED."
        );

        String recipientName = greetingName(context.getRecipientName());

        return "✅ Hi "
                + recipientName
                + ", ticket "
                + context.getTicketReference()
                + " has been closed. Closure notes are available in your ticket history.";
    }

    private String buildOfficialClarificationPublished(WhatsAppTemplateContext context) {

        requireValue(
                context.getTenderId(),
                "Tender ID is required for OFFICIAL_CLARIFICATION_PUBLISHED."
        );

        requireValue(
                context.getTicketReference(),
                "Ticket reference is required for OFFICIAL_CLARIFICATION_PUBLISHED."
        );

        String tenderIdentifier = tenderIdentifier(context);

        return "Official clarification published for "
                + tenderIdentifier
                + ". Reference: "
                + context.getTicketReference()
                + ". The same official response is available to all participating bidders.";
    }

    private String buildBidReceived(WhatsAppTemplateContext context) {

        requireValue(
                context.getBidReference(),
                "Bid reference is required for BID_RECEIVED."
        );

        String recipientName = greetingName(context.getRecipientName());
        String tenderIdentifier = tenderIdentifier(context);

        return "✅ Hi "
                + recipientName
                + ", your bid "
                + context.getBidReference()
                + " has been received for "
                + tenderIdentifier
                + ". Keep the bid reference for tracking.";
    }

    private String buildBidCompliancePassed(WhatsAppTemplateContext context) {
        requireValue(
                context.getBidReference(),
                "Bid reference is required for BID_COMPLIANCE_PASSED."
        );

        return "Compliance passed for bid "
                + context.getBidReference()
                + ". The bid passed the baseline administrative checks and is eligible to proceed to evaluation.";
    }

    private String buildBidComplianceFailed(WhatsAppTemplateContext context) {
        requireValue(
                context.getBidReference(),
                "Bid reference is required for BID_COMPLIANCE_FAILED."
        );

        String failureReason = displayValue(
                context.getComplianceFailureReason(),
                "One or more mandatory compliance requirements were not satisfied."
        );

        return "Compliance failed for bid "
                + context.getBidReference()
                + ". Reason: "
                + failureReason
                + " Log in to KasiBridge to review the result and available support options.";
    }

    private String buildTenderAwarded(WhatsAppTemplateContext context) {

        String tenderIdentifier = tenderIdentifier(context);

        return "The outcome for "
                + tenderIdentifier
                + " has been published. Log in to KasiBridge to view the tender outcome and available evaluation information.";
    }

    private String buildProcurementAnomalyDetected(WhatsAppTemplateContext context) {

        String anomalyReference = displayValue(
                context.getAnomalyReference(),
                "Not available"
        );

        String anomalyType = friendlyEnumValue(displayValue(context.getAnomalyType(), "Unknown anomaly"));

        String severity = displayValue(
                context.getAnomalySeverity(),
                "UNKNOWN"
        );

        return "Procurement anomaly detected. Reference: "
                + anomalyReference
                + ". Type: "
                + anomalyType
                + ". Severity: "
                + severity
                + ". Review is required in the KasiBridge administration portal.";
    }

    private String tenderIdentifier(WhatsAppTemplateContext context) {

        if (hasText(context.getTenderReference())) {
            return "tender " + context.getTenderReference().trim();
        }

        if (context.getTenderId() != null) {
            return "tender ID " + context.getTenderId();
        }

        return "the tender";
    }

    private String greetingName(String value) {
        return hasText(value)
                ? value.trim()
                : "there";
    }

    private String displayValue(String value, String fallback) {
        return hasText(value)
                ? friendlyEnumValue(value.trim())
                : fallback;
    }

    private String friendlyEnumValue(String value) {
        return value
                .replace('_', ' ')
                .toLowerCase();
    }

    private void requireValue(Object value, String message) {
        if (value == null) {
            throw new NotificationOutboxException(message);
        }

        if (value instanceof String text && text.isBlank()) {
            throw new NotificationOutboxException(message);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
