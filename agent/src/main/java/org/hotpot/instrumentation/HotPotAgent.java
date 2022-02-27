package org.hotpot.instrumentation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.asm.SubClassAdapter;
import org.hotpot.events.ClassPathHandler;
import org.hotpot.events.CmdPathHandler;
import org.hotpot.events.PathWatchService;
import org.objectweb.asm.ClassVisitor;

public class HotPotAgent {
    private static Logger LOGGER = LogManager.getLogger(HotPotAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] premain delegate to agentmain");
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] agentmain");

        String hotpotHomeFolder = agentArgs;
        Path hotpotHomePath = null;
        if (hotpotHomeFolder != null) {
            hotpotHomePath = Paths.get(hotpotHomeFolder);
        } else {
            String userHomeProp = System.getProperty("user.home");
            hotpotHomePath = Paths.get(userHomeProp).resolve(".hotpot");
        }

        Path classPathconfigPath = hotpotHomePath.resolve("conf");
        Path cmdPath = hotpotHomePath.resolve("cmd");
        try {
            createDir(hotpotHomePath);
            createDir(classPathconfigPath);
            createDir(cmdPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        LOGGER.info("Using config dir [{}].", classPathconfigPath);
        ClassPathHandler classPathHandler = new ClassPathHandler(classPathconfigPath);
        CmdPathHandler cmdPathHandler = new CmdPathHandler(cmdPath);

        PathWatchService pathWatchService = new PathWatchService();
        pathWatchService.addHandlers(classPathHandler, cmdPathHandler);

        handleChangesInClassPath(classPathHandler);
        handleCmdExecute(classPathHandler, cmdPathHandler, inst);

        new Thread(pathWatchService).start();
    }

    private static void handleChangesInClassPath(ClassPathHandler classPathHandler) {
        ModifiedClasses modifiedClasses = ModifiedClasses.getInstance();
        classPathHandler.onCreate(modifiedClasses::add);// need check if added class is subclass, then not add
        classPathHandler.onModify(modifiedClasses::add);
    }

    private static void handleCmdExecute(ClassPathHandler classPathHandler, CmdPathHandler cmdPathHandler,
            Instrumentation inst) {
        cmdPathHandler.addCmd("retransform", cmdFile -> forLoadedClasseWithMod(classPathHandler, inst));
        cmdPathHandler.addCmd("listClasses", cmdFile -> ModifiedClasses.getInstance().show());
    }

    private static void forLoadedClasseWithMod(ClassPathHandler classPathHandler, Instrumentation inst) {
        ModifiedClasses modifiedClasses = ModifiedClasses.getInstance();
        BiConsumer<ClassLoader, byte[]> redefiner = (cl, b) -> redefine(inst, cl.getClass(), b);
        ;
        Path confFilePath = classPathHandler.getConfFilePath();

        for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
            String className = loadedClass.getName();
            if (modifiedClasses.has(className)) {
                ClassLoader classLoader = loadedClass.getClassLoader();
                if (!modifiedClasses.isRedefined(classLoader)) {
                    ClassLoaderUtils.redefineCl(classLoader, confFilePath, modifiedClasses::setAsRedefined, redefiner);
                }

                byte[] modifiedBytecode = modifiedClasses.getByteCode(className);
                if (onlyChangeMethodBody(loadedClass, modifiedBytecode)) {
                    LOGGER.info("Redefine class [{}] success.", className);
                    redefine(inst, loadedClass, modifiedBytecode);
                } else {
                    LOGGER.info("Creating subclass of [{}].", className);
                    subClass(inst, loadedClass, modifiedBytecode);
                }
            }
        }
    }

    private static boolean onlyChangeMethodBody(Class<?> loadedClass, byte[] modifiedBytecode) {
        try {
            return ASMUtils.onlyChangeMethodBody(loadedClass, modifiedBytecode);
        } catch (IOException e) {
            // We could get class bytecode from resource
        }

        String internalClassName = loadedClass.getName().replace(".", "/");
        byte[] byteCode = ModifiedClasses.getInstance().getByteCode(internalClassName);
        if (byteCode != null) {
            return ASMUtils.onlyChangeMethodBody(byteCode, modifiedBytecode);
        }

        return false;
    }

    private static void createDir(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectory(dir);
                LOGGER.info("Directory [{}] created.", dir);
                return;
            } catch (IOException e) {
                LOGGER.error("Creating directory [{}] failed.", dir);
                e.printStackTrace();
                ;
            }
        }
    }

    private static void subClass(Instrumentation inst, Class<?> clazz, byte[] byteCode) {
        // subclass bytecode
        // save class file
        // addModifiedClass(className, classFilePath); -> done via watchable
        // check if origClass allready subclasses -> rereferencing to new subClassname -> done at load
        // add origClassName -> subClassName to List
        // get all loaded class with ref to orig classname
        // redefine ref
        String superName = clazz.getName().replace(".", "/");
        Path classFilePath = ModifiedClasses.getInstance().getClassFilePath(superName);

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

        redefineClassRef(inst, clazz, subClassName);
    }

    private static void redefineClassRef(Instrumentation inst, Class<?> clazz, String subClassName) {
        String classRef = clazz.getName();
        ModifiedClasses modifiedClasses = ModifiedClasses.getInstance();
        String subClassRef = modifiedClasses.getSubClassRef(classRef);

        if (subClassRef != null) {
            classRef = subClassRef;
        }

        subClassRef = subClassName;
        modifiedClasses.setSubClassRef(classRef, subClassRef);

        ClassLoader loader = clazz.getClassLoader();
        for (Class<?> initiatedClass : inst.getInitiatedClasses(loader)) {
            String initiatedClassName = initiatedClass.getName();
            String initiatedClassFile = initiatedClassName.replace(".", "/") + ".class";
            byte[] byteCodeWithNewRef = null;
            try (InputStream classIS = loader.getResourceAsStream(initiatedClassFile)) {
                if (classIS != null) {
                    byteCodeWithNewRef = ASMUtils.redefineReference(classRef, subClassName, classIS);
                }
            } catch (IOException e) {
                LOGGER.error("Could not close IS for class [{}].", initiatedClassName);
            }

            if (byteCodeWithNewRef == null) {
                byte[] classByteCode = modifiedClasses.getByteCode(initiatedClassName);
                if (classByteCode != null) {
                    byteCodeWithNewRef = ASMUtils.redefineReference(classRef, subClassName, classByteCode);
                }
            }

            if (byteCodeWithNewRef != null) {
                redefine(inst, initiatedClass, byteCodeWithNewRef);
            }
        }
    }

    public static void redefine(Instrumentation inst, Class<?> theClass, byte[] theClassFile) {
        ClassDefinition definition = new ClassDefinition(theClass, theClassFile);

        try {
            inst.redefineClasses(definition);
        } catch (ClassNotFoundException e) {
            LOGGER.info("Redefine class [{}]", theClass.getName());
            e.printStackTrace();
        } catch (UnmodifiableClassException e) {
            LOGGER.info("Redefine failed, class [{}] is unmodifiable.", theClass.getName());
            e.printStackTrace();
        }
    }
}
