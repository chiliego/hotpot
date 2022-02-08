package org.hotpot.events;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigFileWatchEventHandler implements PathWatchEventHandler {
    private Logger LOGGER = LogManager.getLogger();
    private PathWatchService pathWatchService;
    private String CONF_FILE = "watchlist.conf";

    public ConfigFileWatchEventHandler(PathWatchService pathWatchService) {
        this.pathWatchService = pathWatchService;
    }

    @Override
    public void handleModified(Path path) {
        LOGGER.info("Try reading config file [{}].", path);
        if(path.getFileName().toString().equals(CONF_FILE)) {
            try {
                try(BufferedReader br = Files.newBufferedReader(path)) {
                    List<Path> watchablePathList = new ArrayList<>();
                    String watchablePathStr = br.readLine();
                    
                    while (watchablePathStr != null) {
                        Path watchablePath = Paths.get(watchablePathStr);
                        watchablePathList.add(watchablePath);
                        LOGGER.info("Add path [{}] to watch list.", path);
                        watchablePathStr = br.readLine();
                    }
    
                    pathWatchService.addWatchablePaths(watchablePathList);
                } 
            } catch (IOException e) {
                LOGGER.error("No config file [{}] found.", path);
            }
        }
    }

    public void checkAndReadConfFile(Path confDir) {
       handleModified(confDir.resolve(CONF_FILE));
    }
}
