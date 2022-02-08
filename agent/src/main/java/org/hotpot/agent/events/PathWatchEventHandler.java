package org.hotpot.agent.events;

import java.nio.file.Path;

public interface PathWatchEventHandler {
    public void handleModified(Path path);
}
