package com.kasibridge.security;

import java.util.List;

public record KasiBridgeUserPrincipal (
        Long userId,
        String username,
        String orgId,
        List<String> roles
){
}
