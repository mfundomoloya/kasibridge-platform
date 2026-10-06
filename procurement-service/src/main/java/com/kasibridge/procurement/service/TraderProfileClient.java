package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.TraderProfileClientResponse;

public interface TraderProfileClient {
    TraderProfileClientResponse getCurrentTraderProfile();

    TraderProfileClientResponse getTraderProfileById(Long traderId);

    TraderProfileClientResponse getTraderProfileByIdAsSystem(Long traderProfileId);
}
