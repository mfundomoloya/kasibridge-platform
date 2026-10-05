package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.CreateTenderRequest;
import com.kasibridge.procurement.dto.SpecificationIntegrityResponse;
import com.kasibridge.procurement.dto.TenderResponse;
import com.kasibridge.procurement.dto.TraderTenderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenderService {
    TenderResponse createTender(CreateTenderRequest request);

    Page<TenderResponse> getTenders(Pageable pageable);

    Page<TraderTenderResponse> getOpenTenders(Pageable pageable);

    TraderTenderResponse getOpenTenderById(Long id);

    TenderResponse getTenderById(Long id);

    TenderResponse getTenderByReference(String tenderReference);

    TenderResponse publishTender(Long id);

    TenderResponse closeBidding(Long id);

    TenderResponse startEvaluation(Long id);

    TenderResponse startAdjudication(Long id);

    SpecificationIntegrityResponse verifySpecificationIntegrity(Long id);

}
