package fsu.instrumentation;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HotPotAgent {
    static Logger LOGGER = LogManager.getLogger();

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] premain delegate to agentmain");
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] agentmain");

        String configPathStr = agentArgs;
        Path configPath = Paths.get(configPathStr);

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

        Path dir = Paths.get("/workspaces/myApp/app/bin/main/org/chiliego");
        List<Path> watchableDirs = new ArrayList<>();
        watchableDirs.add(configPath);
        watchableDirs.add(dir);
        
        TransformerService ts = new TransformerService(inst);
        WatchEventHandler watchEventHandler = new WatchEventHandler(ts);
        WatchServiceRunner watchService = new WatchServiceRunner(watchEventHandler, watchableDirs);
        
        new Thread(watchService).start();
    }

}
