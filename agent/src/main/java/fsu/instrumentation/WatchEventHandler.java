package fsu.instrumentation;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class WatchEventHandler {
    private TransformerService ts;

    public WatchEventHandler(TransformerService transformerService) {
        this.ts = transformerService;
    }

    public void handle(Path watchedPath, WatchEvent<?> event) {
        if (ENTRY_MODIFY.equals(event.kind())) {
            Path changed = (Path) event.context();
            Path pathChanged = watchedPath.resolve(changed);
            handlePath(pathChanged);
        }
    }

    private void handlePath(Path changedClassFile) {
        try {
            byte[] byteCode = Files.readAllBytes(changedClassFile);
            ts.swapClassFile(byteCode);
        } catch (IOException e) {
            Agent.LOGGER.error("Could not read class file " + changedClassFile + ".", e);
        }
    }
}