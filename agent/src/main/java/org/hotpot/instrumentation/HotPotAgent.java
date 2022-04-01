package org.hotpot.instrumentation;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.events.ClassPathHandler;
import org.hotpot.events.CmdPathHandler;
import org.hotpot.events.PathWatchService;

public class HotPotAgent {
    private static Logger LOGGER = LogManager.getLogger(HotPotAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] premain delegate to agentmain");
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[HotPot] agentmain");
        //HotPotAgent_orig.agentmain(agentArgs, inst);

        String hotpotHomeFolder = agentArgs;
        try {
            Path hotpotHomePath = initDirs(hotpotHomeFolder);
            Path classPathconfigPath = getClassPathConPath(hotpotHomePath);
            Path cmdPath = getCmdPath(hotpotHomePath);
    
            LOGGER.info("Using config dir [{}].", classPathconfigPath);
            ClassPathHandler classPathHandler = new ClassPathHandler(classPathconfigPath);
            CmdPathHandler cmdPathHandler = new CmdPathHandler(cmdPath);
    
            PathWatchService pathWatchService = new PathWatchService();
            pathWatchService.addHandlers(classPathHandler, cmdPathHandler);
    
            //handleChangesInClassPath(classPathHandler);
            //handleCmdExecute(classPathHandler, cmdPathHandler, inst);
            HotPot.cook(classPathHandler, cmdPathHandler, inst);
    
            new Thread(pathWatchService).start();
        } catch (IOException e) {
            LOGGER.error("Init directories failed.");
            e.printStackTrace();
        }
    }

    private static Path initDirs(String hotpotHomeFolder) throws IOException {
        Path hotpotHomePath = getHomeFolder(hotpotHomeFolder);
        Path classPathconfigPath = getClassPathConPath(hotpotHomePath);
        Path cmdPath = getCmdPath(hotpotHomePath);

        createDir(hotpotHomePath);
        createDir(classPathconfigPath);
        createDir(cmdPath);

        return hotpotHomePath;
    }

    private static Path getClassPathConPath(Path hotpotHomePath) {
        return hotpotHomePath.resolve("conf");
    }

    private static Path getHomeFolder(String hotpotHomeFolder) {
        if (hotpotHomeFolder != null) {
            return Paths.get(hotpotHomeFolder);
        } else {
            String userHomeProp = System.getProperty("user.home");
            return Paths.get(userHomeProp).resolve(".hotpot");
        }
    }

    private static Path getCmdPath(Path hotpotHomePath) {
        return hotpotHomePath.resolve("cmd");
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
}
