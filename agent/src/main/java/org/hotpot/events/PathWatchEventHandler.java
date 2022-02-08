package org.hotpot.events;

import java.nio.file.Path;

public interface PathWatchEventHandler {
    public void handleModified(Path path);
}
