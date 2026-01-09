package de.sty.fileserv.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LockManagerTest {

    @Test
    void createsAndFindsActiveLock() {
        LockManager lm = new LockManager();
        var lock = lm.createOrRefreshExclusiveLock("/a.txt", "me", 120, 0);

        assertThat(lock.token()).startsWith("opaquelocktoken:");
        assertThat(lm.getActiveLock("/a.txt")).isPresent();
    }

    @Test
    void refreshReusesToken() {
        LockManager lm = new LockManager();
        var l1 = lm.createOrRefreshExclusiveLock("/a.txt", "me", 120, 0);
        var l2 = lm.createOrRefreshExclusiveLock("/a.txt", "me", 120, 0);

        assertThat(l2.token()).isEqualTo(l1.token());
    }

    @Test
    void expiredLockIsRemoved() {
        LockManager lm = new LockManager();
        // create with short timeout, then force expire by direct wait-free trick:
        // (we can't easily time-travel; so we use a tiny timeout and sleep)
        lm.createOrRefreshExclusiveLock("/b.txt", "me", 1, 0);
        try { Thread.sleep(1200); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(lm.getActiveLock("/a.txt")).isEmpty();
    }

    @Test
    void unlockRequiresMatchingPath() {
        LockManager lm = new LockManager();
        var lock = lm.createOrRefreshExclusiveLock("/a.txt", "me", 120, 0);

        assertThat(lm.unlock(lock.token(), "/other.txt")).isFalse();
        assertThat(lm.unlock(lock.token(), "/a.txt")).isTrue();
        assertThat(lm.getActiveLock("/a.txt")).isEmpty();
    }
}
