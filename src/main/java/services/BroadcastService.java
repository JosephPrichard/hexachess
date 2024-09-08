package services;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.jooby.WebSocket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;
import utils.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
        LOGGER.info(String.format("Ws %s subscribed to id %s", ws.toString(), id));
    }

    public void unsubscribeLocal(String id, WebSocket ws) {
        var socketList = socketsMap.get(id);
        socketList.remove(ws);
        socketsMap.put(id, socketList);
        LOGGER.info(String.format("Ws %s unsubscribed from id %s", ws.toString(), id));
    }

    public void broadcastLocal(String id, String content) {
        var socketList = socketsMap.get(id);
        if (socketList == null) {
            return;
        }
        for (var socket : socketList) {
            socket.send(content);
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String id;
        private String content;
    }

    public void broadcastGlobal(String id, String content) {
        var rawBytes = new ByteArrayOutputStream();
        try (var output = new Output(rawBytes)) {
            var kryo = Serializer.get();
            kryo.writeObject(output, new Message(id, content));

            jedis.publish("global", rawBytes.toString());
        }
    }

    public void startListenSubscribe() {
        jedis.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                super.onMessage(channel, message);

                var rawBytes = new ByteArrayInputStream(message.getBytes());
                try (var input = new Input(rawBytes)) {
                    var kryo = Serializer.get();
                    var msg = kryo.readObject(input, Message.class);

                    broadcastLocal(msg.getId(), msg.getContent());
                }
            }
        }, "global");
    }
}
