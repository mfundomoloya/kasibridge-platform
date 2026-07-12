package com.kasibridge.trader_profile.service;

import com.kasibridge.trader_profile.dto.CreateTraderRequest;
import com.kasibridge.trader_profile.dto.TraderProfileResponse;
import com.kasibridge.trader_profile.dto.UpdateTraderRequest;
import com.kasibridge.trader_profile.entity.TraderProfile;
import com.kasibridge.trader_profile.exception.BusinessValidationException;
import com.kasibridge.trader_profile.exception.DuplicateTraderException;
import com.kasibridge.trader_profile.exception.TraderNotFoundException;
import com.kasibridge.trader_profile.repository.TraderProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TraderProfileServiceImpl implements TraderProfileService{
    private static final Logger log = LoggerFactory.getLogger(TraderProfileServiceImpl.class);
    //repository to persist to database
    private final TraderProfileRepository repository;


    @Override
    public TraderProfileResponse createProfile(CreateTraderRequest request) {
        log.info("Creating trade profile for phone: {}", request.getPhoneNumber() );

        //check for duplicate phone
        if(repository.existsByPhoneNumber(request.getPhoneNumber())){
            throw new DuplicateTraderException(
                    "A trader with phone number " + request.getPhoneNumber() + " already exists."
            );
        }

        //check for duplicate email (if only email is provided)
        if(request.getEmail() != null && repository.existsByEmail(request.getEmail())){
            throw new DuplicateTraderException(
                    "A trader with email " + request.getEmail() + " already exists."
            );
        }

        validateCreatTraderRequest(request);

        //create the trader profile
        TraderProfile trader = TraderProfile.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .businessName(request.getBusinessName())
                .businessType(request.getBusinessType())
                .tradingArea(request.getTradingArea())
                .businessDescription(request.getBusinessDescription())
                .idNumber(request.getIdNumber())
                .hasBusinessRegistration(request.getHasBusinessRegistration())
                .cipcNumber(request.getCipcNumber())
                .hasBankAccount(request.getHasBankAccount())
                .bankName(request.getBankName())
                .onboardingChannel(request.getOnboardingChannel() != null ? request.getOnboardingChannel() : TraderProfile.OnboardingChannel.WEB)
                .status(TraderProfile.ProfileStatus.PENDING)
                .build();


        TraderProfile saved = repository.save(trader);
        log.info("Trader profile created with ID: {}", saved.getId());
        return TraderProfileResponse.from(saved);
    }

    private void validateCreatTraderRequest(CreateTraderRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();

        //cipc only valid if business is required
        if(request.hasCipc() && !request.isRegistered()) {
            fieldErrors.put(
                    "cipcNumber",
                    "CIPC number can only be provided if the business is registered."
            );
        }

        //if registered, CIPC number is required
        if (request.isRegistered() && !request.hasCipc()) {
            fieldErrors.put(
                    "cipcNumber",
                    "CIPC number is required when business is registered."
            );
        }

        //bank name only valid if trader has a bank account
        if (request.hasBankName() && !request.hasBankAccount()) {
            fieldErrors.put(
                    "bankName",
                    "Bank name can only be provided if the trader has a bank account."
            );
        }

        //if trader has a bank account, bank name is required
        if (request.hasBankAccount() && !request.hasBankName()) {
            fieldErrors.put(
                    "bankName",
                    "Bank name is required when trader has a bank account."
            );
        }

        if(!fieldErrors.isEmpty()) {
            throw new BusinessValidationException("Validation failed", fieldErrors);
        }

    }

    @Override
    @Transactional(readOnly = true)
    public TraderProfileResponse getProfileById(Long id) {
        log.info("Fetching trader profile with ID: {}", id);
        TraderProfile trader = findByIdOrThrow(id);
        return TraderProfileResponse.from(trader);
    }

    private TraderProfile findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TraderNotFoundException(
                        "Trader profile not found with ID: " + id
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public TraderProfileResponse getProfileByPhone(String phoneNumber) {
        log.info("Fetching trader profile for phone: {}" , phoneNumber);
        TraderProfile trader = repository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new TraderNotFoundException(
                        "No trader found with phone number: " + phoneNumber
                ));
        return TraderProfileResponse.from(trader);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraderProfileResponse> getAllProfiles() {
        log.info("Fetching all trader profiles");

        return repository.findAll()
                .stream()
                .map(TraderProfileResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraderProfileResponse> getProfilesByArea(String tradingArea) {
        log.info("Fetching all trader profiles for area: {}", tradingArea);

        return repository.findByTradingArea(tradingArea)
                .stream()
                .map(TraderProfileResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraderProfileResponse> getProfilesByStatus(TraderProfile.ProfileStatus status) {
        log.info("Fetching all trader profiles with status: {}", status);

        return repository.findByStatus(status)
                .stream()
                .map(TraderProfileResponse::from)
                  .collect(Collectors.toList());

    }

    @Override
    @Transactional(readOnly = true)
    public List<TraderProfileResponse> getProfilesByChannel(TraderProfile.OnboardingChannel channel) {
        log.info("Fetching all trader profiles onboarded by via: {}", channel);

        return repository.findByOnboardingChannel(channel)
                .stream()
                .map(TraderProfileResponse::from)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public TraderProfileResponse updateProfile(Long id, UpdateTraderRequest request) {
        log.info("Updating trader profile with ID: {}", id);
        TraderProfile trader = findByIdOrThrow(id);

        // Determine effective registration state
        boolean effectivelyRegistered = request.getHasBusinessRegistration() != null
                ? request.getHasBusinessRegistration()
                : Boolean.TRUE.equals(trader.getHasBusinessRegistration());

        boolean incomingCipc = isValid(request.getCipcNumber());
        boolean existingCipc = isValid(trader.getCipcNumber());

        // Cross-field validation
        if (incomingCipc && !effectivelyRegistered) {
            throw new IllegalArgumentException(
                    "CIPC number can only be provided if the business is registered."
            );
        }

        if (effectivelyRegistered && !incomingCipc && !existingCipc) {
            throw new IllegalArgumentException(
                    "CIPC number is required when business is registered."
            );
        }

        if (isValid(request.getBusinessName()))
            trader.setBusinessName(request.getBusinessName());

        if (isValid(request.getBusinessType()))
            trader.setBusinessType(request.getBusinessType());

        if (isValid(request.getTradingArea()))
            trader.setTradingArea(request.getTradingArea());

        if (isValid(request.getBusinessDescription()))
            trader.setBusinessDescription(request.getBusinessDescription());

        if (request.getHasBusinessRegistration() != null) {
            trader.setHasBusinessRegistration(request.getHasBusinessRegistration());

            // Clear CIPC when unregistering
            if (Boolean.FALSE.equals(request.getHasBusinessRegistration())) {
                trader.setCipcNumber(null);
            }
        }

        if (incomingCipc)
            trader.setCipcNumber(request.getCipcNumber());

        if (request.getHasBankAccount() != null)
            trader.setHasBankAccount(request.getHasBankAccount());

        if (isValid(request.getBankName()))
            trader.setBankName(request.getBankName());

        TraderProfile updated = repository.save(trader);
        log.info("Trader profile updated successfully for ID: {}", id);

        return TraderProfileResponse.from(updated);
    }


    //helper method to reject null and blank strings, update only what is provided
    private boolean isValid(String value){
        return value != null && !value.isBlank();
    }

    @Override
    @Transactional()
    public TraderProfileResponse updateStatus(Long id, TraderProfile.ProfileStatus newStatus) {
        log.info("Updating status for trader ID: {} to {}", id, newStatus);
        TraderProfile trader = findByIdOrThrow(id);
        trader.setStatus(newStatus);
        TraderProfile updated = repository.save(trader);
        return TraderProfileResponse.from(updated);
    }

    @Override
    @Transactional()
    public void deleteProfile(Long id) {
        log.info("Deleting trader profile with ID: {}", id);
        TraderProfile trader = findByIdOrThrow(id);
        repository.delete(trader);
        log.info("Trader profile deleted for ID: {}", id);
    }
}
