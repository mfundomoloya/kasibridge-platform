package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.CreateTenderRequest;
import com.kasibridge.procurement.dto.TenderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenderService {
    TenderResponse createTender(CreateTenderRequest request);

    Page<TenderResponse> getTenders(Pageable pageable);

    TenderResponse getTenderById(Long id);

    TenderResponse getTenderByReference(String tenderReference);

    TenderResponse publishTender(Long id);

}
