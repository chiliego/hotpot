package org.hotpot.events;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClassPathHandler implements PathWatchEventHandler {
    private Path[] classPaths;
    private List<Consumer<Path>> classpathHandlers;
    private Consumer<PathWatchEventHandler> changeHandler = h -> {};

    public ClassPathHandler() {
        this(new Path[0]);
        this.classpathHandlers = new ArrayList<>();
    }

    public ClassPathHandler(Path[] classPaths) {
        this.classPaths = classPaths;
    }

    @Override
    public Path[] getWatchablePaths() {
        return classPaths;
    }

    @Override
    public void handleModified(Path path) {
        if (isInClassPaths(path)) {
            for (Consumer<Path> classpathHandler : classpathHandlers) {
                classpathHandler.accept(path);
            }
        } else {

        }
    }

    private boolean isInClassPaths(Path path) {
        for (Path classPath : classPaths) {
            if (path.startsWith(classPath)) {
                return true;
            }
        }

        return false;
    }

    public void setClassPaths(Path... classPaths) {
        this.classPaths = classPaths;
        changeHandler.accept(this);
    }

    public void addClassPathHandler(Consumer<Path> classPathHandler) {
        classpathHandlers.add(classPathHandler);
    }

    @Override
    public void onChange(Consumer<PathWatchEventHandler> changeHandler) {
        this.changeHandler = changeHandler;
        
    }
    
}
