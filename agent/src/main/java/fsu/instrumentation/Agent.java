package fsu.instrumentation;

import java.lang.instrument.Instrumentation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Agent {
    static Logger LOGGER = LogManager.getLogger();

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In premain method delegate to agentmain");
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String className, Instrumentation inst) {
        LOGGER.info("[Agent] In agentmain method");
        TransformerService ts = new TransformerService(inst);
        WatchEventHandler watchEventHandler = new WatchEventHandler(ts);
        WatchServiceRunner watchService = new WatchServiceRunner(watchEventHandler);
        new Thread(watchService).start();
    }

}
