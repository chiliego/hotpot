package org.hotpot.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.events.ClassPathHandler;
import org.hotpot.events.PathWatchService;
import org.hotpot.instrumentation.transformer.HotPotClassTransformer;

public class HotPotAgent {
    private static Logger LOGGER = LogManager.getLogger(HotPotAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] premain delegate to agentmain");
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] agentmain");

        String configPathStr = agentArgs;
        Path configPath = null;
        if (configPathStr != null) {
            configPath = Paths.get(configPathStr);
        } else {
            String userHomeProp = System.getProperty("user.home");
            configPath = Paths.get(userHomeProp).resolve(".hotpot");
        }

        if (!Files.isDirectory(configPath)) {
            try {
                Files.createDirectory(configPath);
                LOGGER.info("Config dir [{}] created.", configPath);
            } catch (IOException e) {
                LOGGER.error("Creating config path [{}] failed.", configPath);
                LOGGER.error(e);
                return;
            }
        }

        LOGGER.info("Using config dir [{}].", configPath);
        ClassPathHandler classPathHandler = new ClassPathHandler(configPath);
        PathWatchService pathWatchService = new PathWatchService();
        pathWatchService.addHandlers(classPathHandler);

        HotPotClassTransformer hotPotClassTransformer = new HotPotClassTransformer();

        handleClassModified(classPathHandler, hotPotClassTransformer, inst);

        inst.addTransformer(hotPotClassTransformer, true);
        new Thread(pathWatchService).start();
    }

    private static void handleClassModified(ClassPathHandler classPathHandler,
            HotPotClassTransformer hotPotClassTransformer, Instrumentation inst) {
        BiConsumer<String, Path> addModifiedClass = hotPotClassTransformer::addModifiedClass;
        BiConsumer<String, Path> retransformClass = (className, path) -> retransform(className, path, inst);;
        BiConsumer<String, Path> modifyObserver = addModifiedClass.andThen(retransformClass);
        
        classPathHandler.onCreate(modifyObserver);
        classPathHandler.onModify(modifyObserver);
        hotPotClassTransformer.setConfFilePath(classPathHandler::getConfFilePath);
        hotPotClassTransformer.setRedefineFunc((clazz, byteCode) -> reDefine(inst, clazz, byteCode));
        
    }

    public static void retransform(String className, Path path, Instrumentation inst) {
        className = className.replace("/", ".");
        for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
            if (loadedClass.getName().equals(className)) {
                retransform(inst, loadedClass);
            }
        }
    }

    public static void reDefine(Instrumentation inst, Class<?> theClass, byte[] theClassFile) {
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
    public static void retransform(Instrumentation inst, Class<?>... classes) {
        String classesStr = Stream.of(classes)
        .map(Class::toString)
        .collect(Collectors.joining(", "));

        try {
            LOGGER.info("Retransform class(es) [{}]", classesStr);
            inst.retransformClasses(classes);
        } catch (UnmodifiableClassException e) {
            LOGGER.info("Retransform failed, class(es) [{}] is/are unmodifiable.", classesStr);
            e.printStackTrace();
        }
    }
}
