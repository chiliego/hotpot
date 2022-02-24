package org.hotpot.instrumentation.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.asm.ClassloaderAdapter;
import org.objectweb.asm.ClassVisitor;

public class HotPotClassTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger(HotPotClassTransformer.class);
    private Map<String, byte[]> redefineClasses;
    private Supplier<Path> confFilePathSupplier;
    private List<String> redefinedClassloaders;
    private Consumer<Class<?>[]> retransformFunc;
    private BiConsumer<Class<?>, byte[]> redefineFunc;

    public HotPotClassTransformer() {
        this.redefineClasses = new HashMap<>();
        this.redefinedClassloaders = new ArrayList<>();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        if (!isRedefined(loader)) {
            redefine(loader);
        }

        byte[] modifiedBytecode = getByteCode(className);
        if (modifiedBytecode != null) {
            try {
                boolean onlyChangeMethodBody = ASMUtils.onlyChangeMethodBody(classfileBuffer, modifiedBytecode);
                if (onlyChangeMethodBody) {
                    LOGGER.info("Redefine class [{}] success.", className);
                    return modifiedBytecode;
                } else {
                    LOGGER.error("Redefinition class [{}] failed.", className);
                }
            } finally {
                removeByteCode(className);
            }
        } else {
            LOGGER.error("No bytecode found for class [{}].", className);
        }

        return null;
    }

    public void setConfFilePath(Supplier<Path> supplier) {
        this.confFilePathSupplier = supplier;
    }

    public void setRetransformFunc(Consumer<Class<?>[]> retransformFunc) {
        this.retransformFunc = retransformFunc;
    }

    public void setRedefineFunc(BiConsumer<Class<?>, byte[]> redefineFunc) {
        this.redefineFunc = redefineFunc;
    }

    public void addModifiedClass(String className, Path classFilePath) {
        try {
            byte[] modifiedBytecode = Files.readAllBytes(classFilePath);
            addModifiedClass(className, modifiedBytecode);
        } catch (IOException e) {
            LOGGER.info("Add class [{}] from file [{}] to redefine List failed.", className, classFilePath);
            e.printStackTrace();
        }
    }
    
    public void addModifiedClass(String className, byte[] modifiedBytecode) {
        LOGGER.info("loader [{}]", getClass().getClassLoader().getClass().getName());
        redefineClasses.put(internalName(className), modifiedBytecode);
        LOGGER.info("Add class [{}] from file [{}] to redefine List.", className);
    }

    public byte[] getByteCode(String className) {
        return redefineClasses.get(internalName(className));
    }

    public byte[] removeByteCode(String className) {
        return redefineClasses.remove(internalName(className));
    }

    private String internalName(String className) {
        return className.replace(".", "/");
    }

    private boolean isRedefined(ClassLoader loader) {
        return redefinedClassloaders.contains(loader.getClass().getName());
    }

    private void setAsRedefined(ClassLoader loader) {
        redefinedClassloaders.add(loader.getClass().getName());
    }

    private byte[] redefine(ClassLoader loader) {
        ClassLoader parentClassLoader = loader.getParent();
        String loaderClassName = loader.getClass().getName();
        try(InputStream byteCodeIS = parentClassLoader.getResourceAsStream(internalName(loaderClassName) + ".class")) {
            return redefine(loader, byteCodeIS, "loadClass", String.class, boolean.class);
        } catch(IOException e) {
            LOGGER.error("Could not close class input stream for [{}]", loader.getClass().getName());
        }

        return null;
    }

    private byte[] redefine(ClassLoader classloader, InputStream byteCodeIS, String methodName, Class<?>... parameterTypes) {
        Class<? extends ClassLoader> classloaderClass = classloader.getClass();
        try {
            Method declaredMethod = classloaderClass.getDeclaredMethod(methodName, parameterTypes);
            return redefine(classloader, byteCodeIS, declaredMethod);
        } catch (NoSuchMethodException e) {
            ClassLoader parentClassloader = classloader.getParent();
            if (parentClassloader != null) {
                LOGGER.info("Try get method {}({}) from parent classloader [{}] of [{}]", methodName, parameterTypes,
                        parentClassloader.getName(), classloader.getName());
                return redefine(parentClassloader, byteCodeIS, methodName, parameterTypes);
            }
        } catch (SecurityException e) {
            LOGGER.error("In class [{}] exist no method {}({}).", classloader.getName(), methodName, parameterTypes);
            e.printStackTrace();
        }

        return null;
    }

    private byte[] redefine(ClassLoader classloader, InputStream byteCodeIS, Method method) {
        LOGGER.info("Redefine method {}({}) in classloader [{}].", classloader.getClass(), method.getName(),
                method.getParameterTypes());

        if (confFilePathSupplier != null) {
            String methodName = method.getName();
            String descriptor = ASMUtils.getMethodDescriptor(method);
            String classPathConfFile = confFilePathSupplier.get().toAbsolutePath().toString();
            UnaryOperator<ClassVisitor> claddloaderAdapter = cv -> new ClassloaderAdapter(cv, methodName, descriptor,
                    classPathConfFile);

            byte[] redefinedByteCode = ASMUtils.applyClassVisitor(byteCodeIS, claddloaderAdapter, false);
            addModifiedClass(classloader.getClass().getName(), redefinedByteCode);
            LOGGER.info("Redefine classloader [{}] done.", classloader.getClass().getName());
            setAsRedefined(classloader);
            //retransform(classloader.getClass());
            redefine(classloader.getClass(), redefinedByteCode);
                    
            return redefinedByteCode;
        }

        return null;
    }

    private void retransform(Class<?>... classes) {
        if (retransformFunc != null) {
            retransformFunc.accept(classes);
        }
    }

    private void redefine(Class<?> clazz, byte[] byteCode) {
        if (redefineFunc != null) {
            redefineFunc.accept(clazz, byteCode);
        }
    }
}