package com.kasibridge.trader_profile.service;

import com.kasibridge.trader_profile.dto.CreateTraderRequest;
import com.kasibridge.trader_profile.dto.TraderProfileResponse;
import com.kasibridge.trader_profile.dto.UpdateTraderRequest;
import com.kasibridge.trader_profile.entity.TraderProfile;

import java.util.List;

public interface TraderProfileService {

    //create a trader profile
    TraderProfileResponse createProfile(CreateTraderRequest request);

    // Get a single profile by ID
    TraderProfileResponse getProfileById(Long id);

    // Get a profile by phone number (used by WhatsApp adapter)
    TraderProfileResponse getProfileByPhone(String phoneNumber);

    // Get all profiles
    List<TraderProfileResponse> getAllProfiles();

    // Get all profiles by trading area
    List<TraderProfileResponse> getProfilesByArea(String tradingArea);

    // Get all profiles by status
    List<TraderProfileResponse> getProfilesByStatus(TraderProfile.ProfileStatus status);

    // Get all profiles by onboarding channel
    List<TraderProfileResponse> getProfilesByChannel(TraderProfile.OnboardingChannel channel);

    // Update a trader profile
    TraderProfileResponse updateProfile(Long id, UpdateTraderRequest request);

    // Change profile status (activate, suspend, deactivate)
    TraderProfileResponse updateStatus(Long id, TraderProfile.ProfileStatus newStatus);

    // Delete a profile
    void deleteProfile(Long id);
}
