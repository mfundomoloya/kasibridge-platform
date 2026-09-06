package com.kasibridge.procurement.service;

import com.kasibridge.procurement.entity.Bid;
import com.kasibridge.procurement.entity.ProcurementAnomaly;
import com.kasibridge.procurement.entity.Tender;

public interface ProcurementNotificationService {
    void queueBidReceived(
            Bid bid,
            Tender tender
    );

    void queueCompliancePassed(
            Bid bid,
            Tender tender
    );

    void queueComplianceFailed(
            Bid bid,
            Tender tender,
            String failureReason

    );

    void queueTenderAwarded(
            Bid bid,
            Tender tender
    );

    void queueAnomalyDetected(
            ProcurementAnomaly anomaly
    );
}
