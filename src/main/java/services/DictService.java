package services;

import lombok.AllArgsConstructor;
import lombok.Data;
import models.Duel;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.Tuple;
import utils.Serializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public interface DictService {

    @Data
    @AllArgsConstructor
    class GetDuelKeysResult {
        private final Double nextCursor;
        private final List<String> duelKeys;
    }

    Duel getDuel(String id);

    Duel setDuel(String id, Duel duel);

    GetDuelKeysResult getDuelKeys(Double cursor, int count);

    Duel.Player getSession(String sessionId);

    void setSession(String sessionId, Duel.Player player, long expirySeconds);

    Duel.Player getSessionOrDefault(String sessionId);
}