package com.kasibridge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.PublicKey;
import java.util.List;

public class JwtTokenValidator {

    private final JwtPublicKeyProvider publicKeyProvider;

    public JwtTokenValidator(JwtPublicKeyProvider publicKeyProvider) {
        this.publicKeyProvider = publicKeyProvider;
    }

    public KasiBridgeUserPrincipal validateAndExtractPrincipal(String token) {

        PublicKey publicKey = publicKeyProvider.getPublicKey();

        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String orgId = claims.get("orgId", String.class);

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);

        return new KasiBridgeUserPrincipal(
                userId,
                username,
                orgId,
                roles
        );
    }

}
