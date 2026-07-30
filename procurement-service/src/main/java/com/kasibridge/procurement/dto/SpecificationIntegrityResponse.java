package com.kasibridge.procurement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpecificationIntegrityResponse {

    private Long tenderId;
    private String tenderReference;
    private String storedHash;
    private String currentHash;
    private boolean integrityValid;
    private String message;
}
