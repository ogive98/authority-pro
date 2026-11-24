package com.authoritypro.platform.runtime.application;

/** Opaque build identity injected by the build; it is not a manifest field. */
public record BuildRevision(String value) {

    public BuildRevision {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("BUILD_REVISION_REQUIRED");
    }
}
