package utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Threading {
    public static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
}
