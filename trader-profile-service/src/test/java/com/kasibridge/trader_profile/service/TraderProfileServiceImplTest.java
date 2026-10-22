package com.kasibridge.trader_profile.service;

import com.kasibridge.trader_profile.dto.TraderProfileResponse;
import com.kasibridge.trader_profile.entity.TraderProfile;
import com.kasibridge.trader_profile.repository.TraderProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraderProfileServiceImplTest {

    private static final String BUSINESS_TYPE =
            "Road Rehabilitation and Maintenance";

    private static final String TRADING_AREA =
            "Gauteng";

    @Mock
    private TraderProfileRepository repository;

    @InjectMocks
    private TraderProfileServiceImpl service;

    private TraderProfile validMatchingProfile;

    @BeforeEach
    void setUp() {
        validMatchingProfile =
                buildProfile(
                        2L,
                        8L,
                        "Sifiso Magagula",
                        "+27610000001",
                        BUSINESS_TYPE,
                        TRADING_AREA,
                        TraderProfile.ProfileStatus.ACTIVE
                );
    }

    @Test
    void findActiveMatchingProfiles_shouldReturnEligibleActiveTrader() {
        when(
                repository
                        .findByBusinessTypeIgnoreCaseAndTradingAreaIgnoreCaseAndStatus(
                                BUSINESS_TYPE,
                                TRADING_AREA,
                                TraderProfile.ProfileStatus.ACTIVE
                        )
        ).thenReturn(
                List.of(
                        validMatchingProfile
                )
        );

        List<TraderProfileResponse> matches =
                service.findActiveMatchingProfiles(
                        BUSINESS_TYPE,
                        TRADING_AREA
                );

        assertEquals(
                1,
                matches.size()
        );

        TraderProfileResponse match =
                matches.getFirst();

        assertEquals(
                2L,
                match.getId()
        );

        assertEquals(
                8L,
                match.getUserId()
        );

        assertEquals(
                "Sifiso Magagula",
                match.getFullName()
        );

        assertEquals(
                "+27610000001",
                match.getPhoneNumber()
        );

        assertEquals(
                BUSINESS_TYPE,
                match.getBusinessType()
        );

        assertEquals(
                TRADING_AREA,
                match.getTradingArea()
        );

        assertEquals(
                TraderProfile.ProfileStatus.ACTIVE,
                match.getStatus()
        );

        verify(repository)
                .findByBusinessTypeIgnoreCaseAndTradingAreaIgnoreCaseAndStatus(
                        BUSINESS_TYPE,
                        TRADING_AREA,
                        TraderProfile.ProfileStatus.ACTIVE
                );
    }

    @Test
    void findActiveMatchingProfiles_shouldTrimRequestValues() {
        when(
                repository
                        .findByBusinessTypeIgnoreCaseAndTradingAreaIgnoreCaseAndStatus(
                                BUSINESS_TYPE,
                                TRADING_AREA,
                                TraderProfile.ProfileStatus.ACTIVE
                        )
        ).thenReturn(
                List.of(
                        validMatchingProfile
                )
        );

        List<TraderProfileResponse> matches =
                service.findActiveMatchingProfiles(
                        "  " + BUSINESS_TYPE + "  ",
                        "  " + TRADING_AREA + "  "
                );

        assertEquals(
                1,
                matches.size()
        );

        verify(repository)
                .findByBusinessTypeIgnoreCaseAndTradingAreaIgnoreCaseAndStatus(
                        BUSINESS_TYPE,
                        TRADING_AREA,
                        TraderProfile.ProfileStatus.ACTIVE
                );
    }

    @Test
    void findActiveMatchingProfiles_shouldExcludeProfileWithoutUserId() {
        TraderProfile profileWithoutUserId =
                buildProfile(
                        3L,
                        null,
                        "Unlinked Trader",
                        "+27610000002",
                        BUSINESS_TYPE,
                        TRADING_AREA,
                        TraderProfile.ProfileStatus.ACTIVE
                );

        when(
                repository
                        .findByBusinessTypeIgnoreCaseAndTradingAreaIgnoreCaseAndStatus(
                                BUSINESS_TYPE,
                                TRADING_AREA,
                                TraderProfile.ProfileStatus.ACTIVE
                        )
        ).thenReturn(
                List.of(
                        validMatchingProfile,
                        profileWithoutUserId
                )
        );

        List<TraderProfileResponse> matches =
                service.findActiveMatchingProfiles(
                        BUSINESS_TYPE,
                        TRADING_AREA
                );

        assertEquals(
                1,
                matches.size()
        );

        assertEquals(
                validMatchingProfile.getId(),
                matches.getFirst().getId()
        );
    }

    @Test
    void findActiveMatchingProfiles_shouldExcludeProfileWithoutPhoneNumber() {
        TraderProfile profileWithoutPhone =
                buildProfile(
                        4L,
                        10L,
                        "Trader Without Phone",
                        null,
                        BUSINESS_TYPE,
                        TRADING_AREA,
                        TraderProfile.ProfileStatus.ACTIVE
                );

        TraderProfile profileWithBlankPhone =
                buildProfile(
                        5L,
                        11L,
                        "Trader With Blank Phone",
                        "   ",
                        BUSINESS_TYPE,
                        TRADING_AREA,
                        TraderProfile.ProfileStatus.ACTIVE
                );

        when(
                repository
                        .findByBusinessTypeIgnoreCaseAndTradingAreaIgnoreCaseAndStatus(
                                BUSINESS_TYPE,
                                TRADING_AREA,
                                TraderProfile.ProfileStatus.ACTIVE
                        )
        ).thenReturn(
                List.of(
                        validMatchingProfile,
                        profileWithoutPhone,
                        profileWithBlankPhone
                )
        );

        List<TraderProfileResponse> matches =
                service.findActiveMatchingProfiles(
                        BUSINESS_TYPE,
                        TRADING_AREA
                );

        assertEquals(
                1,
                matches.size()
        );

        assertEquals(
                validMatchingProfile.getId(),
                matches.getFirst().getId()
        );
    }

    @Test
    void findActiveMatchingProfiles_shouldReturnEmptyListWhenNoProfilesMatch() {
        when(
                repository
                        .findByBusinessTypeIgnoreCaseAndTradingAreaIgnoreCaseAndStatus(
                                BUSINESS_TYPE,
                                TRADING_AREA,
                                TraderProfile.ProfileStatus.ACTIVE
                        )
        ).thenReturn(
                List.of()
        );

        List<TraderProfileResponse> matches =
                service.findActiveMatchingProfiles(
                        BUSINESS_TYPE,
                        TRADING_AREA
                );

        assertEquals(
                0,
                matches.size()
        );
    }

    @Test
    void findActiveMatchingProfiles_shouldRejectBlankBusinessType() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.findActiveMatchingProfiles(
                                        "   ",
                                        TRADING_AREA
                                )
                );

        assertEquals(
                "Business type is required "
                        + "for trader matching.",
                exception.getMessage()
        );
    }

    @Test
    void findActiveMatchingProfiles_shouldRejectBlankTradingArea() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.findActiveMatchingProfiles(
                                        BUSINESS_TYPE,
                                        "   "
                                )
                );

        assertEquals(
                "Trading area is required "
                        + "for trader matching.",
                exception.getMessage()
        );
    }

    private TraderProfile buildProfile(
            Long id,
            Long userId,
            String fullName,
            String phoneNumber,
            String businessType,
            String tradingArea,
            TraderProfile.ProfileStatus status
    ) {
        return TraderProfile.builder()
                .id(id)
                .userId(userId)
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .email(
                        "trader-" + id
                                + "@kasibridge.test"
                )
                .businessName(
                        "Business " + id
                )
                .businessType(businessType)
                .tradingArea(tradingArea)
                .onboardingChannel(
                        TraderProfile
                                .OnboardingChannel
                                .WEB
                )
                .status(status)
                .build();
    }
}