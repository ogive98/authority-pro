package com.authoritypro.platform.access.application;

import java.util.UUID;

/** Authorization decision for the protected compiled inventory; deny-by-default. */
public interface InventoryAccessPolicy {

    enum Decision { GRANTED, DENIED }

    Decision decide(UUID userId);
}
