package com.kasibridge.trader_profile.dto;

import com.kasibridge.trader_profile.entity.TraderProfile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TraderProfileResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String businessName;
    private String businessType;
    private String tradingArea;
    private String businessDescription;
    private Boolean hasBusinessRegistration;
    private String cipcNumber;
    private Boolean hasBankAccount;
    private String bankName;
    private TraderProfile.OnboardingChannel onboardingChannel;
    private TraderProfile.ProfileStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Credibility score will be computed by the AI module later
    private Integer credibilityScore;

    public static TraderProfileResponse from(TraderProfile trader) {
        return TraderProfileResponse.builder()
                .id(trader.getId())
                .fullName(trader.getFullName())
                .phoneNumber(trader.getPhoneNumber())
                .email(trader.getEmail())
                .businessName(trader.getBusinessName())
                .businessType(trader.getBusinessType())
                .tradingArea(trader.getTradingArea())
                .businessDescription(trader.getBusinessDescription())
                .hasBusinessRegistration(trader.getHasBusinessRegistration())
                .cipcNumber(trader.getCipcNumber())
                .hasBankAccount(trader.getHasBankAccount())
                .bankName(trader.getBankName())
                .onboardingChannel(trader.getOnboardingChannel())
                .status(trader.getStatus())
                .createdAt(trader.getCreatedAt())
                .updatedAt(trader.getUpdatedAt())
                .credibilityScore(null) // this is just a placeholder until AI module is wired
                .build();
    }
}
