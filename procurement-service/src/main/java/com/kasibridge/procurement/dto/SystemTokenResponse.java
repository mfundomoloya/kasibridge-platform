package com.kasibridge.procurement.dto;

import lombok.Data;

import java.util.Set;

@Data
public class SystemTokenResponse {

    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private String orgId;
    private Set<String> roles;
}
