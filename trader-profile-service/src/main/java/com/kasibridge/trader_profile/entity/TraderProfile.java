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

    //this is personal info
    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(unique = true)
    private String email;


    //this is business info
    @Column(nullable = false)

    private String businessName;
    private String businessType;                //this categorizes the business : Spaza Shop, Street Vendor, Salon
    private String tradingArea;                 //Township, Suburb

    @Column(length = 1000)
    private String businessDescription;

    //compliance & credibility
    private String idNumber;          // SA ID (optional, for verification)

    private Boolean hasBusinessRegistration;

    private String cipcNumber;        // CIPC registration number if available

    private Boolean hasBankAccount;

    private String bankName;

    //channel
    @Enumerated(EnumType.STRING)
    private OnboardingChannel onboardingChannel;  // WEB or WHATSAPP

    //profile status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProfileStatus status = ProfileStatus.PENDING;

    //audit fields
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
