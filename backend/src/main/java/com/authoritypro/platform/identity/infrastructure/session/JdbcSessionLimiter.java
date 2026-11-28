package com.authoritypro.platform.identity.infrastructure.session;

import com.authoritypro.platform.identity.application.IdentityAccessException;
import com.authoritypro.platform.identity.application.SessionLimiter;
import java.util.Comparator;
import java.util.Map;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

/**
 * D2 best-effort pre-eviction before authentication completes; the authoritative cap is
 * enforced atomically by {@link CappedSessionRepository} in the insert transaction.
 */
public final class JdbcSessionLimiter implements SessionLimiter {
    public static final int MAX_CONCURRENT = 3;

    private final FindByIndexNameSessionRepository<? extends Session> repository;

    public JdbcSessionLimiter(FindByIndexNameSessionRepository<? extends Session> repository) {
        if (repository == null) throw new IllegalArgumentException("SESSION_REPOSITORY_REQUIRED");
        this.repository = repository;
    }

    @Override
    public void makeRoom(String userId) {
        if (userId == null || userId.isBlank()) throw IdentityAccessException.refused();
        try {
            Map<String, ? extends Session> sessions = repository.findByPrincipalName(userId);
            int excess = sessions.size() - (MAX_CONCURRENT - 1);
            if (excess <= 0) return;
            sessions.values().stream()
                    .sorted(Comparator.comparing(Session::getCreationTime))
                    .limit(excess)
                    .forEach(session -> repository.deleteById(session.getId()));
        } catch (RuntimeException failure) {
            throw IdentityAccessException.storeUnavailable(failure);
        }
    }
}
