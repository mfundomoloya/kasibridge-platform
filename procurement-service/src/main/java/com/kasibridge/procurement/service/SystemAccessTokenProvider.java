package com.kasibridge.procurement.service;

public interface SystemAccessTokenProvider {

    String getAccessToken();
    void invalidateToken();
}
