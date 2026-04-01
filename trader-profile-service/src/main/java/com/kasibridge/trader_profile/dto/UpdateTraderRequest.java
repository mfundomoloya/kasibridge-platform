package com.kasibridge.trader_profile.dto;

import lombok.Data;

@Data
public class UpdateTraderRequest {
    private String businessName;
    private String businessType;
    private String tradingArea;
    private String businessDescription;
    private Boolean hasBusinessRegistration;
    private String cipcNumber;
    private Boolean hasBankAccount;
    private String bankName;
}
