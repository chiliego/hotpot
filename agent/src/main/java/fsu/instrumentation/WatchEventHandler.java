package fsu.instrumentation;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class WatchEventHandler {
    private TransformerService ts;

    public WatchEventHandler(TransformerService transformerService) {
        this.ts = transformerService;
    }

    public void handle(Path watchablePath, WatchEvent<?> event) {
        if (ENTRY_MODIFY.equals(event.kind())) {
            Path classFile = (Path) event.context();
            Path classFilePath = watchablePath.resolve(classFile);
            handleModified(classFilePath);
        }
    }

    private void handleModified(Path classFile) {
        ts.swapClassFile(classFile);
    }
}