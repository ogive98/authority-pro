package com.authoritypro.platform.runtime.application;

/** Bounded rejection reason; never carries descriptor content, paths or parser output. */
public final class ManifestValidationException extends RuntimeException {

    public enum Code {
        EMPTY_CATALOGUE,
        OVERSIZED,
        MALFORMED_JSON,
        NOT_OBJECT,
        FIELD_MISSING,
        FIELD_UNKNOWN,
        CONSTANT_INVALID,
        TYPE_INVALID,
        VALUE_INVALID,
        ARRAY_TOO_LONG,
        DUPLICATE_MODULE,
        DUPLICATE_DEPENDENCY,
        DUPLICATE_CAPABILITY,
        DUPLICATE_UI,
        DEPENDENCY_SELF,
        DEPENDENCY_MISSING,
        DEPENDENCY_VERSION_MISMATCH,
        DEPENDENCY_CYCLE,
        CAPABILITY_AMBIGUOUS,
        CAPABILITY_MISSING,
        CAPABILITY_VERSION_MISMATCH,
        CAPABILITY_SELF_REQUIRED,
        PERMISSION_DUPLICATE,
        RESOURCE_DUPLICATE,
        UI_PERMISSION_UNKNOWN,
        ROUTE_UNRESOLVED
    }

    private final Code code;
    private final String descriptor;

    public ManifestValidationException(Code code, String descriptor) {
        super(code.name());
        this.code = code;
        this.descriptor = descriptor;
    }

    public Code code() {
        return code;
    }

    /** Compiled resource name only; never an absolute path. */
    public String descriptor() {
        return descriptor;
    }
}
