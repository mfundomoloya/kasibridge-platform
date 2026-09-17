package com.kasibridge.procurement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SystemLoginRequest {

    private String username;
    private String password;
}
