package com.authoritypro.platform.identity.infrastructure.session;

import com.authoritypro.platform.identity.application.IdentityAccessException;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Enforces the per-principal session cap inside the transaction that inserts the new
 * session row. A transaction-scoped advisory lock serializes concurrent logins for the
 * same principal; different principals only contend on a hash collision.
 */
public final class CappedSessionRepository implements FindByIndexNameSessionRepository<Session> {
    public static final int MAX_CONCURRENT = 3;
    private static final Duration LOCK_WAIT = Duration.ofSeconds(2);
    private static final Comparator<Session> AGE = Comparator
            .comparing(Session::getLastAccessedTime)
            .thenComparing(Session::getCreationTime)
            .thenComparing(Session::getId);

    private final JdbcTemplate jdbc;
    private final FindByIndexNameSessionRepository<Session> delegate;
    private final TransactionOperations transactions;

    @SuppressWarnings("unchecked")
    public CappedSessionRepository(DataSource dataSource,
            FindByIndexNameSessionRepository<? extends Session> delegate, TransactionOperations transactions) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.delegate = (FindByIndexNameSessionRepository<Session>) delegate;
        this.transactions = transactions;
    }

    @Override
    public Session createSession() {
        return delegate.createSession();
    }

    @Override
    public Session findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }

    @Override
    public Map<String, Session> findByPrincipalName(String principalName) {
        return delegate.findByPrincipalName(principalName);
    }

    @Override
    public Map<String, Session> findByIndexNameAndIndexValue(String indexName, String indexValue) {
        return delegate.findByIndexNameAndIndexValue(indexName, indexValue);
    }

    @Override
    public void save(Session session) {
        String principal = principal(session);
        if (principal == null || delegate.findById(session.getId()) != null) {
            delegate.save(session);
            return;
        }
        transactions.executeWithoutResult(status -> {
            lock(principal);
            if (delegate.findById(session.getId()) != null) {
                delegate.save(session);
                return;
            }
            Map<String, Session> existing = delegate.findByPrincipalName(principal);
            while (existing.size() > MAX_CONCURRENT) {
                Session oldest = existing.values().stream().min(AGE).orElseThrow();
                delegate.deleteById(oldest.getId());
                existing.remove(oldest.getId());
            }
            if (existing.size() == MAX_CONCURRENT) {
                Session oldest = existing.values().stream().min(AGE).orElseThrow();
                if (AGE.compare(session, oldest) < 0) return;
                delegate.deleteById(oldest.getId());
            }
            delegate.save(session);
        });
    }

    private static String principal(Session session) {
        Object value = session.getAttribute(PRINCIPAL_NAME_INDEX_NAME);
        return value instanceof String name && !name.isBlank() ? name : null;
    }

    private void lock(String principal) {
        long deadline = System.nanoTime() + LOCK_WAIT.toNanos();
        do {
            Boolean acquired = jdbc.queryForObject(
                    "SELECT pg_try_advisory_xact_lock(hashtextextended(?, 0))", Boolean.class, principal);
            if (Boolean.TRUE.equals(acquired)) return;
            try {
                Thread.sleep(10);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw IdentityAccessException.storeUnavailable(interrupted);
            }
        } while (System.nanoTime() < deadline);
        throw IdentityAccessException.storeUnavailable(new IllegalStateException("SESSION_CAP_LOCK_TIMEOUT"));
    }
}