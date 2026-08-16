package dam.guildmaster.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-process login limiter for a single API instance.
 * Production deployments with multiple instances must move this state to Redis.
 */
@Service
public class LoginRateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Duration blockDuration;
    private final Map<String, AttemptState> failures = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${guildmaster.auth.login.max-attempts:5}") int maxAttempts,
            @Value("${guildmaster.auth.login.window-seconds:60}") long windowSeconds,
            @Value("${guildmaster.auth.login.block-seconds:300}") long blockSeconds) {
        if (maxAttempts < 1 || windowSeconds < 1 || blockSeconds < 1) {
            throw new IllegalArgumentException("Login rate limiter values must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
        this.blockDuration = Duration.ofSeconds(blockSeconds);
    }

    public boolean isBlocked(String ipAddress, String mail) {
        Instant now = Instant.now();
        return isBlocked(key("ip", ipAddress), now) || isBlocked(key("mail", normalizeMail(mail)), now);
    }

    public void recordFailure(String ipAddress, String mail) {
        recordFailure(key("ip", ipAddress));
        recordFailure(key("mail", normalizeMail(mail)));
    }

    public void clear(String ipAddress, String mail) {
        failures.remove(key("ip", ipAddress));
        failures.remove(key("mail", normalizeMail(mail)));
    }

    private boolean isBlocked(String key, Instant now) {
        AttemptState state = failures.get(key);
        if (state == null) return false;
        synchronized (state) {
            if (state.blockedUntil != null && now.isBefore(state.blockedUntil)) return true;
            if (state.blockedUntil != null && !now.isBefore(state.blockedUntil)) {
                failures.remove(key, state);
                return false;
            }
            discardExpired(state, now);
            if (state.failures.isEmpty()) failures.remove(key, state);
            return false;
        }
    }

    private void recordFailure(String key) {
        Instant now = Instant.now();
        AttemptState state = failures.computeIfAbsent(key, ignored -> new AttemptState());
        synchronized (state) {
            discardExpired(state, now);
            state.failures.addLast(now);
            if (state.failures.size() >= maxAttempts) {
                state.blockedUntil = now.plus(blockDuration);
            }
        }
    }

    private void discardExpired(AttemptState state, Instant now) {
        Instant cutoff = now.minus(window);
        while (!state.failures.isEmpty() && state.failures.peekFirst().isBefore(cutoff)) {
            state.failures.removeFirst();
        }
    }

    private static String key(String type, String value) {
        return type + ':' + (value == null || value.isBlank() ? "unknown" : value.trim());
    }

    private static String normalizeMail(String mail) {
        return mail == null ? "unknown" : mail.trim().toLowerCase(Locale.ROOT);
    }

    private static final class AttemptState {
        private final Deque<Instant> failures = new ArrayDeque<>();
        private Instant blockedUntil;
    }
}
