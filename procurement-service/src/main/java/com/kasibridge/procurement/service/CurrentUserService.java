package com.kasibridge.procurement.service;

public interface CurrentUserService {

    Long getCurrentUserId();
    String getCurrentUserName();
    String getCurrentOrgId();
    boolean hasRole(String role);
}
