package com.authoritypro.platform.access.application;

/** The authorization store could not be read or the refusal could not be recorded. */
public final class InventoryStoreUnavailableException extends RuntimeException {

    public InventoryStoreUnavailableException() {
        super("INVENTORY_STORE_UNAVAILABLE");
    }
}
