package org.hotpot.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.ProtectionDomain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.events.ClassPathConfHandler;
import org.hotpot.events.ClassPathHandler;
import org.hotpot.events.PathWatchService;
import org.hotpot.instrumentation.helper.ClassLoaderUtil;
import org.hotpot.instrumentation.transformer.TransformerService;

public class HotPotAgent {
    private static Logger LOGGER = LogManager.getLogger(HotPotAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] premain delegate to agentmain");
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] agentmain");

        // exp
        ClassLoaderUtil classLoaderUtil = new ClassLoaderUtil(inst::getAllLoadedClasses, inst::isModifiableClass);
        String className = "fsu.jportal.mets.JPMetsHierarchyGenerator";

        try {
            ClassLoader classLoader = classLoaderUtil.getClassLoader(className);
            LOGGER.info("[HotPot] Found classloader [{}] for class [{}]", classLoader.getClass().getName(), className);
            classLoader.loadClass(className);
        } catch (IOException e1) {
            LOGGER.error("[HotPot] No classloader found for class [{}]", className);
        } catch (ClassNotFoundException e) {
            LOGGER.error("[HotPot] class not found [{}]", className);
        }
        // exp

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
        ClassPathConfHandler classPathConfHandler = new ClassPathConfHandler(configPath);
        Path classPathConfFilePath = classPathConfHandler.getConfFilePath();
        ClassPathHandler classPathHandler = new ClassPathHandler();
        TransformerService ts = new TransformerService(inst, classPathConfFilePath);

        classPathConfHandler.addClassPathConsumer(classPathHandler::setClassPaths);
        classPathHandler.addClassPathHandler(ts::handle);
        classPathConfHandler.init();

        PathWatchService pathWatchService = new PathWatchService();
        pathWatchService.addHandlers(classPathConfHandler, classPathHandler);

        new Thread(pathWatchService).start();
        //info(inst);
    }

    public static void info(Instrumentation inst) {
        ClassFileTransformer transformer = new InfoTransformer();
        inst.addTransformer(transformer, true);

    }

    private static class InfoTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            try {
                String loaderName = loader.getClass().getName();
                String domain = protectionDomain.toString();

                String msg = "class: [" + className + "] - [" + domain + "] - Loader: [" + loaderName + "]";
                Files.write(Paths.get("/workspaces/agentTest/info.txt"), msg.getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                // exception handling left as an exercise for the reader
            }
            return null;
        }

    }
}
