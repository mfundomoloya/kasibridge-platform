package com.kasibridge.trader_profile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trader_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraderProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false ,unique = true)
    private Long userId;

    //this is personal info
    @Column(name = "full_name",nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(unique = true, length = 254)
    private String email;


    //this is business info
    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "business_type", length = 100)
    private String businessType;                //this categorizes the business : Spaza Shop, Street Vendor, Salon

    @Column(name = "trading_area", length = 150)
    private String tradingArea;                 //Township, Suburb

    @Column(name = "business_description", length = 1000)
    private String businessDescription;

    //compliance & credibility
    @Column(name = "id_number", length = 13)
    private String idNumber;          // SA ID (optional, for verification)

    private Boolean hasBusinessRegistration;


    @Column(name = "cipc_number", length = 50)
    private String cipcNumber;        // CIPC registration number if available

    private Boolean hasBankAccount;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    //channel
    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_channel", length = 20)
    private OnboardingChannel onboardingChannel;  // WEB or WHATSAPP

    //profile status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProfileStatus status = ProfileStatus.PENDING;

    //audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    //enums
    public enum ProfileStatus {
        PENDING,    // Just registered, not yet verified
        ACTIVE,     // Verified and active
        SUSPENDED,  // Flagged or suspended
        INACTIVE    // Deactivated by trader
    }

    public enum OnboardingChannel {
        WEB,
        WHATSAPP
    }
}
