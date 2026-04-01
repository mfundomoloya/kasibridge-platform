package com.kasibridge.trader_profile.repository;

import com.kasibridge.trader_profile.entity.TraderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TraderProfileRepository extends JpaRepository<TraderProfile, Long> {
    // Find by phone (used for WhatsApp lookups)
    Optional<TraderProfile> findByPhoneNumber(String phoneNumber);

    // Find by email
    Optional<TraderProfile> findByEmail(String email);

    // Find all traders in a specific area
    List<TraderProfile> findByTradingArea(String tradingArea);

    // Find all traders by status
    List<TraderProfile> findByStatus(TraderProfile.ProfileStatus status);

    // Find by onboarding channel (WEB vs WHATSAPP)
    List<TraderProfile> findByOnboardingChannel(TraderProfile.OnboardingChannel channel);

    // Check if phone already registered
    boolean existsByPhoneNumber(String phoneNumber);

    // Check if email already registered
    boolean existsByEmail(String email);
}
