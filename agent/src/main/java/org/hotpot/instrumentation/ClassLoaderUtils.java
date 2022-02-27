package org.hotpot.instrumentation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.asm.ClassloaderAdapter;
import org.objectweb.asm.ClassVisitor;

public class ClassLoaderUtils {
    private static Logger LOGGER = LogManager.getLogger(ClassLoaderUtils.class);
    private Supplier<Path> confFilePathSupplier;
    private List<String> redefinedClassloaders;
    private BiConsumer<Class<?>, byte[]> redefineFunc;

    public ClassLoaderUtils() {
        this.redefinedClassloaders = new ArrayList<>();
    }

    public static void redefineCl(ClassLoader classloader, Path confFilePath, Consumer<List<ClassLoader>> setAsRedefined,
            BiConsumer<ClassLoader, byte[]> redefiner) {
        List<ClassLoader> redefinedClassLoaders = new ArrayList<>();
        byte[] redefinedByteCode = null;
        ClassLoader loader = classloader;
        while (loader != null) {
            Class<? extends ClassLoader> classloaderClass = loader.getClass();
            redefinedClassLoaders.add(loader);
            try {
                Method declaredMethod = classloaderClass.getDeclaredMethod("loadClass", String.class, boolean.class);
                redefinedByteCode = redefineCl(loader, declaredMethod, confFilePath.toString());
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

    public void redefine(ClassLoader loader) {
        if (!isRedefined(loader)) {// can checked before call this method, eg. im ModifiedClasses class
            redefine(loader, "loadClass", String.class, boolean.class);
        }
    }

    public void setRedefineFunc(BiConsumer<Class<?>, byte[]> redefineFunc) {
        this.redefineFunc = redefineFunc;
    }

    public void setConfFilePath(Supplier<Path> supplier) {
        this.confFilePathSupplier = supplier;
    }

    private static byte[] redefineCl(String classPathConfFile, ClassLoader loader, String methodName,
            Class<?>... parameterTypes) {
        List<String> redefinedClassLoaders = new ArrayList<>();
        while (loader != null) {
            Class<? extends ClassLoader> classloaderClass = loader.getClass();
            redefinedClassLoaders.add(classloaderClass.getName());
            try {
                Method declaredMethod = classloaderClass.getDeclaredMethod(methodName, parameterTypes);
                byte[] redefinedByteCode = redefineCl(loader, declaredMethod, classPathConfFile);
                if (redefinedByteCode != null) {
                    LOGGER.info("Redefine classloader [{}] done.", loader.getClass().getName());
                    return redefinedByteCode;
                }
            } catch (NoSuchMethodException e) {
                // OK we check if we redefine parent class
            } catch (SecurityException e) {
                // OK we check if we redefine parent class
            }

            loader = loader.getParent();
        }
        return null;
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

    private static byte[] redefineCl(ClassLoader loader, Method method, String classPathConfFile) {
        ClassLoader parentClassLoader = loader.getParent();
        String loaderClassName = loader.getClass().getName();

        try (InputStream byteCodeIS = parentClassLoader
                .getResourceAsStream(loaderClassName.replace(".", "/") + ".class")) {
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

    private String internalName(String className) {
        return className.replace(".", "/");
    }

    private void setAsRedefined(ClassLoader loader) {
        String name = internalName(loader.getClass().getName());
        if (!redefinedClassloaders.contains(name)) {
            redefinedClassloaders.add(name);
        }
    }

    private boolean isRedefined(ClassLoader loader) {
        String name = internalName(loader.getClass().getName());

        boolean contains = redefinedClassloaders.contains(name);
        return contains;
    }
}
