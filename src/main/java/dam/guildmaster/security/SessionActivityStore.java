package dam.guildmaster.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding idle window (default 30 min) while JWT absolute exp is max-hours (16h).
 * Same JWT string is reused; validity is enforced here via last access.
 */
@Service
public class SessionActivityStore {

    private final long idleSeconds;
    private final Map<String, Instant> lastAccess = new ConcurrentHashMap<>();

    public SessionActivityStore(@Value("${guildmaster.jwt.idle-minutes:30}") long idleMinutes) {
        this.idleSeconds = idleMinutes * 60;
    }

    public void register(String jti) {
        lastAccess.put(jti, Instant.now());
    }

    /** @return true if session still valid and lastAccess was refreshed */
    public boolean touch(String jti) {
        cleanup();
        Instant last = lastAccess.get(jti);
        if (last == null) {
            return false;
        }
        Instant now = Instant.now();
        if (now.isAfter(last.plusSeconds(idleSeconds))) {
            lastAccess.remove(jti);
            return false;
        }
        lastAccess.put(jti, now);
        return true;
    }

    public void revoke(String jti) {
        lastAccess.remove(jti);
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(idleSeconds);
        Iterator<Map.Entry<String, Instant>> it = lastAccess.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Instant> e = it.next();
            if (e.getValue().isBefore(cutoff)) {
                it.remove();
            }
        }
    }
}
