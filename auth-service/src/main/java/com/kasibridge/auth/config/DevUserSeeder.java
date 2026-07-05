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

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!appUserRepository.existsByUsername("analyst")) {
            appUserRepository.save(
                    AppUser.create(
                            "analyst",
                            passwordEncoder.encode("password"),
                            "kasibridge-internal",
                            Set.of(Role.ROLE_ANALYST)
                    )
            );
        }

        if (!appUserRepository.existsByUsername("admin")) {
            appUserRepository.save(
                    AppUser.create(
                            "admin",
                            passwordEncoder.encode("password"),
                            "kasibridge-internal",
                            Set.of(Role.ROLE_PLATFORM_ADMIN)
                    )
            );
        }

        if (!appUserRepository.existsByUsername("system")) {
            appUserRepository.save(
                    AppUser.create(
                            "system",
                            passwordEncoder.encode("password"),
                            "kasibridge-system",
                            Set.of(Role.ROLE_SYSTEM)
                    )
            );
        }
    }
}