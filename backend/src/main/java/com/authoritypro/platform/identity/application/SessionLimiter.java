package com.authoritypro.platform.identity.application;

/** Keeps at most the approved number of concurrent sessions by invalidating the oldest. */
public interface SessionLimiter {
    void makeRoom(String userId);
}
