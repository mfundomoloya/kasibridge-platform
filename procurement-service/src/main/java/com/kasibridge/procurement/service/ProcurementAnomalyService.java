package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.ProcurementAnomalyRecordResponse;
import com.kasibridge.procurement.dto.ProcurementAnomalyResponse;
import com.kasibridge.procurement.dto.ReviewProcurementAnomalyRequest;
import com.kasibridge.procurement.entity.ProcurementAnomaly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProcurementAnomalyService {
    List<ProcurementAnomalyResponse> detectTenderAnomalies(Long tenderId);
    List<ProcurementAnomalyRecordResponse> detectAndPersistTenderAnomalies(Long tenderId);

    Page<ProcurementAnomalyRecordResponse> getAnomalies(Pageable pageable);

    Page<ProcurementAnomalyRecordResponse> getAnomaliesByTender(Long tenderId, Pageable pageable);

    Page<ProcurementAnomalyRecordResponse> getAnomaliesByStatus(ProcurementAnomaly.AnomalyStatus status, Pageable pageable);

    ProcurementAnomalyRecordResponse markReviewed(Long anomalyId, ReviewProcurementAnomalyRequest request);

    ProcurementAnomalyRecordResponse dismiss(Long anomalyId, ReviewProcurementAnomalyRequest request
    );
}
