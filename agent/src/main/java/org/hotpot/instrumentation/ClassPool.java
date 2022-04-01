package org.hotpot.instrumentation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClassPool {
    private static Logger LOGGER = LogManager.getLogger(ClassPool.class);
    private Map<String, Path> classPathMap;
    private List<ClassLoader> classloaders;
    
    public ClassPool() {
        this.classPathMap = new HashMap<>();
        this.classloaders = new ArrayList<>();
    }

    public void addClassPath(String className, Path classPath) {
        if (!classPathMap.containsKey(className)) {
            classPathMap.put(className, classPath);
        }
    }

    public Class<?> loadClass(String className) {
        className = className.replace("/", ".");
        for (ClassLoader loader : classloaders) {
            try {
				return loader.loadClass(className);
			} catch (ClassNotFoundException e) {
				// do nothing when class not found
			}
        }

        return null;
    }
}
