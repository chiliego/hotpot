package org.hotpot.events;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface PathWatchEventHandler {
    public Path[] getWatchablePaths();
    public void handleModified(Path path);
    public void onChange(Consumer<PathWatchEventHandler> changeHandler);
}
