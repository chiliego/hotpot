package org.hotpot.instrumentation;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.hotpot.agent.events.ConfigFileWatchEventHandler;
import org.hotpot.agent.events.PathWatchService;
import org.hotpot.agent.events.TransformerWatchEventHandler;

public class HotPotAgent {
    static Logger LOGGER = LogManager.getLogger();

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] premain delegate to agentmain");
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] agentmain");

        String configPathStr = agentArgs;
        Path configPath = null;
        if(configPathStr != null){
            configPath = Paths.get(configPathStr);
        } else {
            String userHomeProp = System.getProperty("user.home");
            configPath = Paths.get(userHomeProp).resolve(".hotpot");
        }

        if(!Files.isDirectory(configPath)) {
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
        TransformerService ts = new TransformerService(inst);
        TransformerWatchEventHandler watchEventHandler = new TransformerWatchEventHandler(ts);
        PathWatchService transformerWatchService = new PathWatchService(watchEventHandler);
        ConfigFileWatchEventHandler configFileEventHandler = new ConfigFileWatchEventHandler(transformerWatchService);
        PathWatchService configFileWatchServiceRunner = new PathWatchService(configFileEventHandler, configPath);
        
        configFileEventHandler.checkAndReadConfFile(configPath);

        new Thread(transformerWatchService).start();
        new Thread(configFileWatchServiceRunner).start();
    }

}
