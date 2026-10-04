package com.kasibridge.procurement.config;

import com.kasibridge.security.JwtAuthenticationFilter;
import com.kasibridge.security.JwtPublicKeyProvider;
import com.kasibridge.security.JwtTokenValidator;
import com.kasibridge.security.KasiBridgeJwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableConfigurationProperties(KasiBridgeJwtProperties.class)
public class JwtCommonConfig {

    @Bean
    public JwtPublicKeyProvider jwtPublicKeyProvider(
            ResourceLoader resourceLoader,
            KasiBridgeJwtProperties properties
    ) {
        return new JwtPublicKeyProvider(resourceLoader, properties);
    }

    @Bean
    public JwtTokenValidator jwtTokenValidator(
            JwtPublicKeyProvider jwtPublicKeyProvider
    ) {
        return new JwtTokenValidator(jwtPublicKeyProvider);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenValidator jwtTokenValidator
    ) {
        return new JwtAuthenticationFilter(jwtTokenValidator);
    }

}
