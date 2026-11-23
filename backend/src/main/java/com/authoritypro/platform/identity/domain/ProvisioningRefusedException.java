package com.authoritypro.platform.identity.domain;

/** Opaque bootstrap refusal: no detail about existing state or the failing step is exposed. */
public final class ProvisioningRefusedException extends RuntimeException {
    public ProvisioningRefusedException() {
        super("PROVISIONING_REFUSED");
    }
}
