package dam.guildmaster.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding idle window (default 30 min) while JWT absolute exp is max-hours (16h).
 * Same JWT string is reused; validity is enforced here via last access.
 */
@Service
public class SessionActivityStore {

    private final long idleSeconds;
    private final Map<String, Instant> lastAccess = new ConcurrentHashMap<>();
    private final Set<String> revokedJtis = ConcurrentHashMap.newKeySet();

    public SessionActivityStore(@Value("${guildmaster.jwt.idle-minutes:30}") long idleMinutes) {
        this.idleSeconds = idleMinutes * 60;
    }

    public void register(String jti) {
        if (jti == null) return;
        revokedJtis.remove(jti);
        lastAccess.put(jti, Instant.now());
    }

    /** @return true if session still valid and lastAccess was refreshed */
    public boolean touch(String jti) {
        if (jti == null) return false;
        if (revokedJtis.contains(jti)) return false;
        cleanup();
        Instant now = Instant.now();
        Instant last = lastAccess.get(jti);
        if (last == null) {
            // Re-admit a still-valid JWT after API restart (in-memory store was cleared).
            lastAccess.put(jti, now);
            return true;
        }
        if (now.isAfter(last.plusSeconds(idleSeconds))) {
            lastAccess.remove(jti);
            return false;
        }
        lastAccess.put(jti, now);
        return true;
    }

    public void revoke(String jti) {
        if (jti == null) return;
        lastAccess.remove(jti);
        revokedJtis.add(jti);
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
