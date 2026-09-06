package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.TraderProfileClientResponse;
import com.kasibridge.procurement.dto.WhatsAppTemplateContext;
import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.NotificationOutbox;
import com.kasibridge.procurement.entity.ProcurementAnomaly;
import com.kasibridge.procurement.entity.Tender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementNotificationServiceImpl implements ProcurementNotificationService {

    private final TraderProfileClient traderProfileClient;
    private final NotificationOutboxService notificationOutboxService;
    private final WhatsAppMessageTemplateService templateService;

    @Override
    public void queueBidReceived(Bid bid, Tender tender) {
        try {
            TraderProfileClientResponse trader = loadTrader(bid);

            WhatsAppTemplateContext context =
            baseBidContext(bid, tender, trader);

            queueBidderNotification(
                    NotificationOutbox.NotificationTemplateType.BID_RECEIVED,
                    context,
                    trader,
                    bid,
                    tender
            );

        } catch (Exception ex) {
            log.error(
                    "Unable to queue BID_RECEIVED notification for tenderId={} bidId={}",
                    safeTenderId(tender),
                    safeBidId(bid),
                    ex
            );
        }
    }

    @Override
    public void queueCompliancePassed(Bid bid, Tender tender) {

        try {


            TraderProfileClientResponse trader = loadTrader(bid);

            WhatsAppTemplateContext context = baseBidContext(
                    bid,
                    tender,
                    trader
            );

            queueBidderNotification(
                    NotificationOutbox.NotificationTemplateType.BID_COMPLIANCE_PASSED,
                    context,
                    trader,
                    bid,
                    tender
            );
        } catch (Exception ex) {
            log.error(
                    "Unable to queue BID_COMPLIANCE_PASSED notification for tenderId={} bidId={}",
                    safeTenderId(tender),
                    safeBidId(bid),
                    ex
            );
        }

    }

    @Override
    public void queueComplianceFailed(Bid bid, Tender tender, String failureReason) {
        try {
            TraderProfileClientResponse trader = loadTrader(bid);

            WhatsAppTemplateContext context = WhatsAppTemplateContext.builder()

                    .recipientName(trader.getFullName())
                    .tenderId(tender.getId())
                    .tenderReference(tender.getTenderReference())
                    .tenderTitle(tender.getTitle())
                    .bidId(bid.getId())
                    .bidReference(bid.getBidReference())
                    .bidderAlias(bid.getBidderAlias())
                    .bidAmount(bid.getPriceAmount())
                    .complianceFailureReason(failureReason)
                    .build();

            queueBidderNotification(
                    NotificationOutbox.NotificationTemplateType.BID_COMPLIANCE_FAILED,
                    context,
                    trader,
                    bid,
                    tender
            );
        } catch (Exception ex) {
            log.error(
                    "Unable to queue BID_COMPLIANCE_FAILED notification for tenderId={} bidId={}",
                    safeTenderId(tender),
                    safeBidId(bid),
                    ex
            );
        }
    }

    @Override
    public void queueTenderAwarded(Bid bid, Tender tender) {

        try {

            TraderProfileClientResponse trader = loadTrader(bid);

            WhatsAppTemplateContext context = baseBidContext(
                    bid,
                    tender,
                    trader
            );

            queueBidderNotification(
                    NotificationOutbox.NotificationTemplateType.TENDER_AWARDED,
                    context,
                    trader,
                    bid,
                    tender
            );
        } catch (Exception ex) {
            log.error(
                    "Unable to queue TENDER_AWARDED notification for tenderId={} bidId={}",
                    safeTenderId(tender),
                    safeBidId(bid),
                    ex
            );
        }
    }

    @Override
    public void queueAnomalyDetected(ProcurementAnomaly anomaly) {

        try {

            WhatsAppTemplateContext context = WhatsAppTemplateContext.builder()

                    .tenderId(anomaly.getTenderId())
                    .bidId(anomaly.getBidId())
                    .anomalyReference(anomaly.getAnomalyReference())
                    .anomalyType(anomaly.getType().name())
                    .anomalySeverity(anomaly.getSeverity().name())
                    .build();

            String message = templateService.generateMessage(
                    NotificationOutbox.NotificationTemplateType.PROCUREMENT_ANOMALY_DETECTED,
                    context
            );

            notificationOutboxService.queueNotification(
                    NotificationOutbox.NotificationChannel.IN_APP,
                    NotificationOutbox.NotificationTemplateType.PROCUREMENT_ANOMALY_DETECTED,
                    null,
                    null,
                    null,
                    message,

                    anomaly.getTenderId(),
                    anomaly.getBidId(),
                    null
            );
        } catch (Exception ex) {
            log.error(
                    "Unable to queue PROCUREMENT_ANOMALY_DETECTED notification for anomalyId={} tenderId={}",
                    anomaly != null ? anomaly.getId() : null,
                    anomaly != null ? anomaly.getTenderId() : null,
                    ex
            );
        }
    }

    private TraderProfileClientResponse loadTrader(Bid bid) {

        if (bid.getTraderProfileId() == null) {
            throw new IllegalStateException("Bid does not contain a trader profile ID.");
        }

        return traderProfileClient.getTraderProfileById(
                bid.getTraderProfileId()
        );
    }

    private WhatsAppTemplateContext baseBidContext(Bid bid, Tender tender, TraderProfileClientResponse trader) {
        return WhatsAppTemplateContext.builder()
                .recipientName(trader.getFullName())
                .tenderId(tender.getId())
                .tenderReference(tender.getTenderReference())
                .tenderTitle(tender.getTitle())
                .bidId(bid.getId())
                .bidReference(bid.getBidReference())
                .bidderAlias(bid.getBidderAlias())
                .bidAmount(bid.getPriceAmount())
                .build();
    }

    private void queueBidderNotification(NotificationOutbox.NotificationTemplateType templateType,
                                         WhatsAppTemplateContext context,
                                         TraderProfileClientResponse trader,
                                         Bid bid,
                                         Tender tender) {
        String message = templateService.generateMessage(
                templateType,
                context
        );

        notificationOutboxService.queueNotification(
                NotificationOutbox.NotificationChannel.WHATSAPP,
                templateType,
                trader.getUserId(),
                trader.getPhoneNumber(),
                trader.getEmail(),
                message,
                tender.getId(),
                bid.getId(),
        null
);

        log.info(
                "Procurement notification queued: templateType={} tenderId={} bidId={} recipientUserId={}",
                templateType,
                tender.getId(),
                bid.getId(),
                trader.getUserId()
        );
    }

    private Long safeBidId(Bid bid) {
        return bid != null ? bid.getId() : null;
    }

    private Long safeTenderId(Tender tender) {
        return tender != null ? tender.getId() : null;
    }
}