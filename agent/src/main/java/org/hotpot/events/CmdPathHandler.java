package org.hotpot.events;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CmdPathHandler implements PathWatchEventHandler {
    private static Logger LOGGER = LogManager.getLogger(CmdPathHandler.class);
    private Path cmdPath;
    private Map<String, Consumer<Path>> cmdsMap;
    private boolean isWatching = false;

    public CmdPathHandler(Path cmdPath) {
        this.cmdPath = cmdPath;
        this.cmdsMap = new HashMap<>();
    }

    @Override
    public List<Path> getWatchablePaths() {
        ArrayList<Path> watchablePaths = new ArrayList<>();

        if(!isWatching) {
            watchablePaths.add(cmdPath);
            isWatching = true;
        }

        return watchablePaths;
    }

    @Override
    public void handleCreated(Path path) {
        handleModified(path);
    }

    @Override
    public void handleModified(Path path) {
        if (path.startsWith(cmdPath)) {
            String cmd = path.getFileName().toString();
            Consumer<Path> cmdFunc = cmdsMap.get(cmd);
            if(cmdFunc == null) {
                LOGGER.info("Not supported command [{}].", cmd);
                return;
            }
    
            cmdFunc.accept(path);
        }
    }

    @Override
    public void handleDeleted(Path path) {
        // TODO Auto-generated method stub
        
    }

    public void addCmd(String cmd, Consumer<Path> cmdFunc) {
        cmdsMap.put(cmd, cmdFunc);
    }
}
