package com.kasibridge.security;


import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class JwtPublicKeyProvider {

    private final ResourceLoader resourceLoader;
    private final KasiBridgeJwtProperties properties;

    public JwtPublicKeyProvider(
            ResourceLoader resourceLoader,
            KasiBridgeJwtProperties properties
    ) {
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    public PublicKey getPublicKey() {
        try {
            Resource resource = resourceLoader.getResource(properties.getPublicKeyLocation());

            String pem = new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String cleanedPem = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(cleanedPem);

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(keySpec);

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load JWT public key", ex);
        }
    }

}
