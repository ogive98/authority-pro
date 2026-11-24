package com.authoritypro.platform.runtime.infrastructure.manifest;

import com.authoritypro.platform.runtime.application.ModuleCatalogue;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/** Descriptor health only: a valid catalogue does not claim enablement or licensing. */
@Component("moduleCatalogue")
public final class ModuleCatalogueHealthIndicator implements HealthIndicator {
    private final ModuleCatalogue catalogue;

    public ModuleCatalogueHealthIndicator(ModuleCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public Health health() {
        return catalogue.available()
                ? Health.up().build()
                : Health.down().build();
    }
}
