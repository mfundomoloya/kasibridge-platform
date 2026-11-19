package com.kasibridge.procurement.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppProviderConfigurationValidator {

    private final boolean mockEnabled;
    private final boolean metaEnabled;
    private final boolean dispatchEnabled;

    public WhatsAppProviderConfigurationValidator(
            @Value(
                    "${kasibridge.notifications."
                            + "mock-whatsapp.enabled:false}"
            )
            boolean mockEnabled,
            @Value(
                    "${kasibridge.notifications."
                            + "whatsapp.enabled:true}"
            )
            boolean metaEnabled,
            @Value(
                    "${kasibridge.notifications."
                            + "dispatch.enabled:false}"
            )
            boolean dispatchEnabled
    ) {
        this.mockEnabled = mockEnabled;
        this.metaEnabled = metaEnabled;
        this.dispatchEnabled = dispatchEnabled;
    }

    @PostConstruct
    void validate() {
        if (mockEnabled && metaEnabled) {
            throw new IllegalStateException(
                    "Mock and Meta WhatsApp providers "
                            + "cannot both be enabled."
            );
        }

        if (
                dispatchEnabled
                        && !mockEnabled
                        && !metaEnabled
        ) {
            throw new IllegalStateException(
                    "WhatsApp notification dispatch "
                            + "requires exactly one provider."
            );
        }
    }
}