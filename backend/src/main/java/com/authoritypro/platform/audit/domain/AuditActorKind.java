package com.authoritypro.platform.audit.domain;

public enum AuditActorKind {
    USER("user"),
    OPERATOR("operator"),
    SERVICE("service");

    private final String databaseValue;

    AuditActorKind(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static AuditActorKind fromDatabase(String value) {
        for (AuditActorKind kind : values()) {
            if (kind.databaseValue.equals(value)) return kind;
        }
        throw new IllegalStateException("Unknown stored actor kind");
    }
}
