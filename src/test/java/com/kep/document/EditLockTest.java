package com.kep.document;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class EditLockTest {

    @Test
    void acquire_sets_30_min_ttl() {
        EditLock lock = EditLock.acquire(1L, 100L);
        assertThat(lock.getExpiresAt())
            .isAfter(lock.getAcquiredAt().plus(Duration.ofMinutes(29)))
            .isBefore(lock.getAcquiredAt().plus(Duration.ofMinutes(31)));
    }

    @Test
    void isExpired_false_for_fresh_lock() {
        EditLock lock = EditLock.acquire(1L, 100L);
        assertThat(lock.isExpired()).isFalse();
    }

    @Test
    void refresh_resets_ttl_and_holder() throws Exception {
        EditLock old = EditLock.acquire(1L, 100L);
        // 模拟过期（直接反射改 expiresAt）
        var f = EditLock.class.getDeclaredField("expiresAt");
        f.setAccessible(true);
        f.set(old, java.time.OffsetDateTime.now().minusMinutes(1));

        EditLock refreshed = EditLock.refresh(old, 200L);
        assertThat(refreshed.getHolderId()).isEqualTo(200L);
        assertThat(refreshed.isExpired()).isFalse();
    }

    @Test
    void isHeldBy_returns_true_for_holder() {
        EditLock lock = EditLock.acquire(1L, 100L);
        assertThat(lock.isHeldBy(100L)).isTrue();
        assertThat(lock.isHeldBy(200L)).isFalse();
        assertThat(lock.isHeldBy(null)).isFalse();
    }
}
