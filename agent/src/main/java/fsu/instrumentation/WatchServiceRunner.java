package fsu.instrumentation;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WatchServiceRunner implements Runnable {
    static Logger LOGGER = LogManager.getLogger();
    private WatchEventHandler handler;
    private WatchService watchService;
    private Map<Path,WatchKey> watchablePathMap;

    public WatchServiceRunner(WatchEventHandler handler) {
        this.handler = handler;
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            LOGGER.error("Coud not register watchservice.", e);
        }
    }

    public WatchServiceRunner(WatchEventHandler handler, List<Path> watchableDirs) {
        this(handler);
        setWatchablePaths(watchableDirs);
    }
    public WatchServiceRunner(WatchEventHandler handler, Path... watchablePath) {
    }

    @Override
    public void run() {
        try {
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path dir = (Path)key.watchable();
                    handler.handle(dir, event);
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Watchservice coud not get key.", e);
        }
    }

    private Map<Path,WatchKey> getWatchablePathMap() {
        if(watchablePathMap == null) {
            watchablePathMap = new HashMap<>();
        }

        return watchablePathMap;
    }

    public void setWatchablePaths(List<Path> paths) {
        unregisterWatchable();
        addWatchablePaths(paths);
    }

    private void unregisterWatchable() {
        getWatchablePathMap().keySet().forEach(path -> {
            WatchKey key = getWatchablePathMap().remove(path);
            if(key != null) {
                key.cancel();
                LOGGER.info("Unregistered watchservice for [{}]", path);
            }
        });
    }

    public void addWatchablePaths(List<Path> paths) {
        try {
            for (Path path : paths) {
                if (!getWatchablePathMap().containsKey(path)) {
                    WatchKey key = path.toAbsolutePath().register(
                        watchService,
                        ENTRY_MODIFY);
                    getWatchablePathMap().put(path, key);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Coud not register watchservice.", e);
        }
    }
}