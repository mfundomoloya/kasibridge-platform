package com.kasibridge.auth.config;

import com.kasibridge.auth.entity.AppUser;
import com.kasibridge.auth.repository.AppUserRepository;
import com.kasibridge.auth.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevUserSeeder implements CommandLineRunner {

    private static final String DEVELOPMENT_PASSWORD =
            "password";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUserIfMissing(
                "analyst",
                "kasibridge-internal",
                Role.ROLE_ANALYST
        );

        createUserIfMissing(
                "trader",
                "trader-org-001",
                Role.ROLE_TRADER
        );

        createUserIfMissing(
                "bidder-a",
                "trader-org-a",
                Role.ROLE_TRADER
        );

        createUserIfMissing(
                "bidder-b",
                "trader-org-b",
                Role.ROLE_TRADER
        );

        createUserIfMissing(
                "bidder-c",
                "trader-org-c",
                Role.ROLE_TRADER
        );

        createUserIfMissing(
                "bidder-d",
                "trader-org-d",
                Role.ROLE_TRADER
        );

        createUserIfMissing(
                "spec",
                "buyer-org-001",
                Role.ROLE_SPECIFICATION_OFFICER
        );

        createUserIfMissing(
                "spec-two",
                "buyer-org-001",
                Role.ROLE_SPECIFICATION_OFFICER
        );

        createUserIfMissing(
                "evaluator",
                "buyer-org-001",
                Role.ROLE_EVALUATOR
        );

        createUserIfMissing(
                "adjudicator",
                "buyer-org-001",
                Role.ROLE_ADJUDICATOR
        );

        createUserIfMissing(
                "adjudicator-two",
                "buyer-org-001",
                Role.ROLE_ADJUDICATOR
        );

        createUserIfMissing(
                "admin",
                "kasibridge-internal",
                Role.ROLE_PLATFORM_ADMIN
        );

        createUserIfMissing(
                "procurement-system",
                "kasibridge-internal",
                Role.ROLE_SYSTEM
        );
    }

    private void createUserIfMissing(
            String username,
            String organisationId,
            Role role
    ) {
        if (appUserRepository.existsByUsername(username)) {
            return;
        }

        appUserRepository.save(
                AppUser.create(
                        username,
                        passwordEncoder.encode(
                                DEVELOPMENT_PASSWORD
                        ),
                        organisationId,
                        Set.of(role)
                )
        );
    }
}