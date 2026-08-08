package com.kasibridge.trader_profile.controller;

import com.kasibridge.trader_profile.dto.CreateTraderRequest;
import com.kasibridge.trader_profile.dto.TraderProfileResponse;
import com.kasibridge.trader_profile.dto.UpdateTraderRequest;
import com.kasibridge.trader_profile.entity.TraderProfile;
import com.kasibridge.trader_profile.service.TraderProfileService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/traders")
public class TraderProfileController {

    //inject the service
    TraderProfileService service;

    public TraderProfileController(TraderProfileService service) {
        this.service = service;
    }

    // api/v1/traders
    @PostMapping
    public ResponseEntity<TraderProfileResponse> createProfile(@Valid
                                                               @RequestBody CreateTraderRequest request){
        log.info("POST /api/v1/traders - registering trader: {}", request.getPhoneNumber());
        TraderProfileResponse response = service.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // api/v1/traders
    @GetMapping
    public ResponseEntity<List<TraderProfileResponse>> getAllProfiles(){
        log.info("GET /api/v1/traders - fetching all profiles");
        return ResponseEntity.ok(service.getAllProfiles());
    }

    // api/v1/traders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TraderProfileResponse> getProfileById(@PathVariable("id") Long id){
        log.info("GET /api/v1/traders/{} - fetching profile", id);
                return ResponseEntity.ok(service.getProfileById(id));
    }

    // api/v1/traders/phone/{phoneNumber}
    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<TraderProfileResponse> getProfileByPhone(@PathVariable("phoneNumber") String phoneNumber){
        log.info("GET /api/v1/traders/phone/{} - fetching profile", phoneNumber);
        return ResponseEntity.ok(service.getProfileByPhone(phoneNumber));
    }

    // api/v1/traders/area/{tradingArea}
    @GetMapping("/area/{tradingArea}")
    public ResponseEntity<List<TraderProfileResponse>> getProfileByArea(@PathVariable("tradingArea") String tradingArea){
        log.info("GET - /api/v1/traders/area/{}", tradingArea);
        return ResponseEntity.ok(service.getProfilesByArea(tradingArea));
    }

    // api/v1/traders/status/{status}
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TraderProfileResponse>> getProfilesByStatus(@PathVariable("status") TraderProfile.ProfileStatus status){
        log.info("GET - /api/v1/traders/status/{} - fetching profiles", status);
        return ResponseEntity.ok(service.getProfilesByStatus(status));
    }

    // api/v1/traders/channel/{channel}
    @GetMapping("/channel/{channel}")
    public ResponseEntity<List<TraderProfileResponse>> getProfilesByChannel(@PathVariable("channel") TraderProfile.OnboardingChannel channel){
        log.info("GET - /api/v1/traders/channel/{} - fetching profiles", channel);
        return ResponseEntity.ok(service.getProfilesByChannel(channel));
    }

    // api/v1/traders/{id}
    @PutMapping("/{id}")
    public ResponseEntity<TraderProfileResponse> updateProfile(@PathVariable("id") Long id,
                                                               @RequestBody UpdateTraderRequest request){
        log.info("PUT - /api/v1/traders/{} - updating profile", id);
        return ResponseEntity.ok(service.updateProfile(id, request));
    }

    // api/v1/traders/{id}/status
    // Change a trader's status (ACTIVE, SUSPENDED, INACTIVE)
    @PatchMapping("/{id}/status")
    public ResponseEntity<TraderProfileResponse> updateStatus(@PathVariable("id") Long id,
                                                              @RequestParam TraderProfile.ProfileStatus status){
        log.info("PATCH - /api/v1/traders/{}/status - new status: {}", id, status);
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    // api/v1/traders/{id}
    //delete a trader profile
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable("id") Long id){
        log.info("DELETE - /api/v1/traders/{} - deleting profile:", id);
        service.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<TraderProfileResponse> getProfileByUserId(@PathVariable("userId") Long userId) {
        log.info("GET /api/v1/traders/user/{} - fetching profile", userId);

        return ResponseEntity.ok(service.getProfileByUserId(userId));
    }

    @PatchMapping("/{id}/link-user/{userId}")
    public ResponseEntity<TraderProfileResponse> linkUser(@PathVariable("id") Long id, @PathVariable("userId") Long userId)
    {
        log.info("PATCH /api/v1/traders/{}/link-user/{} - linking trader profile to auth user", id, userId);

        return ResponseEntity.ok(service.linkUser(id, userId));}
}
