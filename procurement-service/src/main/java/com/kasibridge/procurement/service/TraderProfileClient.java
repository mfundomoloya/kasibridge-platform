package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.TraderProfileClientResponse;

public interface TraderProfileClient {
    TraderProfileClientResponse getTraderProfileByUserId(Long userId);

    TraderProfileClientResponse getTraderProfileById(Long traderId);
}
