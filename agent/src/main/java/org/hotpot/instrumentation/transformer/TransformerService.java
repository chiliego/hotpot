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
import java.util.Map;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.instrumentation.helper.ModifiedClass;

public class TransformerService {
    private static Logger LOGGER = LogManager.getLogger(TransformerService.class);
    private Instrumentation inst;
    private Path classPathConfFilePath;
    private Map<String, ModifiedClass> modifiedClasses;
    private Map<String, ClassLoader> classloaderMap;

    public TransformerService(Instrumentation inst) {
        this.inst = inst;
        this.modifiedClasses = new HashMap<>();
        this.classloaderMap = new HashMap<>();
    }
    
    public TransformerService(Instrumentation inst, Path classPathConfFilePath) {
        this.inst = inst;
        this.classPathConfFilePath = classPathConfFilePath;
        this.modifiedClasses = new HashMap<>();
        this.classloaderMap = new HashMap<>();
    }

    public void handle(Path modifiedClassFile) {
        if (!Files.exists(modifiedClassFile)) {
            return;
        }

        if (!Files.isRegularFile(modifiedClassFile) || !modifiedClassFile.getFileName().toString().endsWith(".class")) {
            LOGGER.warn("[{}] may be not a class file.", modifiedClassFile);
            return;
        }
        
        if(isSubclass(modifiedClassFile)){
            return;
        }

        try {
            ModifiedClass modifiedClass = createModClass(modifiedClassFile);
            if (!modifiedClass.isModifiable()) {
                LOGGER.error("Class [{}] is not modifiable.", modifiedClass.getName());
                return;
            }
            handle(modifiedClass);
        } catch (IOException e) {
            LOGGER.error("Could not read class file " + modifiedClassFile + ".", e);
        }
    }

    private boolean isSubclass(Path modifiedClassFile) {
        for (ModifiedClass modClass : modifiedClasses.values()) {
            boolean isSubclass = modClass.isSubclass();
            boolean isSameFile = false;
            try {
                isSameFile = Files.isSameFile(modClass.getClassFilePath(), modifiedClassFile);
            } catch (IOException e) {
                // seems to be not same file
            }

            if (isSubclass && isSameFile) {
                return true;
            }
        } 

        return false;
    }

    private ModifiedClass createModClass(Path modifiedClassFile) throws IOException {
        byte[] byteCode = Files.readAllBytes(modifiedClassFile);
        ModifiedClass modifiedClass = new ModifiedClass(byteCode, modifiedClassFile);
        collectClassInfo(modifiedClass);
        return modifiedClass;
    }

    private void collectClassInfo(ModifiedClass modifiedClass) {
        String modifiedClassName = modifiedClass.getName();

        Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();
        Predicate<String> excludesClassLoader = name -> name.endsWith(".DelegatingClassLoader");
        excludesClassLoader = excludesClassLoader
                .or(name -> name.endsWith("$DynamicClassLoader"));

        for (Class<?> clazz : allLoadedClasses) {
            String loadedClassName = clazz.getName();
            boolean isModifiableClass = inst.isModifiableClass(clazz);
            boolean modClassAllreadyLoaded = loadedClassName.equals(modifiedClassName);

            if (modClassAllreadyLoaded) {
                modifiedClass.setModifiable(isModifiableClass);
                modifiedClass.setLoadedClass(clazz);
            } else if (isModifiableClass && !modClassAllreadyLoaded) {
                ClassLoader classLoader = clazz.getClassLoader();

                if (classLoader != null) {
                    String clName = classLoader.getClass().getName();
                    if (excludesClassLoader.negate().test(clName)) {
                        String loadedClassFile = loadedClassName.replace(".", "/") + ".class";
                        InputStream classIS = classLoader.getResourceAsStream(loadedClassFile);
                        boolean hasReference = ASMUtils.hasReference(classIS, modifiedClassName);
                        if (hasReference) {
                            modifiedClass.addClassWithRef(clazz);
                        }
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

        LOGGER.info("class [{}] loaded class [{}] and exist [{}]", modifiedClassName, loadedClass,
                modifiedClass.classExists());

        if (loadedClass == null) {
            LOGGER.info("Class [{}] not loaded, will be load when needed.", modifiedClassName);
            return;
        }

        ClassLoader classloader = modifiedClass.getClassloader();
        String clClassName = classloader.getClass().getName();

        LOGGER.info("Class [{}] has classloader [{}]", classloader.getName(), loadedClass.getName());
        Class<? extends ClassLoader> classLoaderClass = classloader.getClass();
        if (!classloaderMap.containsKey(clClassName)) {
            if (inst.isModifiableClass(classLoaderClass)) {
                LOGGER.info("Transform classloader [{}] of class [{}].", modifiedClassName, clClassName);
                String methodName = "loadClass";
                ClassLoaderWithMethod classLoaderWithMethod = getClassLoaderWithMethod(classloader, methodName,
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
                //classLoaders.add(classLoaderWithMethod.classloader);
                classloaderMap.put(clClassName, classloader);
                // classLoaders.add(classLoaderWithMethod.classloader);
            } else {
                LOGGER.info("Transform classloader [{}] not modifiable.", classloader.getName());
            }
        }

        ClassTransformer classTransformer = new ClassTransformer(modifiedClass);
        transform(classTransformer, loadedClass);
        ModifiedClass subClass = classTransformer.getModClass();
        for (Class<?> classWithRef : subClass.getClassesWithRef()) {
            LOGGER.info("Class [{}] has ref to [{}]", classWithRef, subClass.getName());
        }
        LOGGER.info("Class [{}] is subclass [{}]",  subClass.getName(), subClass.isSubclass());

        if (subClass.isSubclass()) {
            // method changed
            String subClassName = subClass.getName();
            Path subClassFilePath = subClass.getClassFilePath();
            byte[] subClassByteCode = subClass.getByteCode();
            LOGGER.info("Subclass [{}] saved to file [{}].", subClassName, subClassFilePath);
            // deletes subclass file after loaded by Classloader
            // find all classes with reference to class
            // reset reference from class to subclass
            updateClassRefs(subClass.getSuperName(), subClassName, modifiedClass.getClassesWithRef());

            try {
                Files.write(subClassFilePath, subClassByteCode);
                modifiedClasses.put(subClass.getSuperName(), subClass);
            } catch (IOException e) {
                LOGGER.error("Save subclass [{}] to file [{}] failed.", subClassName, subClassFilePath);
                e.printStackTrace();
            }
        }
    }

    private void updateClassRefs(String modifiedClassName, String subClassName, Class<?>[] classesWithRef) {
        ClassReferenceTransformer classReferenceTransformer = new ClassReferenceTransformer(modifiedClassName,
                subClassName);
        transform(classReferenceTransformer, classesWithRef);
    }

    private void transform(ClassFileTransformer transformer, Class<?>... targetCls) {
        inst.addTransformer(transformer, true);
        // String className = targetCls.getName();
        try {
            LOGGER.info("before retransform using [{}]", transformer.getClass());
            for (Class<?> clazz : targetCls) {
                LOGGER.info("before retransform target [{}]", clazz);
            }
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

    public void addModified(Path classFilePath) {
        
    }
}
