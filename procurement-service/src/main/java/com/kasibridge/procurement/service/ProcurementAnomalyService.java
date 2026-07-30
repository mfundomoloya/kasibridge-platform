package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.ProcurementAnomalyResponse;

import java.util.List;

public interface ProcurementAnomalyService {
    List<ProcurementAnomalyResponse> detectTenderAnomalies(Long tenderId);
}
