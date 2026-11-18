package com.authoritypro.platform.runtime.infrastructure.http;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/** Readiness is intentionally closed until the protected nucleus is implemented. */
@Component("skeleton")
public final class NucleusHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.outOfService().build();
    }
}
