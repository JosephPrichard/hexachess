package services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.jooby.WebSocket;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static utils.Log.LOGGER;

public class LocalBroadcaster implements Broadcaster {

    private final LoadingCache<String, List<WebSocket>> socketsMap = Caffeine.newBuilder()
        .scheduler(Scheduler.systemScheduler())
        .build(key -> new CopyOnWriteArrayList<>());

    @Override
    public void subscribe(String id, WebSocket ws) {
        var socketList = socketsMap.get(id);
        socketList.add(ws);
        socketsMap.put(id, socketList);
        LOGGER.info(String.format("Ws %s subscribed to id: %s, ref: %s", ws.toString(), id, this));
    }

    @Override
    public void unsubscribe(String id, WebSocket ws) {
        var socketList = socketsMap.get(id);
        socketList.remove(ws);
        socketsMap.put(id, socketList);
        LOGGER.info(String.format("Ws %s unsubscribed from id: %s, ref: %s", ws.toString(), id, this));
    }

    @Override
    public void broadcast(String id, String content) {
        var socketList = socketsMap.get(id);
        if (socketList == null) {
            LOGGER.info(String.format("Broadcast local to id: %s, but there where no subscribers", id));
            return;
        }
        for (var socket : socketList) {
            socket.send(content);
        }
        LOGGER.info(String.format("Broadcast local to id: %s, content: %s, ref: %s", id, content, this));
    }
}
