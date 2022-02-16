package org.hotpot.events;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PathWatchService implements Runnable {
    static Logger LOGGER = LogManager.getLogger(PathWatchService.class);
    private WatchService watchService;
    private Map<PathWatchEventHandler, List<WatchablePath>> handlerPathsMap;

    public PathWatchService() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            LOGGER.error("Coud not register watchservice.", e);
        }
    }

    @Override
    public void run() {
        try {
            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path dir = (Path) key.watchable();
                    Path path = dir.resolve((Path) event.context());
                    
                    if (ENTRY_CREATE.equals(event.kind())) {
                        LOGGER.info("File created [{}].", path);
                        handleCreated(path);
                    } else if (ENTRY_MODIFY.equals(event.kind())) {
                        LOGGER.info("File modified [{}].", path);
                        handleModified(path);
                    }
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Watchservice coud not get key.", e);
        }
    }

    private void handleCreated(Path path) {
        callHandler(handler -> handler.handleCreated(path));
    }

    private void handleModified(Path path) {
        callHandler(handler -> handler.handleModified(path));
    }

    private void handleDeleted(Path path) {
        callHandler(handler -> handler.handleDeleted(path));
    }

    private void callHandler(Consumer<? super PathWatchEventHandler> action) {
        getHandlerPathsMap().keySet()
                .forEach(action);
    }

    public void addHandlers(PathWatchEventHandler... handlers) {
        for (PathWatchEventHandler handler : handlers) {
            addHandler(handler);
            handler.onChange(this::addHandler);
        }
    }

    public Map<PathWatchEventHandler, List<WatchablePath>> getHandlerPathsMap() {
        if (handlerPathsMap == null) {
            handlerPathsMap = new HashMap<>();
        }

        return handlerPathsMap;
    }

    private void addHandler(PathWatchEventHandler handler) {
        List<WatchablePath> watchablePaths = getWatchablePaths(handler);
        List<WatchablePath> previousWatchablePaths = getHandlerPathsMap().put(handler, watchablePaths);

        if (previousWatchablePaths != null) {
            for (WatchablePath previousWatchablePath : previousWatchablePaths) {
                if (!watchablePaths.contains(previousWatchablePath)) {
                    LOGGER.info("Stop watching path [{}].", previousWatchablePath.getPath());
                    previousWatchablePath.stopWatching();
                }
            }
        }
    }

    private List<WatchablePath> getWatchablePaths(PathWatchEventHandler handler) {
        return toWatchablePaths(handler.getWatchablePaths());
    }

    private List<WatchablePath> toWatchablePaths(Path... paths) {
        List<WatchablePath> watchablePathList = new ArrayList<>();
        for (Path path : paths) {
            try {
                Files.walk(path)
                        .filter(Files::isDirectory)
                        .map(this::toWatchable)
                        .filter(w -> w != null)
                        .forEach(watchablePathList::add);
            } catch (IOException e) {
                LOGGER.error("Coud not register watchservice for path [{}].", path);
                e.printStackTrace();
            }
        }

        return watchablePathList;
    }

    private WatchablePath toWatchable(Path path) {
        try {
            WatchKey key = startWatching(path);
            LOGGER.info("Start watching path [{}].", path);
            return new WatchablePath(path, key);
        } catch (IOException e) {
            LOGGER.error("Coud not register watchservice for path [{}].", path);
            e.printStackTrace();
        }

        return null;
    }

    private WatchKey startWatching(Path path) throws IOException {
        return path.toAbsolutePath().register(
                watchService,
                ENTRY_CREATE,
                ENTRY_MODIFY,
                ENTRY_DELETE);
    }

    private class WatchablePath {
        private Path path;
        private WatchKey key;

        public WatchablePath(Path path, WatchKey key) {
            this.path = path;
            this.key = key;
        }

        public void stopWatching() {
            key.cancel();
        }

        public Path getPath() {
            return path;
        }

        public WatchKey getKey() {
            return key;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof WatchablePath) {
                WatchablePath otherWatchable = (WatchablePath) obj;
                String otherAbsPath = otherWatchable.getPath().toAbsolutePath().toString();
                return getPath().toAbsolutePath().toString().equals(otherAbsPath);
            } else {
                return false;
            }
        }
    }
}