package com.campusconnect.common.ratelimit;

import com.campusconnect.common.exception.RateLimitedException;
import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class FixedWindowLimiterTest {

    @Test
    void allowsUpToLimit_thenThrows() {
        var l = new FixedWindowLimiter(3, Duration.ofMinutes(1), 100);
        for (int i = 0; i < 3; i++) l.acquireOrThrow("ip1");
        assertThatThrownBy(() -> l.acquireOrThrow("ip1")).isInstanceOf(RateLimitedException.class);
    }

    @Test
    void keysAreIndependent() {
        var l = new FixedWindowLimiter(1, Duration.ofMinutes(1), 100);
        l.acquireOrThrow("a");
        assertThatCode(() -> l.acquireOrThrow("b")).doesNotThrowAnyException();
    }

    @Test
    void failureCounting_blocksOnlyAfterLimitReached() {
        var l = new FixedWindowLimiter(2, Duration.ofMinutes(1), 100);
        l.recordFailure("k");
        assertThatCode(() -> l.failIfExceeded("k")).doesNotThrowAnyException();
        l.recordFailure("k");
        assertThatThrownBy(() -> l.failIfExceeded("k")).isInstanceOf(RateLimitedException.class);
    }
}