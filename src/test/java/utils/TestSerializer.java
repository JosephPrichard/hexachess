package utils;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import services.Duel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class TestSerializer {

    @Test
    public void testRoundTrip() {
        // given
        var match = Duel.start();

        // when
        var rawBytesOut = new ByteArrayOutputStream();
        try (var output = new Output(rawBytesOut)) {
            var kryo = Serializer.get();
            kryo.writeObject(output, match);
        }

        Duel afterDuel;
        var rawBytesIn = new ByteArrayInputStream(rawBytesOut.toByteArray());
        try (var input = new Input(rawBytesIn)) {
            var kryo = Serializer.get();
            afterDuel = kryo.readObject(input, Duel.class);
        }

        // then
        Assertions.assertEquals(match, afterDuel);
    }
}
