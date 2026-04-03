package com.kasibridge.trader_profile.dto;

import com.kasibridge.trader_profile.entity.TraderProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTraderRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+27[0-9]{9}$", message = "Phone must be in format +27XXXXXXXXX")
    private String phoneNumber;

    private String email;

    @NotBlank(message = "Business name is required")
    private String businessName;

    private String businessType;

    @NotBlank(message = "Trading area is required")
    private String tradingArea;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String businessDescription;

    private String idNumber;

    private Boolean hasBusinessRegistration;

    private String cipcNumber;

    private Boolean hasBankAccount;

    private String bankName;

    private TraderProfile.OnboardingChannel onboardingChannel;

    public boolean isRegistered() {
        return Boolean.TRUE.equals(hasBusinessRegistration);
    }

    public boolean hasCipc() {
        return cipcNumber != null && !cipcNumber.trim().isEmpty();
    }
}
