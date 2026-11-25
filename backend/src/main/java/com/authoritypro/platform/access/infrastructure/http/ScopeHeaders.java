package com.authoritypro.platform.access.infrastructure.http;

/** Untrusted selector headers; validated values are also exposed as request attributes. */
public final class ScopeHeaders {
    public static final String TENANT = "X-Authority-Tenant";
    public static final String COMPANY = "X-Authority-Company";
    public static final String SITE = "X-Authority-Site";

    private ScopeHeaders() { }
}
