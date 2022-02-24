package org.hotpot.events;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClassPathHandler implements PathWatchEventHandler{
    private static Logger LOGGER = LogManager.getLogger(ClassPathHandler.class);
    private Path confDir;
    private List<Path> classPaths;
    private List<Path> watchAble;
    private BiConsumer<String, Path> modifyObserver;
    private BiConsumer<String, Path> createObserver;

    public ClassPathHandler(Path confDir) {
        this.confDir = confDir;
        init();
    }

    private boolean isConfigFile(Path path) {
        return getConfFilePath().equals(path);
    }

    public Path getConfFilePath() {
        return confDir.resolve("classPath.conf");
    }

    @Override
    public void handleCreated(Path path) {
        if (isConfigFile(path)) {
            LOGGER.info("Config file [{}] created.", path);
            readConfigFile(path);
        } else if (isInClassPaths(path)) {
            LOGGER.info("Class [{}] created.", path);
            if (Files.exists(path) && path.getFileName().toString().endsWith(".class")) {
                handleClassCreated(path);
            }
        }
    }
    
    @Override
    public void handleModified(Path path) {
        if (isConfigFile(path)) {
            LOGGER.info("Config file [{}] modified.", path);
            readConfigFile(path);
        } else {
            if (Files.exists(path) && path.getFileName().toString().endsWith(".class")) {
                LOGGER.info("Class file [{}] modified.", path);
                handleClassModified(path);
            }
        }
    }

    private void handleClassCreated(Path path) {
        String className = internalClassNameFromPath(path);
        if(className != null) {
            if(createObserver != null) {
                LOGGER.info("Notify observer for created class [{}] with file [{}].", className, path);
                createObserver.accept(className, path);
            }
        } else {
            LOGGER.error("Get class name from path [{}] failed.", path);
        }
    }

    private void handleClassModified(Path path) {
        String className = internalClassNameFromPath(path);
        if(className != null) {
            if(modifyObserver != null) {
                LOGGER.info("Notify observer for modified class [{}] with file [{}].", className, path);
                modifyObserver.accept(className, path);
            }
        } else {
            LOGGER.error("Get class name from path [{}] failed.", path);
        }
    }

    private String internalClassNameFromPath(Path path) {
        for (Path classPath : classPaths) {
            if (path.startsWith(classPath)) {
                return classPath.relativize(path).toString().replace(".class", "");
            }
        }

        return null;
    }

    @Override
    public void handleDeleted(Path path) {
        LOGGER.info("File [{}] deleted.", path);
    }

    private boolean isInClassPaths(Path path) {
        for (Path classPath : classPaths) {
            if (path.startsWith(classPath)) {
                return true;
            }
        }

        return false;
    }

    private void readConfigFile(Path classPathConf) {
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
                
                setClassPaths(classPathList);
            }
        } catch (IOException e) {
            LOGGER.error("Could not read config file [{}].", classPathConf);
        }
    }

    private void setClassPaths(List<Path> classPathList) {
        this.classPaths = classPathList;
        addWatchable(classPathList);
    }

    private void addWatchable(List<Path> paths) {
        getWatchable().addAll(paths);
    }

    private void addWatchable(Path path) {
        getWatchable().add(path);
    }

    private List<Path> getWatchable() {
        if (watchAble == null) {
            watchAble = new ArrayList<>();
        }

        return watchAble;
    }

    @Override
    public List<Path> getWatchablePaths() {
        ArrayList<Path> watchAble = new ArrayList<>();
        if (this.watchAble != null) {
            watchAble.addAll(this.watchAble);
            this.watchAble = null;    
        } 

        return watchAble;
    }

    private void init() {
        readConfigFile(getConfFilePath());
        addWatchable(confDir);
    }

    public void onCreate(BiConsumer<String, Path> createObserver) {
        this.createObserver = createObserver;
    }

    public void onModify(BiConsumer<String, Path> modifyObserver) {
        this.modifyObserver = modifyObserver;
    }
}
