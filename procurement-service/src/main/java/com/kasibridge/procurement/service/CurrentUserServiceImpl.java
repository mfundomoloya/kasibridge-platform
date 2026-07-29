package com.kasibridge.procurement.service;

import com.kasibridge.security.KasiBridgeUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {
    @Override
    public Long getCurrentUserId() {
        return getPrincipal().userId();
    }

    @Override
    public String getCurrentUserName() {
        return getPrincipal().username();
    }

    @Override
    public String getCurrentOrgId() {
        return getPrincipal().orgId();
    }

    private KasiBridgeUserPrincipal getPrincipal(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || authentication.getPrincipal() == null){
            throw new IllegalStateException("Authenticated user not found");
        }

        Object principal = authentication.getPrincipal();

        if(!(principal instanceof KasiBridgeUserPrincipal kasiBridgeUserPrincipal)){
            throw new IllegalStateException("Invalid authenticated principal");
        }

        return kasiBridgeUserPrincipal;
    }
}
