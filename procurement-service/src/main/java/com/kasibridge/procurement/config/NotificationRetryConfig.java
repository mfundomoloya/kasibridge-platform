package com.kasibridge.procurement.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(
        NotificationRetryProperties.class
)
public class NotificationRetryConfig {
}