package utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Globals {
    public static final String GREEN_COLOR = "green-color";
    public static final String RED_COLOR = "red-color";
    public static final String YELLOW_COLOR = "yellow-color";
    public static final List<Object> EMPTY_lIST = List.of();
    public static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper()
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
}
