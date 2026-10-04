package com.kasibridge.auth.dto;

import com.kasibridge.auth.security.Role;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;

    private Long userId;
    private String username;
    private String orgId;

    private Set<Role> roles;
}