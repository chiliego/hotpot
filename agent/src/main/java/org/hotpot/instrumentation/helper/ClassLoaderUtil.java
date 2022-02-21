package org.hotpot.instrumentation.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ClassLoaderUtil {
    private Map<String, ClassLoader> classloaderMap;
    private Supplier<Class<?>[]> loadedClassesSupplier;
    private Function<Class<?>, Boolean> isModifiableFunc;

    public ClassLoaderUtil(Supplier<Class<?>[]> loadedClassesSupplier, Function<Class<?>, Boolean> isModifiableFunc) {
        this.classloaderMap = new HashMap<>();
        this.loadedClassesSupplier = loadedClassesSupplier;
        this.isModifiableFunc = isModifiableFunc;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassNotFoundException exp = null;
        for (ClassLoader classLoader : classloaderMap.values()) {
            try {
                return classLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
                exp = e;
            }
        }

        Class<?>[] allLoadedClasses = loadedClassesSupplier.get();
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        String sysClName = systemClassLoader.getClass().getName();
        ClassLoader platformClassLoader = ClassLoader.getPlatformClassLoader();
        String platClName = platformClassLoader.getClass().getName();

        for (Class<?> clazz : allLoadedClasses) {
            if (isModifiableFunc.apply(clazz)) {
                ClassLoader classLoader = clazz.getClassLoader();
                
                if(classLoader != null) {
                    String clName = classLoader.getClass().getName();
                    if (!clName.equals(sysClName) && !clName.equals(platClName) && !classloaderMap.containsKey(clName)) {
                        try {
                            return classLoader.loadClass(name);
                        } catch (ClassNotFoundException e) {
                            exp = e;
                            classloaderMap.put(clName, classLoader);
                        }
                        
                    }
                }
            }   
        }

        if (exp == null) {
            exp = new ClassNotFoundException("Could not load class " + name);
        }

        throw exp;
    }

    public ClassLoader getClassLoader(String className) throws IOException {
        IOException exp = null;

        for (ClassLoader classLoader : classloaderMap.values()) {
            try(InputStream resourceAsStream = classLoader.getResourceAsStream(className.replace(".", "/") + ".class")) {
                if (resourceAsStream != null) {
                    return classLoader;
                }
            } catch (IOException e) {
                exp = e;
            }
        }

        Class<?>[] allLoadedClasses = loadedClassesSupplier.get();
        Predicate<String> excludesClassLoader = name -> name.endsWith(".DelegatingClassLoader");
        excludesClassLoader = excludesClassLoader
            .or(name -> name.endsWith("$DynamicClassLoader"))
            .or(name -> classloaderMap.containsKey(name));

        for (Class<?> clazz : allLoadedClasses) {
            if (isModifiableFunc.apply(clazz)) {
                ClassLoader classLoader = clazz.getClassLoader();
                
                if(classLoader != null) {
                    String clName = classLoader.getClass().getName();
                    if (excludesClassLoader.negate().test(clName)) {
                        try(InputStream resourceAsStream = classLoader.getResourceAsStream(className.replace(".", "/") + ".class")) {
                            if (resourceAsStream != null) {
                                return classLoader;
                            }
                        } catch (IOException e) {
                            exp = e;
                        }
                    }
                }
            }   
        }

        if (exp == null) {
            exp = new IOException("Could not get class [" + className + "] as resource stream with any classloader.");
        }

        throw exp;
    }
}
