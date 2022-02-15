package org.hotpot.conf;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class Configs {
    private static Configs instance;

    private List<Path> watchablePathList;
    private List<Consumer<List<Path>>> consumerList;

    private Configs() {
    }

    public static Configs instance() {
        if (instance == null) {
            instance = new Configs();
        }

        return instance;
    }

    private List<Consumer<List<Path>>> getConsumerList() {
        if(consumerList == null) {
            return new ArrayList<>();
        }

        return consumerList;
    }

    public List<Path> getWatchablePathList() {
        if(watchablePathList == null) {
            return Collections.emptyList();
        }

        return watchablePathList;
    }

    public void setWatchablePathList(List<Path> pathList) {
        watchablePathList = pathList;

        for (Consumer<List<Path>> consumer : getConsumerList()) {
            consumer.accept(pathList);
        }
    }

    public void addWatchablePathComsumer(Consumer<List<Path>> consumer) {
        getConsumerList().add(consumer);
    }
}
