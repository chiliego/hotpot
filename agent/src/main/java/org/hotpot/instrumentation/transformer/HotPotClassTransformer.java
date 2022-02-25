package org.hotpot.instrumentation.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.asm.ClassloaderAdapter;
import org.hotpot.asm.SubClassAdapter;
import org.objectweb.asm.ClassVisitor;

public class HotPotClassTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger(HotPotClassTransformer.class);
    private Map<String, Path> redefineClasses;
    private Supplier<Path> confFilePathSupplier;
    private List<String> redefinedClassloaders;
    private BiConsumer<Class<?>, byte[]> redefineFunc;
    private Map<String, String> subClassRef;
    private Function<ClassLoader, Class<?>[]> initiatedClassesSupplier;
    private Consumer<Class<?>[]> retransformFunc;

    public HotPotClassTransformer() {
        this.redefineClasses = new HashMap<>();
        this.redefinedClassloaders = new ArrayList<>();
        this.subClassRef = new HashMap<>();
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
                // if redefine refs exisits -> redefine ref to modifiedBytecode
                boolean onlyChangeMethodBody = ASMUtils.onlyChangeMethodBody(classfileBuffer, modifiedBytecode);
                if (onlyChangeMethodBody) {
                    LOGGER.info("Redefine class [{}] success.", className);
                    return modifiedBytecode;
                } else {
                    LOGGER.info("Creating subclass of [{}].", className);
                    subClass(loader, className, modifiedBytecode);
                }
            } finally {
                removeFromRedefinedList(className);
            }
        }

        // if redefine refs exisits -> redefine ref to classfileBuffer

        return null;
    }

    public void setConfFilePath(Supplier<Path> supplier) {
        this.confFilePathSupplier = supplier;
    }

    public void setRedefineFunc(BiConsumer<Class<?>, byte[]> redefineFunc) {
        this.redefineFunc = redefineFunc;
    }

    public void addModifiedClass(String className, Path classFilePath) {
        LOGGER.info("Add class [{}] from file [{}] to redefine List.", className, classFilePath);
        redefineClasses.put(internalName(className), classFilePath);
    }

    /**
     * @param className will be converted to internal form automatically
     */
    public Path getClassFilePath(String className) {
        return redefineClasses.get(internalName(className));
    }

    public byte[] getByteCode(String className) {
        Path classFilePath = getClassFilePath(className);

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

    public Path removeFromRedefinedList(String className) {
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

    private void subClass(ClassLoader loader, String superName, byte[] byteCode) {
        // subclass bytecode
        // save class file
        // addModifiedClass(className, classFilePath); -> done via watchable
        // check if origClass allready subclasses -> rereferencing to new subClassname -> done at load
        // add origClassName -> subClassName to List
        // get all loaded class with ref to orig classname
        // redefine ref
        Path classFilePath = getClassFilePath(superName);

        if (classFilePath == null) {
            LOGGER.error("No class file path for [{}] found. Abort create subclass.", superName);
            return;
        }

        long currentTimeMillis = System.currentTimeMillis();
        String subClassName = superName + "_" + currentTimeMillis;

        LOGGER.info("Creating subclass [{}] of [{}].", subClassName, superName);
        UnaryOperator<ClassVisitor> subclassAdapter = cv -> new SubClassAdapter(cv, subClassName);
        byte[] subClassByteCode = ASMUtils.applyClassVisitor(byteCode, subclassAdapter, false);

        if (subClassByteCode == null) {
            LOGGER.error("Creating bytecode for subclass [{}] of [{}] failed.", subClassName, superName);
            return;
        }

        Path subClassFileName = Paths.get(subClassName + ".class").getFileName();
        Path subClassFilePath = classFilePath
                .getParent()
                .resolve(subClassFileName);

        try {
            Files.write(subClassFilePath, subClassByteCode);
        } catch (IOException e) {
            LOGGER.error("Save subclass [{}] to file [{}] failed.", subClassName, subClassFilePath);
            e.printStackTrace();
            return;
        }

        redefineClassRef(loader, superName, subClassName);
    }

    private void redefineClassRef(ClassLoader loader, String superName, String subClassName) {
        String classRef = superName;
        String subClassRef = getSubClassRef(classRef);

        if (subClassRef != null) {
            classRef = subClassRef;
        }

        subClassRef = subClassName;
        setSubClassRef(classRef, subClassRef);

        ArrayList<Class<?>> classesWithRef = new ArrayList<>();
        for (Class<?> initiatedClass : getInitiatedClasses(loader)) {
            String initiatedClassName = initiatedClass.getName();
            String initiatedClassFile = initiatedClassName.replace(".", "/") + ".class";
            InputStream classIS = loader.getResourceAsStream(initiatedClassFile);
            boolean hasReference = ASMUtils.hasReference(classIS, classRef);
            if (hasReference) {
                LOGGER.info("initiatedClass: [{}] has ref to [{}]", initiatedClass.getName(), classRef);
                classesWithRef.add(initiatedClass);
            }
        }

        if (!classesWithRef.isEmpty()) {
            Class<?>[] classesArray = classesWithRef.toArray(new Class<?>[classesWithRef.size()]);
            retransform(classesArray);
        }
    }

    public void setRetransformFunc(Consumer<Class<?>[]> retransformFunc) {
        this.retransformFunc = retransformFunc;
    }

    private void retransform(Class<?>[] classesArray) {
        if (this.retransformFunc == null) {
            LOGGER.error("retransformFunc not set.");
            return;
        }

        this.retransformFunc.accept(classesArray);
    }

    private void setSubClassRef(String superName, String subClassName) {
        this.subClassRef.put(superName, subClassName);
    }

    private String getSubClassRef(String superName) {
        return this.subClassRef.get(superName);
    }

    public void setInitiatedClassesSupplier(Function<ClassLoader, Class<?>[]> initiatedClassesSupplier) {
        this.initiatedClassesSupplier = initiatedClassesSupplier;
    }

    private Class<?>[] getInitiatedClasses(ClassLoader loader) {
        if (this.initiatedClassesSupplier == null) {
            LOGGER.error("initiatedClassesSupplier not set.");
            return new Class<?>[0];
        }

        return this.initiatedClassesSupplier.apply(loader);
    }
}