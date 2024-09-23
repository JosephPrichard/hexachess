package services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.jooby.WebSocket;
import lombok.AllArgsConstructor;
import lombok.Data;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import static utils.Log.LOGGER;

public class BroadcastService {

    private static final String CHANNEL_NAME = "global-ws-broadcast";
    private static final char FIELD_SPLIT = 0x1e;

    private final JedisPooled jedis;
    private final LoadingCache<String, List<WebSocket>> socketsMap = Caffeine.newBuilder()
        .scheduler(Scheduler.systemScheduler())
        .build(key -> new CopyOnWriteArrayList<>());

    public BroadcastService(JedisPooled jedis) {
        this.jedis = jedis;
    }

    public void subscribeLocal(String id, WebSocket ws) {
        var socketList = socketsMap.get(id);
        socketList.add(ws);
        socketsMap.put(id, socketList);
        LOGGER.info(String.format("Ws %s subscribed to id: %s, ref: %s", ws.toString(), id, this));
    }

    public void unsubscribeLocal(String id, WebSocket ws) {
        var socketList = socketsMap.get(id);
        socketList.remove(ws);
        socketsMap.put(id, socketList);
        LOGGER.info(String.format("Ws %s unsubscribed from id: %s, ref: %s", ws.toString(), id, this));
    }

    public void broadcastLocal(String id, String content) {
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

    @Data
    @AllArgsConstructor
    static class Message {
        private final String id;
        private final String content;
    }

    public void broadcastGlobal(String id, String content) {
        var message = id + FIELD_SPLIT + content;
        jedis.publish(CHANNEL_NAME, message);
        LOGGER.info(String.format("Broadcast global to id: %s, message: %s", id, message));
    }

    public JedisPubSub startListenSubscribe() throws ExecutionException, InterruptedException {
        CompletableFuture<JedisPubSub> futureSubscriber = new CompletableFuture<>();
        var thread = new Thread(() -> {
            var subscriber = new JedisPubSub() {
                @Override
                public void onSubscribe(String channel, int subscribedChannels) {
                    super.onSubscribe(channel, subscribedChannels);
                    LOGGER.info(String.format("Started the subscriber listener for broadcast instance: %s", this));
                    futureSubscriber.complete(this);
                }

                @Override
                public void onMessage(String channel, String message) {
                    try {
                        super.onMessage(channel, message);
                        var index = message.indexOf(FIELD_SPLIT);
                        if (index == -1) {
                            LOGGER.error(String.format("Invalid message format: %s", message));
                            return;
                        }
                        broadcastLocal(message.substring(0, index), message.substring(index + 1));
                    } catch (Exception ex) {
                        LOGGER.error("Error occurred in subscriber thread " + ex);
                    }
                }
            };
            jedis.subscribe(subscriber, CHANNEL_NAME); // start the subscriber, blocking the current thread until subscriber is stopped
        });
        thread.start();

        // don't actually return the jedis subscriber until the thread notifies us that we've created it
        return futureSubscriber.get();
    }
}
