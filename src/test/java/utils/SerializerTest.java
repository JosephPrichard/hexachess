package utils;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import models.GameState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class SerializerTest {

    @Test
    public void testRoundTrip() {
        // given
        var match = GameState.startWithGame("1");

        // when
        var rawBytesOut = new ByteArrayOutputStream();
        try (var output = new Output(rawBytesOut)) {
            var kryo = Serializer.get();
            kryo.writeObject(output, match);
        }

        GameState afterGameState;
        var rawBytesIn = new ByteArrayInputStream(rawBytesOut.toByteArray());
        try (var input = new Input(rawBytesIn)) {
            var kryo = Serializer.get();
            afterGameState = kryo.readObject(input, GameState.class);
        }

        // then
        Assertions.assertEquals(match, afterGameState);
    }
}
