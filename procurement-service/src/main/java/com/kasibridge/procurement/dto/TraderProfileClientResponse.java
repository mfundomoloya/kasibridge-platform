package com.kasibridge.procurement.dto;

import lombok.Data;

@Data
public class TraderProfileClientResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String businessName;
    private String businessType;
    private String tradingArea;
}
