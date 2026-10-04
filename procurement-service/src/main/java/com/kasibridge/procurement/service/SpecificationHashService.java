package com.kasibridge.procurement.service;

import com.kasibridge.procurement.entity.Tender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class SpecificationHashService {

    public String generateHash(Tender tender) {
        String source = String.join("|",
                safe(tender.getTitle()),
                safe(tender.getDescription()),
                safe(tender.getEvaluationCriteria()),
                tender.getBudgetAmount() != null ? tender.getBudgetAmount().toPlainString() : "",
                safe(tender.getBuyerOrgId())
        );

        return sha256(source);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] encodedHash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hexString = new StringBuilder();

            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate specification hash", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

}
