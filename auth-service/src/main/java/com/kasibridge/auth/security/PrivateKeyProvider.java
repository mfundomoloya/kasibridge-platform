package com.kasibridge.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
public class PrivateKeyProvider {

    private final ResourceLoader resourceLoader;

    @Value("${kasibridge.security.jwt.private-key-location}")
    private String privateKeyLocation;

    public PrivateKeyProvider(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public PrivateKey getPrivateKey() {
        try {
            Resource resource = resourceLoader.getResource(privateKeyLocation);

            String pem = new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String cleanedPem = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(cleanedPem);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(keySpec);

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load JWT private key", ex);
        }
    }
}