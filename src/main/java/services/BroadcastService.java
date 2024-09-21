package services;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.jooby.WebSocket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;
import utils.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static utils.Log.LOGGER;

public class BroadcastService {

    private final JedisPooled jedis;
    private final LoadingCache<String, Queue<WebSocket>> socketsMap = Caffeine.newBuilder()
        .initialCapacity(1000)
        .scheduler(Scheduler.systemScheduler())
        .build(key -> new ConcurrentLinkedQueue<>());

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

    public void broadcastGlobal(String id, String content) {
        var msg = id + ":" + content;
        jedis.publish("global", msg);
        LOGGER.info(String.format("Broadcast global to id: %s, message: %s", id, msg));
    }

    public JedisPubSub startListenSubscribe() throws ExecutionException, InterruptedException {
        LOGGER.info(String.format("Started the subscriber listener for broadcast instance: %s", this));

        CompletableFuture<JedisPubSub> futureSubscriber = new CompletableFuture<>();
        var thread = new Thread(() -> {
            var subscriber = new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    super.onMessage(channel, message);

                    var index = message.indexOf(':');
                    if (index == -1) {
                        LOGGER.error(String.format("Invalid message format: %s", message));
                        return;
                    }

                    broadcastLocal(message.substring(0, index), message.substring(index + 1));
                }
            };
            futureSubscriber.complete(subscriber);
            jedis.subscribe(subscriber, "global"); // start the subscriber, blocking the current thread until subscriber is stopped
        });
        thread.start();

        // don't actually return the jedis subscriber until the thread notifies us that we've created it
        return futureSubscriber.get();
    }
}
