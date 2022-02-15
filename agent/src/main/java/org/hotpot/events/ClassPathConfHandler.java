package org.hotpot.events;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClassPathConfHandler implements PathWatchEventHandler{
    private static Logger LOGGER = LogManager.getLogger(ClassPathConfHandler.class);
    private List<Consumer<Path[]>> classPathConsumers;
    private Path confDir;

    public ClassPathConfHandler(Path confDir) {
        this.confDir = confDir;
        this.classPathConsumers = new ArrayList<>();
    }

    private boolean isSupportedFile(Path path) {
        return getConfFilePath().equals(path);
    }

    public Path getConfFilePath() {
        return confDir.resolve("classPath.conf");
    }

    @Override
    public void handleModified(Path classPathConf) {
        if (isSupportedFile(classPathConf)) {
            if (!Files.exists(classPathConf)) {
                try {
                    Files.createFile(classPathConf);
                } catch (IOException e) {
                    LOGGER.error("Could not create config file [{}].", classPathConf);
                }
            }

            try {
                try (BufferedReader br = Files.newBufferedReader(classPathConf)) {
                    List<Path> classPathList = new ArrayList<>();
                    String classPathStr = br.readLine();
                    
                    while (classPathStr != null) {
                        Path classPath = Paths.get(classPathStr);
                        if(Files.exists(classPath)){
                            classPathList.add(classPath);
                            LOGGER.info("Add path [{}] to watch list.", classPath);
                        } else {
                            LOGGER.error("Class path [{}] does not exist.", classPath.toAbsolutePath());
                        }
                        classPathStr = br.readLine();
                    }
                    
                    for (Consumer<Path[]> consumer : classPathConsumers) {
                        consumer.accept(classPathList.toArray(new Path[classPathList.size()]));
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Could not read config file [{}].", classPathConf);
            }
        }
    }

    public void addClassPathConsumer(Consumer<Path[]> consumer) {
        classPathConsumers.add(consumer);
    }

    @Override
    public Path[] getWatchablePaths() {
        return toArray(confDir);
    }

    private Path[] toArray(Path... confDirs) {
        return confDirs;
    }

    @Override
    public void onChange(Consumer<PathWatchEventHandler> changeHandler) {
        // do nothing
    }

    public void init() {
        handleModified(getConfFilePath());
    }
}
