package org.hotpot.events;

import java.nio.file.Path;
import java.util.List;

public interface PathWatchEventHandler {
    public List<Path> getWatchablePaths();
    public void handleCreated(Path path);
    public void handleModified(Path path);
    public void handleDeleted(Path path);
}
