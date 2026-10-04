package com.kasibridge.auth.service;

import com.kasibridge.auth.dto.LoginRequest;
import com.kasibridge.auth.dto.LoginResponse;
import com.kasibridge.auth.entity.AppUser;
import com.kasibridge.auth.repository.AppUserRepository;
import com.kasibridge.auth.security.JwtIssuerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final JwtIssuerService jwtIssuerService;

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        String token = jwtIssuerService.issueToken(user);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .orgId(user.getOrgId())
                .roles(user.getRoles())
                .build();
    }
}