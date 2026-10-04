package com.kasibridge.trader_profile.dto;

import jakarta.validation.Valid;
import lombok.Data;

@Data
@Valid
public class UpdateTraderRequest {
    private Long userId;
    private String businessName;
    private String businessType;
    private String tradingArea;
    private String businessDescription;
    private Boolean hasBusinessRegistration;
    private String cipcNumber;
    private Boolean hasBankAccount;
    private String bankName;
}
