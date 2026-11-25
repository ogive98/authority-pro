package com.authoritypro.platform.access.application;

/** The scoped access store could not be read or a refusal could not be recorded. */
public final class AccessStoreUnavailableException extends RuntimeException {

    public AccessStoreUnavailableException() {
        super("ACCESS_STORE_UNAVAILABLE");
    }
}
