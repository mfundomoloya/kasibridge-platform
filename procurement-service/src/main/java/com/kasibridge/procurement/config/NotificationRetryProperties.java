package com.kasibridge.procurement.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(
        prefix = "kasibridge.notifications.retry"
)
@Validated
public record NotificationRetryProperties(

        @Min(
                value = 1,
                message = "Notification retry base delay "
                        + "must be positive."
        )
        long baseDelaySeconds,

        @Min(
                value = 1,
                message = "Notification retry maximum delay "
                        + "must be positive."
        )
        long maxDelaySeconds
) {

    public NotificationRetryProperties {
        if (maxDelaySeconds < baseDelaySeconds) {
            throw new IllegalArgumentException(
                    "Notification retry maximum delay "
                            + "must be greater than or equal "
                            + "to the base delay."
            );
        }
    }
}