package com.kasibridge.auth.security;

import com.kasibridge.auth.entity.AppUser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JwtIssuerService {

    private final PrivateKeyProvider privateKeyProvider;

    @Value("${kasibridge.security.jwt.expiration-ms}")
    private long expirationMs;

    public JwtIssuerService(PrivateKeyProvider privateKeyProvider) {
        this.privateKeyProvider = privateKeyProvider;
    }

    public String issueToken(AppUser user) {

        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMs);

        List<String> roles = user.getRoles()
                .stream()
                .map(Enum::name)
                .toList();

        Map<String, Object> claims = Map.of(
                "username", user.getUsername(),
                "orgId", user.getOrgId(),
                "roles", roles,
                "tokenType", "access"
        );

        PrivateKey privateKey = privateKeyProvider.getPrivateKey();

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claims(claims)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}