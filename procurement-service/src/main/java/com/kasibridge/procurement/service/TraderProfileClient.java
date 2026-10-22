package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.TraderProfileClientResponse;

import java.util.List;

public interface TraderProfileClient {
    TraderProfileClientResponse getCurrentTraderProfile();

    TraderProfileClientResponse getTraderProfileById(Long traderId);

    TraderProfileClientResponse getTraderProfileByIdAsSystem(Long traderProfileId);

    List<TraderProfileClientResponse> findActiveMatchingProfilesAsSystem(String businessType, String tradingArea);

}
