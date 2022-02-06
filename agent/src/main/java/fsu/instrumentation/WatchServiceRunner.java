package fsu.instrumentation;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class WatchServiceRunner implements Runnable {
    private WatchEventHandler handler;

    public WatchServiceRunner(WatchEventHandler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            Path path = Paths.get("/workspaces/myApp/app/bin/main/org/chiliego");
            path.register(
                    watchService,
                    ENTRY_MODIFY);
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    handler.handle(path, event);
                }
                key.reset();
            }
        } catch (IOException e) {
            Agent.LOGGER.error("Coud not register watchservice.", e);
        } catch (InterruptedException e) {
            Agent.LOGGER.error("Watchservice coud not get key.", e);
        }
    }
}