package com.kasibridge.procurement.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRetryPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            NotificationRetryConfig.class
                    );

    @Test
    void properties_shouldBindConfiguredRetryDelays() {
        contextRunner
                .withPropertyValues(
                        "kasibridge.notifications.retry"
                                + ".base-delay-seconds=15",
                        "kasibridge.notifications.retry"
                                + ".max-delay-seconds=900"
                )
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed();

                    assertThat(context)
                            .hasSingleBean(
                                    NotificationRetryProperties.class
                            );

                    NotificationRetryProperties properties =
                            context.getBean(
                                    NotificationRetryProperties.class
                            );

                    assertThat(
                            properties.baseDelaySeconds()
                    ).isEqualTo(15L);

                    assertThat(
                            properties.maxDelaySeconds()
                    ).isEqualTo(900L);
                });
    }

    @Test
    void properties_shouldRejectMaximumDelayBelowBaseDelay() {
        contextRunner
                .withPropertyValues(
                        "kasibridge.notifications.retry"
                                + ".base-delay-seconds=60",
                        "kasibridge.notifications.retry"
                                + ".max-delay-seconds=30"
                )
                .run(context -> {
                    assertThat(context)
                            .hasFailed();

                    assertThat(
                            context.getStartupFailure()
                    )
                            .hasRootCauseInstanceOf(
                                    IllegalArgumentException.class
                            )
                            .hasRootCauseMessage(
                                    "Notification retry maximum delay "
                                            + "must be greater than or equal "
                                            + "to the base delay."
                            );
                });
    }

    @Test
    void properties_shouldRejectNonPositiveBaseDelay() {
        contextRunner
                .withPropertyValues(
                        "kasibridge.notifications.retry"
                                + ".base-delay-seconds=0",
                        "kasibridge.notifications.retry"
                                + ".max-delay-seconds=300"
                )
                .run(context -> {
                    assertThat(context)
                            .hasFailed();

                    assertThat(
                            context.getStartupFailure()
                    ).isNotNull();
                });
    }

    @Test
    void properties_shouldRejectNonPositiveMaximumDelay() {
        contextRunner
                .withPropertyValues(
                        "kasibridge.notifications.retry"
                                + ".base-delay-seconds=60",
                        "kasibridge.notifications.retry"
                                + ".max-delay-seconds=0"
                )
                .run(context -> {
                    assertThat(context)
                            .hasFailed();

                    assertThat(
                            context.getStartupFailure()
                    ).isNotNull();
                });
    }
}