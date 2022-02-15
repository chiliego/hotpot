package org.hotpot.instrumentation.transformer;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;

public class TransformerService {
    private static Logger LOGGER = LogManager.getLogger(TransformerService.class);
    private Instrumentation inst;
    private Set<Class<?>> classLoaders;
    private Path classPathConfFilePath;

    public TransformerService(Instrumentation inst, Path classPathConfFilePath) {
        this.inst = inst;
        this.classPathConfFilePath = classPathConfFilePath;
        this.classLoaders = new HashSet<>();
    }

    public void handle(Path modifiedClassFile) {
        if (!modifiedClassFile.getFileName().toString().endsWith(".class")) {
            LOGGER.warn("[{}] may be not a class file.", modifiedClassFile);
            return;
        }

        try {
            byte[] byteCode = Files.readAllBytes(modifiedClassFile);
            handle(byteCode);
        } catch (IOException e) {
            LOGGER.error("Could not read class file " + modifiedClassFile + ".", e);
        }
    }

    public void handle(byte[] modifiedByteCode) {
        String className = ASMUtils.getClassName(modifiedByteCode);
        Class<?> targetCls = getClass(className);

        if (targetCls == null) {
            LOGGER.info("Could not get class [{}], retransform failed.", className);
            return;
        }

        ClassLoader classLoader = targetCls.getClassLoader();
        Class<? extends ClassLoader> classLoaderClass = classLoader.getClass();
        if (!classLoaders.contains(classLoaderClass)) {
            if (inst.isModifiableClass(classLoaderClass)) {
                LOGGER.info("Transform classloader [{}] of class [{}].", classLoaderClass.getName(), className);
                String methodName = "loadClass";
                ClassWithMethod classWithMethod = getSuperClassWithMethod(classLoaderClass, methodName, String.class,
                        boolean.class);

                if (classWithMethod == null) {
                    LOGGER.error("Retransform classloader [{}] of class [{}] failed.", classLoaderClass.getName(),
                            className);
                    return;
                }

                Class<?> targetclassLoaderClass = classWithMethod.clazz;
                String descriptor = ASMUtils.getMethodDescriptor(classWithMethod.method);

                ClassLoaderTransformer classLoaderTransformer = new ClassLoaderTransformer(classPathConfFilePath,
                        targetclassLoaderClass, methodName, descriptor);
                transform(targetclassLoaderClass, classLoaderTransformer);
                classLoaders.add(classLoaderClass);
            } else {
                LOGGER.info("Transform classloader [{}] not modifiable.", classLoader.getName());
            }
        }

        SwapClassTransformer transformer = new SwapClassTransformer(targetCls, modifiedByteCode);
        transform(targetCls, transformer);
    }

    private void transform(Class<?> targetCls, ClassFileTransformer transformer) {
        inst.addTransformer(transformer, true);
        String className = targetCls.getName();
        try {
            LOGGER.info("Retransform [{}].", targetCls);
            inst.retransformClasses(targetCls);
        } catch (UnmodifiableClassException e) {
            LOGGER.error("Can not retransform class [{}].", className);
            LOGGER.error(e);
        } finally {
            inst.removeTransformer(transformer);
        }
    }

    public Class<?> getClass(String className) {
        LOGGER.info("Try get class [{}] with Class.forName...", className);
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.info("Get class [{}] with Class.forName failed.", className);
        }

        LOGGER.info("Try get class [{}]  with Instrumentation.getAllLoadedClasses...", className);
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        LOGGER.info("Class [{}] not found in all loaded class.", className);

        return null;
    }

    private ClassWithMethod getSuperClassWithMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        String className = clazz.getName();
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, parameterTypes);
            LOGGER.info("Class [{}] allready has method {}({})", clazz.getName(), methodName, parameterTypes);
            return new ClassWithMethod(clazz, declaredMethod);
        } catch (NoSuchMethodException e) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass == null) {
                LOGGER.error("No superclass of [{}] found to check for method [{}].", className, methodName);
                return null;
            }

            LOGGER.info("Try get method {}({}) from superclass [{}] of [{}]", methodName, parameterTypes,
                    superclass.getName(), className);
            return getSuperClassWithMethod(superclass, methodName, parameterTypes);
        } catch (SecurityException e) {
            LOGGER.error("Could not get method [{}] of class [{}].", methodName, className);
            e.printStackTrace();
        }
        return null;
    }

    private class ClassWithMethod {
        Class<?> clazz;
        Method method;

        public ClassWithMethod(Class<?> clazz, Method method) {
            this.clazz = clazz;
            this.method = method;
        }
    }

}
