package com.kasibridge.procurement.event;

import com.kasibridge.procurement.exception.TicketAiAssessmentStateException;
import com.kasibridge.procurement.service.SupportTicketAiAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupportTicketAiAssessmentListener {

    private final SupportTicketAiAssessmentService assessmentService;

    @Value("${kasibridge.ticket-ai.auto-assessment.enabled:true}")
    private boolean autoAssessmentEnabled;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSupportTicketCreated(
            SupportTicketCreatedEvent event
    ) {
        if (!autoAssessmentEnabled) {
            log.info("Automatic ticket AI assessment is disabled. ticketId={}", event.ticketId());

            return;
        }

        try {
            assessmentService.generateAssessment(
                    event.ticketId()
            );

            log.info("Automatic ticket AI assessment completed: ticketId={}", event.ticketId());

        } catch (TicketAiAssessmentStateException ex) {
            log.warn(
                    "Automatic ticket AI assessment skipped: ticketId={} reason={}",
                    event.ticketId(),
                    ex.getMessage()
            );

        } catch (Exception ex) {
            log.error(
                    "Automatic ticket AI assessment failed without affecting ticket creation: ticketId={}",
                    event.ticketId(),
                    ex
            );
        }
    }
}