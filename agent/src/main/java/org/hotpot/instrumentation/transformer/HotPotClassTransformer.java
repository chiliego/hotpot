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
    private BiConsumer<Class<?>, byte[]> redefineFunc;

    public HotPotClassTransformer() {
        this.redefineClasses = new HashMap<>();
        this.redefinedClassloaders = new ArrayList<>();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        byte[] modifiedBytecode = getByteCode(className);
        if (modifiedBytecode != null) {
            if (!isRedefined(loader)) {
                redefine(loader, "loadClass", String.class, boolean.class);
            }

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
        }

        return null;
    }

    public void setConfFilePath(Supplier<Path> supplier) {
        this.confFilePathSupplier = supplier;
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
        String name = internalName(loader.getClass().getName());

        boolean contains = redefinedClassloaders.contains(name);
        return contains;
    }

    private void setAsRedefined(ClassLoader loader) {
        String name = internalName(loader.getClass().getName());
        if (!redefinedClassloaders.contains(name)) {
            redefinedClassloaders.add(name);
        }
    }

    private boolean redefine(ClassLoader loader, String methodName, Class<?>... parameterTypes) {
        try {
            Class<? extends ClassLoader> classloaderClass = loader.getClass();
            Method declaredMethod = classloaderClass.getDeclaredMethod(methodName, parameterTypes);
            boolean redefined = redefine(loader, declaredMethod);
            if (redefined) {
                setAsRedefined(loader);
            }
            return redefined;
        } catch (NoSuchMethodException e) {
            ClassLoader parentClassLoader = loader.getParent();
            if (parentClassLoader != null) {
                LOGGER.info("redefine parent [{}] of [{}].", parentClassLoader.getClass().getName(),
                        loader.getClass().getName());
                boolean redefined = redefine(parentClassLoader, methodName, parameterTypes);
                if (redefined) {
                    setAsRedefined(parentClassLoader);
                }
                return redefined;
            }
        } catch (SecurityException e) {
            LOGGER.error("In class [{}] exist no method {}({}).", loader.getName(), methodName, parameterTypes);
            e.printStackTrace();
        }

        return false;
    }

    private boolean redefine(ClassLoader loader, Method method) {
        ClassLoader parentClassLoader = loader.getParent();
        String loaderClassName = loader.getClass().getName();

        try (InputStream byteCodeIS = parentClassLoader
                .getResourceAsStream(internalName(loaderClassName) + ".class")) {
            if (confFilePathSupplier != null) {
                String methodName = method.getName();
                String descriptor = ASMUtils.getMethodDescriptor(method);
                String classPathConfFile = confFilePathSupplier.get().toAbsolutePath().toString();
                UnaryOperator<ClassVisitor> claddloaderAdapter = cv -> new ClassloaderAdapter(cv, methodName,
                        descriptor,
                        classPathConfFile);

                byte[] redefinedByteCode = ASMUtils.applyClassVisitor(byteCodeIS, claddloaderAdapter, false);
                if (redefinedByteCode != null) {
                    redefine(loader.getClass(), redefinedByteCode);
                    LOGGER.info("Redefine classloader [{}] done.", loader.getClass().getName());
                    return true;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not close class input stream for [{}]", loader.getClass().getName());
        }

        LOGGER.info("Redefine classloader [{}] failed.", loader.getClass().getName());
        return false;
    }

    private void redefine(Class<?> clazz, byte[] byteCode) {
        if (redefineFunc != null) {
            redefineFunc.accept(clazz, byteCode);
        }
    }
}