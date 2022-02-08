package org.hotpot.agent.events;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PathWatchService implements Runnable {
    static Logger LOGGER = LogManager.getLogger();
    private PathWatchEventHandler handler;
    private WatchService watchService;
    private Map<Path, WatchKey> watchablePathMap;

    public PathWatchService(PathWatchEventHandler handler) {
        this.handler = handler;
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            LOGGER.error("Coud not register watchservice.", e);
        }
    }

    public PathWatchService(PathWatchEventHandler handler, List<Path> watchableDirs) {
        this(handler);
        setWatchablePaths(watchableDirs);
    }

    public PathWatchService(PathWatchEventHandler handler, Path... watchablePaths) {
        this(handler);
        setWatchablePaths(watchablePaths);

    }

    @Override
    public void run() {
        try {
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path dir = (Path) key.watchable();
                    if (ENTRY_MODIFY.equals(event.kind())) {
                        Path path = dir.resolve((Path) event.context());
                        handler.handleModified(path);
                    }
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Watchservice coud not get key.", e);
        }
    }

    private Map<Path, WatchKey> getWatchablePathMap() {
        if (watchablePathMap == null) {
            watchablePathMap = new HashMap<>();
        }

        return watchablePathMap;
    }

    public void setWatchablePaths(Path... paths) {
        List<Path> pathList = new ArrayList<>();
        Collections.addAll(pathList, paths);
        setWatchablePaths(pathList);
    }

    public void setWatchablePaths(List<Path> paths) {
        unregisterAllWatchable();
        addWatchablePaths(paths);
    }

    private void unregisterAllWatchable() {
        getWatchablePathMap().keySet()
            .forEach(this::unregisterWatchable);
    }

    private void unregisterWatchable(Path path) {
        WatchKey key = getWatchablePathMap().remove(path);
        if (key != null) {
            key.cancel();
            LOGGER.info("Unregistered watchservice for [{}]", path);
        }
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