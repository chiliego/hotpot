package org.hotpot.instrumentation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.asm.ClassloaderAdapter;
import org.hotpot.events.ClassPathHandler;
import org.hotpot.events.CmdPathHandler;
import org.objectweb.asm.ClassVisitor;

public class HotPot {
    private static Logger LOGGER = LogManager.getLogger(HotPot.class);

    public static void cook(ClassPathHandler classPathHandler, CmdPathHandler cmdPathHandler, Instrumentation inst) {
        ClassPool classPool = new ClassPool();
        BiConsumer<String, Path> observer = (className, classPath) -> observer(className, classPath, classPool, inst);

        //classPathHandler.onCreate(observer);
        List<ClassLoader> transformedClassloaders = transformClassloader(inst,
                classPathHandler.getConfFilePath().toAbsolutePath().toString());
        classPathHandler.onModify(observer);
    }

    public static List<ClassLoader> transformClassloader(Instrumentation inst, String classPathConfFile) {
        ArrayList<ClassLoader> classloaders = new ArrayList<>();
        ArrayList<Class<?>> classloadersWithLoadClass = new ArrayList<>();
        ArrayList<ClassDefinition> classDefinitions = new ArrayList<>();
        for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = loadedClass.getClassLoader();
            if (classLoader != null && !classloaders.contains(classLoader)) {
                Class<?> classLoaderClass = classLoader.getClass();
                while (!classloadersWithLoadClass.contains(classLoaderClass)) {
                    classloadersWithLoadClass.add(classLoaderClass);
                    try {
                        Method declaredMethod = classLoaderClass.getDeclaredMethod("loadClass", String.class,
                        boolean.class);
                        if (inst.isModifiableClass(classLoaderClass)) {
                            byte[] modifedByteCode = modifyClassLoader(classLoaderClass, declaredMethod,
                            classPathConfFile);
                            ClassDefinition definition = new ClassDefinition(classLoaderClass, modifedByteCode);
                            classDefinitions.add(definition);
                            LOGGER.info("Create new class definition for [{}].", classLoaderClass.getName());
                            classloaders.add(classLoader);
                        }
                        break;
                    } catch (NoSuchMethodException e) {
                        // do Nothing
                    } catch (SecurityException e) {
                        // do Nothing
                    }
                    classLoaderClass = classLoaderClass.getSuperclass();
                }
            }
        }

        ClassDefinition[] classDefArray = classDefinitions.toArray(new ClassDefinition[classDefinitions.size()]);
        try {
            inst.redefineClasses(classDefArray);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Redefine class [{}] failed");
            e.printStackTrace();
        } catch (UnmodifiableClassException e) {
            LOGGER.error("Redefine failed, class [{}] is unmodifiable.");
            e.printStackTrace();
        }

        return classloaders;
    }

    public static void observer(String className, Path classPath, ClassPool classPool, Instrumentation inst) {
        //classPool.addClassPath(className, classPath);
        //Class<?> loadedClass = classPool.loadClass(className);

        LOGGER.info("On Modify class [{}] path [{}].", className, classPath);
    }

    public static void redefineCl(ClassLoader classloader, Path confFilePath,
            Consumer<List<ClassLoader>> setAsRedefined,
            BiConsumer<ClassLoader, byte[]> redefiner) {
        List<ClassLoader> redefinedClassLoaders = new ArrayList<>();
        byte[] redefinedByteCode = null;
        ClassLoader loader = classloader;
        while (loader != null) {
            Class<? extends ClassLoader> classloaderClass = loader.getClass();
            redefinedClassLoaders.add(loader);
            try {
                Method declaredMethod = classloaderClass.getDeclaredMethod("loadClass", String.class, boolean.class);
                redefinedByteCode = redefineCl1(loader, declaredMethod, confFilePath.toString());
                setAsRedefined.accept(redefinedClassLoaders);
                redefiner.accept(loader, redefinedByteCode);
                LOGGER.info("redefine loader [{}]", loader.getClass().getName());
                return;
            } catch (NoSuchMethodException e) {
                // OK we check if we redefine parent class
            } catch (SecurityException e) {
                // OK we check if we redefine parent class
            }

            loader = loader.getParent();
        }

        LOGGER.error("Redefine classloader [{}] failed.", classloader.getName());
    }

    private static byte[] redefineCl1(ClassLoader loader, Method method, String classPathConfFile) {
        return null;
    }

    public static InputStream getClassIS(Class<?> theClass) {
        String classFileName = theClass.getName().replace(".", "/") + ".class";
        ClassLoader classLoader = theClass.getClassLoader();

        if (classLoader != null) {
            return classLoader.getResourceAsStream(classFileName);
        }

        return ClassLoader.getSystemResourceAsStream(classFileName);
    }

    private static byte[] modifyClassLoader(Class<?> loader, Method method, String classPathConfFile) {
        try (InputStream byteCodeIS = getClassIS(loader)) {
            String methodName = method.getName();
            String descriptor = ASMUtils.getMethodDescriptor(method);
            UnaryOperator<ClassVisitor> claddloaderAdapter = cv -> new ClassloaderAdapter(cv, methodName,
                    descriptor,
                    classPathConfFile);

            return ASMUtils.applyClassVisitor(byteCodeIS, claddloaderAdapter, false);
        } catch (IOException e) {
            LOGGER.error("Could not close class input stream for [{}]", loader.getClass().getName());
        }

        LOGGER.info("Redefine classloader [{}] failed.", loader.getClass().getName());
        return null;
    }

    public static void redefine(Instrumentation inst, Class<?> theClass, byte[] theClassFile) {
        ClassDefinition definition = new ClassDefinition(theClass, theClassFile);

        try {
            inst.redefineClasses(definition);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Redefine class [{}]", theClass.getName());
            e.printStackTrace();
        } catch (UnmodifiableClassException e) {
            LOGGER.error("Redefine failed, class [{}] is unmodifiable.", theClass.getName());
            e.printStackTrace();
        }
    }
}
