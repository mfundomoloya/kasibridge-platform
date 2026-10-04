package com.kasibridge.trader_profile.dto;

import com.kasibridge.trader_profile.entity.TraderProfile;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateTraderRequest {

    private Long userId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+27[0-9]{9}$", message = "Phone must be in format +27XXXXXXXXX")
    private String phoneNumber;

    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Business name is required")
    private String businessName;

    private String businessType;

    @NotBlank(message = "Trading area is required")
    private String tradingArea;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String businessDescription;

    @Pattern(regexp = "^[0-9]{13}$",
            message = "ID number must be 13 digits")
    private String idNumber;

    private Boolean hasBusinessRegistration;

    private String cipcNumber;

    private Boolean hasBankAccount;

    private String bankName;

    @NotNull(message = "Onboarding channel is required")
    private TraderProfile.OnboardingChannel onboardingChannel;

    public boolean isRegistered() {
        return Boolean.TRUE.equals(hasBusinessRegistration);
    }

    public boolean hasCipc() {
        return cipcNumber != null && !cipcNumber.trim().isEmpty();
    }

    public boolean hasBankAccount() {
        return Boolean.TRUE.equals(hasBankAccount);
    }

    public boolean hasBankName() {
        return bankName != null && !bankName.trim().isEmpty();
    }

}
