package de.sty.fileserv.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LockManager {

    private static final Logger LOG = LoggerFactory.getLogger(LockManager.class);

    private final Map<String, Lock> byPath = new ConcurrentHashMap<>();
    private final Map<String, Lock> byToken = new ConcurrentHashMap<>();

    public Optional<Lock> getActiveLock(String path) {
        Lock l = byPath.get(path);
        if (l == null) {
            LOG.debug("getActiveLock: no lock for path={}", path);
            return Optional.empty();
        }
        if (Instant.now().isAfter(l.expiresAt())) {
            LOG.debug("getActiveLock: lock expired for path={} token={} (expired at {})", l.path(), l.token(), l.expiresAt());
            remove(l);
            return Optional.empty();
        }
        LOG.debug("getActiveLock: active lock for path={} token={} expires={}", l.path(), l.token(), l.expiresAt());
        return Optional.of(l);
    }

    public Lock createOrRefreshExclusiveLock(String path, String owner, long timeoutSeconds, int depth) {
        // single exclusive lock per resource
        Lock existing = getActiveLock(path).orElse(null);
        String token = (existing != null) ? existing.token() : "opaquelocktoken:" + UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(Math.max(30, timeoutSeconds)); // enforce min
        Lock l = new Lock(path, token, owner, expiresAt, depth);

        if (existing != null) {
            LOG.debug("createOrRefreshExclusiveLock: refreshing lock for path={} token={} newExpires={}", path, token, expiresAt);
            remove(existing);
        } else {
            LOG.debug("createOrRefreshExclusiveLock: creating new lock for path={} token={} owner={} depth={} timeoutSeconds={} expires={}",
                    path, token, owner, depth, timeoutSeconds, expiresAt);
        }
        byPath.put(path, l);
        byToken.put(token, l);
        return l;
    }

    public boolean unlock(String token, String path) {
        Lock l = byToken.get(token);
        if (l == null) {
            LOG.debug("unlock: no lock for token={} (path requested={})", token, path);
            return false;
        }
        if (!l.path().equals(path)) {
            LOG.debug("unlock: path mismatch for token={} requestedPath={} actualPath={}", token, path, l.path());
            return false;
        }
        remove(l);
        LOG.debug("unlock: unlocked path={} token={}", path, token);
        return true;
    }

    private void remove(Lock l) {
        byPath.remove(l.path(), l);
        byToken.remove(l.token(), l);
        LOG.debug("remove: removed lock path={} token={}", l.path(), l.token());
    }

    public record Lock(String path, String token, String owner, Instant expiresAt, int depth) {
    }
}
