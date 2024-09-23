package services;

import models.Duel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// a local dictionary storage for mocking, testing, and debugging
public class LocalDictService implements DictService {

    private final Map<String, Object> map = new ConcurrentHashMap<>();
    private final List<String> duelKeys = new CopyOnWriteArrayList<>();

    public Duel getDuel(String id) {
        return (Duel) map.get(id);
    }

    public Duel setDuel(String id, Duel duel) {
        var exists = getDuel(id) != null;
        if (!exists) {
            map.put(id, duel);
            duelKeys.add(id);
        }
        return duel;
    }

    public GetDuelKeysResult getDuelKeys(Double cursor, int count) {
        return new GetDuelKeysResult(null, duelKeys);
    }

    public Duel.Player getSession(String sessionId) {
        return (Duel.Player) map.get(sessionId);
    }

    public void setSession(String sessionId, Duel.Player player, long expirySeconds) {
        map.put(sessionId, player);
    }

    public Duel.Player getSessionOrDefault(String sessionId) {
        if (sessionId == null) {
            return new Duel.Player(UUID.randomUUID().toString(), "default-name");
        }
        var player = getSession(sessionId);
        if (player == null) {
            return new Duel.Player(UUID.randomUUID().toString(), "default-name");
        }
        return player;
    }
}
