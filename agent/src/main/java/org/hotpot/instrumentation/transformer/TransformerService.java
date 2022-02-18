package org.hotpot.instrumentation.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.instrumentation.helper.ModifiedClass;

public class TransformerService {
    private static Logger LOGGER = LogManager.getLogger(TransformerService.class);
    private Instrumentation inst;
    private Set<ClassLoader> classLoaders;
    private Path classPathConfFilePath;
    private Map<String, ModifiedClass> modifiedClasses;

    public TransformerService(Instrumentation inst, Path classPathConfFilePath) {
        this.inst = inst;
        this.classPathConfFilePath = classPathConfFilePath;
        this.classLoaders = new HashSet<>();
        this.modifiedClasses = new HashMap<>();
    }

    public void handle(Path modifiedClassFile) {
        if (!Files.isRegularFile(modifiedClassFile) || !modifiedClassFile.getFileName().toString().endsWith(".class")) {
            LOGGER.warn("[{}] may be not a class file or does not exist.", modifiedClassFile);
            return;
        }

        try {
            ModifiedClass modifiedClass = createModClass(modifiedClassFile);
            handle(modifiedClass);
        } catch (IOException e) {
            LOGGER.error("Could not read class file " + modifiedClassFile + ".", e);
        }
    }

    private ModifiedClass createModClass(Path modifiedClassFile) throws IOException {
        byte[] byteCode = Files.readAllBytes(modifiedClassFile);
        ModifiedClass modifiedClass = new ModifiedClass(byteCode, modifiedClassFile);
        collectClassInfo(modifiedClass);
        return modifiedClass;
    }

    private void collectClassInfo(ModifiedClass modifiedClass) {
        String modifiedClassName = modifiedClass.getName();
        InputStream origClassIS = ClassLoader
                            .getSystemResourceAsStream(modifiedClassName.replace(".", "/") + ".class");
        boolean classExists = origClassIS != null;

        modifiedClass.setClassExist(classExists);

        if(!classExists) {
            return;
        }

        Class<?>[] initiatedClasses = inst.getAllLoadedClasses();
        for (Class<?> clazz : initiatedClasses) {
            String initiatedClassName = clazz.getName();
            boolean isModifiableClass = inst.isModifiableClass(clazz);
            boolean modClassAllreadyLoaded = initiatedClassName.equals(modifiedClassName);

            if (isModifiableClass && modClassAllreadyLoaded) {
                modifiedClass.setModifiable(isModifiableClass);
                modifiedClass.setLoadedClass(clazz);
            } else if (isModifiableClass && !modClassAllreadyLoaded) {
                ClassLoader loader = clazz.getClassLoader();
                if (loader != null) {
                    InputStream classIS = ClassLoader
                            .getSystemResourceAsStream(initiatedClassName.replace(".", "/") + ".class");

                    boolean hasReference = ASMUtils.hasReference(classIS, modifiedClassName);
                    if (hasReference && inst.isModifiableClass(clazz)) {
                        LOGGER.info("Class [{}] has reference to [{}]", clazz.getName(), modifiedClassName);
                        modifiedClass.addClassWithRef(clazz);
                    }
                }
            }
        }
    }

    /**
     * <ol>
     *  <li>
     *      newly created Class, not exist at runtime
     *  </li>
     *  <li>
     *      modified class, class not loaded by classloader -> load from class file
     *  </li>
     *  <li>
     *      modified class, class allready loaded by classloader
     *      <ul>
     *          <li>
     *              only modified method body -> swap bytecode
     *          </li>
     *          <li> 
     *              add, remove or rename fields or methods, change the signatures of
     *              methods, or change inheritance -> subclassing and change references
     *          </li>
     *      </ul>
     *  </li>
     * </ol>
     * 
     * @param modifiedClass
     */
    public void handle(ModifiedClass modifiedClass) {
        String modifiedClassName = modifiedClass.getName();
        Class<?> loadedClass = modifiedClass.getLoadedClass();

        if (loadedClass == null) {
            LOGGER.info("Class [{}] not loaded, no retransformation.", modifiedClassName);
            try {
                Class.forName(modifiedClassName);
                ModifiedClass removeModifiedClass = modifiedClasses.remove(modifiedClassName);
                if (removeModifiedClass != null) {
                    try {
                        Files.delete(removeModifiedClass.getClassFilePath());
                    } catch (IOException e) {
                        LOGGER.error("Delete modified class file [{}] failed.", removeModifiedClass.getClassFilePath());
                        e.printStackTrace();
                    }
                }

                /* if(!modifiedClass.classExists()) {
                    return;
                } */
            } catch (ClassNotFoundException e) {
                LOGGER.info("Get class [{}] with Class.forName failed.", modifiedClassName);
            }
            return;
        }

        ClassLoader classLoader = loadedClass.getClassLoader();
        LOGGER.info("get classloader {} from {}", classLoader.getName(), loadedClass.getName());
        Class<? extends ClassLoader> classLoaderClass = classLoader.getClass();
        if (!classLoaders.contains(classLoaderClass)) {
            if (inst.isModifiableClass(classLoaderClass)) {
                LOGGER.info("Transform classloader [{}] of class [{}].", classLoaderClass.getName(), modifiedClassName);
                String methodName = "loadClass";
                ClassLoaderWithMethod classLoaderWithMethod = getClassLoaderWithMethod(classLoader, methodName,
                        String.class, boolean.class);

                if (classLoaderWithMethod == null) {
                    LOGGER.error("Retransform classloader [{}] of class [{}] failed.", classLoaderClass.getName(),
                            modifiedClassName);
                    return;
                }

                Class<?> targetclassLoaderClass = classLoaderWithMethod.classloader.getClass();
                String descriptor = ASMUtils.getMethodDescriptor(classLoaderWithMethod.method);

                ClassLoaderTransformer classLoaderTransformer = new ClassLoaderTransformer(classPathConfFilePath,
                        targetclassLoaderClass, methodName, descriptor);
                transform(classLoaderTransformer, targetclassLoaderClass);
                classLoaders.add(classLoaderWithMethod.classloader);
                // classLoaders.add(classLoaderWithMethod.classloader);
            } else {
                LOGGER.info("Transform classloader [{}] not modifiable.", classLoader.getName());
            }
        }

        ClassTransformer classTransformer = new ClassTransformer(modifiedClass);
        transform(classTransformer, loadedClass);
        ModifiedClass subClass = classTransformer.getSubClass();

        if (subClass != null) {
            // method changed
            String subClassName = subClass.getName();
            Path subClassFilePath = subClass.getClassFilePath();
            byte[] subClassByteCode = subClass.getByteCode();
            LOGGER.info("Subclass [{}] saved to file [{}].", subClassName, subClassFilePath);
            // deletes subclass file after loaded by Classloader
            // find all classes with reference to class
            // reset reference from class to subclass
            ClassReferenceTransformer classReferenceTransformer = new ClassReferenceTransformer(modifiedClassName,
                    subClassName);

            transform(classReferenceTransformer, modifiedClass.getClassesWithRef());

            try {
                Files.write(subClassFilePath, subClassByteCode);
                modifiedClasses.put(subClassName, subClass);
            } catch (IOException e) {
                LOGGER.error("Save subclass [{}] to file [{}] failed.", subClassName, subClassFilePath);
                e.printStackTrace();
            }
        }

        /*
         * Class[] initiatedClasses = inst.getInitiatedClasses(classLoader);
         * Path logFile =
         * classPathConfFilePath.getParent().resolve("initiatedClasses.txt");
         * 
         * try {
         * if(!Files.exists(logFile)) {
         * Files.createFile(logFile);
         * }
         * FileWriter fileWriter = new FileWriter(logFile.toFile(), true);
         * for (Class initiatedClass : initiatedClasses) {
         * fileWriter.write(initiatedClass.getName()+"\n");
         * }
         * } catch (IOException e) {
         * // TODO Auto-generated catch block
         * e.printStackTrace();
         * }
         */
    }

    private void transform(ClassFileTransformer transformer, Class<?>... targetCls) {
        inst.addTransformer(transformer, true);
        // String className = targetCls.getName();
        try {
            inst.retransformClasses(targetCls);
        } catch (UnmodifiableClassException e) {
            LOGGER.error(e);
        } finally {
            inst.removeTransformer(transformer);
        }
    }

    public Class<?> getLoadedClass(String className) {
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

    private ClassLoaderWithMethod getClassLoaderWithMethod(ClassLoader classloader, String methodName,
            Class<?>... parameterTypes) {
        Class<? extends ClassLoader> classloaderClass = classloader.getClass();
        try {
            Method declaredMethod = classloaderClass.getDeclaredMethod(methodName, parameterTypes);
            LOGGER.info("Class [{}] allready has method {}({})", classloaderClass.getName(), methodName,
                    parameterTypes);
            return new ClassLoaderWithMethod(classloader, declaredMethod);
        } catch (NoSuchMethodException e) {
            ClassLoader parentClassloader = classloader.getParent();
            if (parentClassloader == null) {
                LOGGER.error("Reach bootstraploader from {}({}).", classloaderClass.getName(), methodName,
                        parameterTypes);
                return null;
            }

            LOGGER.info("Try get method {}({}) from parent classloader [{}] of [{}]", methodName, parameterTypes,
                    parentClassloader.getName(), classloader.getName());
            return getClassLoaderWithMethod(parentClassloader, methodName, parameterTypes);
        } catch (SecurityException e) {
            LOGGER.error("In class [{}] exist no method {}({}).", classloader.getName(), methodName, parameterTypes);
            e.printStackTrace();
        }

        return null;
    }

    private class ClassLoaderWithMethod {
        ClassLoader classloader;
        Method method;

        public ClassLoaderWithMethod(ClassLoader classloader, Method method) {
            this.classloader = classloader;
            this.method = method;
        }
    }
}
