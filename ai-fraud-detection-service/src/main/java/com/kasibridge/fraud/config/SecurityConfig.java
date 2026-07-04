package com.kasibridge.fraud.config;


import com.kasibridge.security.KasiBridgeJwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KasiBridgeJwtProperties.class)
public class SecurityConfig {
}
