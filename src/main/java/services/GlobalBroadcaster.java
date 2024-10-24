package services;

import io.jooby.WebSocket;
import lombok.AllArgsConstructor;
import lombok.Data;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static utils.Log.LOGGER;

public class GlobalBroadcaster implements Broadcaster {

    private static final String CHANNEL_NAME = "global-ws-broadcast";
    private static final char FIELD_SPLIT = 0x1e;

    private final JedisPooled jedis;
    private final LocalBroadcaster localBroadcaster = new LocalBroadcaster();

    public GlobalBroadcaster(JedisPooled jedis) {
        this.jedis = jedis;
    }

    @Data
    @AllArgsConstructor
    static class Message {
        private final String id;
        private final String content;
    }

    @Override
    public void subscribe(String id, WebSocket ws) {
        localBroadcaster.subscribe(id, ws);
    }

    @Override
    public void unsubscribe(String id, WebSocket ws) {
        localBroadcaster.unsubscribe(id, ws);
    }

    @Override
    public void broadcast(String id, String content) {
        var message = id + FIELD_SPLIT + content;
        jedis.publish(CHANNEL_NAME, message);
        LOGGER.info(String.format("Broadcast global to id: %s, message: %s", id, message));
    }

    public JedisPubSub startListenSubscribe() throws ExecutionException, InterruptedException {
        CompletableFuture<JedisPubSub> futureSubscriber = new CompletableFuture<>();
        var thread = Thread.ofVirtual().start(() -> {
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
                        localBroadcaster.broadcast(message.substring(0, index), message.substring(index + 1));
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
