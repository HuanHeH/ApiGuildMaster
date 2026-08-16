package dam.guildmaster.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRateLimiterTest {

    @Test
    void blocksAfterConfiguredFailuresAndClearsOnSuccess() {
        LoginRateLimiter limiter = new LoginRateLimiter(2, 60, 300);

        assertFalse(limiter.isBlocked("127.0.0.1", "user@example.test"));
        limiter.recordFailure("127.0.0.1", "user@example.test");
        assertFalse(limiter.isBlocked("127.0.0.1", "user@example.test"));
        limiter.recordFailure("127.0.0.1", "user@example.test");
        assertTrue(limiter.isBlocked("127.0.0.1", "USER@example.test"));

        limiter.clear("127.0.0.1", "user@example.test");

        assertFalse(limiter.isBlocked("127.0.0.1", "user@example.test"));
    }
}
