package org.hotpot.instrumentation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModifiedClasses {
    private static Logger LOGGER = LogManager.getLogger(ModifiedClasses.class);
    private static ModifiedClasses instance;
    private Map<String, Path> modifiedClasses;
    private Map<String, byte[]> preModifiedClasses;
    private Map<String, String> subClassRef;
    private List<String> redefinedClassloaders;

    private ModifiedClasses() {
        this.modifiedClasses = new HashMap<>();
        this.preModifiedClasses = new HashMap<>();
        this.subClassRef = new HashMap<>();
        this.redefinedClassloaders = new ArrayList<>();
    }

    public static ModifiedClasses getInstance() {
        if (instance == null) {
            instance = new ModifiedClasses();
        }

        return instance;
    }

    public void add(String className, Path classFilePath) {
        if (!modifiedClasses.containsKey(className)) {
            LOGGER.info("Add file [{}] to modified class List.", classFilePath);
            preModifiedClasses.put(internalName(className), getByteCode(classFilePath));
        }

        modifiedClasses.put(internalName(className), classFilePath);
    }

    /**
     * @param className will be converted to internal form automatically
     */
    public Path getClassFilePath(String className) {
        return modifiedClasses.get(internalName(className));
    }

    /**
     * 
     * @param className
     * @return the bytecode, null if not exists
     */
    public byte[] getByteCode(String className) {
        Path classFilePath = getClassFilePath(className);
        return getByteCode(classFilePath);
    }
    
    private byte[] getByteCode(Path classFilePath) {
        if (classFilePath == null) {
            return null;
        }
        
        try {
            return Files.readAllBytes(classFilePath);
        } catch (IOException e) {
            LOGGER.error("Get class bytecode from file [{}] failed.", classFilePath);
            e.printStackTrace();
        }
        
        return null;
    }

    public byte[] getPreModByteCode(String className) {
        return preModifiedClasses.get(internalName(className));
    }
    
    public Path remove(String className) {
        return modifiedClasses.remove(internalName(className));
    }

    private String internalName(String className) {
        return className.replace(".", "/");
    }

    public boolean has(String className) {
        return modifiedClasses.containsKey(internalName(className));
    }

    public boolean isRedefined(ClassLoader loader) {
        String name = internalName(loader.getClass().getName());

        boolean contains = redefinedClassloaders.contains(name);
        return contains;
    }

    public void setAsRedefined(List<ClassLoader> classloaders) {
        for (ClassLoader classLoader : classloaders) {
            setAsRedefined(classLoader);
        }
    }

    public void setAsRedefined(ClassLoader loader) {
        String name = internalName(loader.getClass().getName());
        if (!redefinedClassloaders.contains(name)) {
            redefinedClassloaders.add(name);
        }
    }

    public void show() {
        for (String string : redefinedClassloaders) {
            LOGGER.info("Classloader [{}]", string);
        }

        for (String className : modifiedClasses.keySet()) {
            LOGGER.info("Modified class [{}], path [{}]", className, modifiedClasses.get(className));
            
        }
    }

    public void setSubClassRef(String superName, String subClassName) {
        this.subClassRef.put(superName, subClassName);
    }

    public String getSubClassRef(String superName) {
        return this.subClassRef.get(superName);
    }
}
