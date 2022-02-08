package org.hotpot.events;

import java.nio.file.Path;

import org.hotpot.instrumentation.TransformerService;

public class TransformerWatchEventHandler implements PathWatchEventHandler {
    private TransformerService ts;

    public TransformerWatchEventHandler(TransformerService transformerService) {
        this.ts = transformerService;
    }

    @Override
    public void handleModified(Path classFile) {
        ts.swapClassFile(classFile);
    }
}