package com.authoritypro.platform.identity.application;

/** Transport-neutral refusal categories; adapters map them to stable RFC 9457 codes. */
public final class IdentityAccessException extends RuntimeException {
    public enum Kind { AUTHENTICATION_REFUSED, AUTHENTICATION_THROTTLED, STORE_UNAVAILABLE }

    private final Kind kind;

    private IdentityAccessException(Kind kind, Throwable cause) {
        super(kind.name(), cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static IdentityAccessException refused() {
        return new IdentityAccessException(Kind.AUTHENTICATION_REFUSED, null);
    }

    public static IdentityAccessException throttled() {
        return new IdentityAccessException(Kind.AUTHENTICATION_THROTTLED, null);
    }

    public static IdentityAccessException storeUnavailable(Throwable cause) {
        return new IdentityAccessException(Kind.STORE_UNAVAILABLE, cause);
    }
}
