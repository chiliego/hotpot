package fsu.instrumentation;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Agent {
    private static Logger LOGGER = LogManager.getLogger();

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In agentmain method delegate to agentmain");
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String className, Instrumentation inst) {
        LOGGER.info("[Agent] In agentmain method");
        // see if we can get the class using forName
        try {
            TransformerService ts = new TransformerService(inst);
            ts.transform(className);
        } catch (Exception ex) {
            LOGGER.error("Class [{}] not found with Class.forName");
        }
    }

    private static class TransformerService {
        private Instrumentation inst;

        public TransformerService(Instrumentation inst) {
            this.inst = inst;
        }

        public void transformStr(String className) {
            MyClassTransformerStr transformer = new MyClassTransformerStr(className);
            inst.addTransformer(transformer, true);
        }

        public void transform(String className) {
            MyClassTransformer transformer;
            Class<?> targetCls;

            try {
                targetCls = Class.forName(className);
                ClassLoader targetClassLoader = targetCls.getClassLoader();
                transformer = new MyClassTransformer(className, targetClassLoader);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Could not load class " + className + ".", e);
                return;
            }

            try {
                inst.addTransformer(transformer, true);
                inst.retransformClasses(targetCls);
                WatchServiceThread watchService = new WatchServiceThread(() -> inst.retransformClasses(targetCls));
                new Thread(watchService).start();
            } catch (UnmodifiableClassException e) {
                LOGGER.error("Can not retransform class " + className + ".", e);
            }
        }
        
    }

    /**
     * Executor
     */ 
    public interface Command {
        public void run() throws UnmodifiableClassException;
    }

    private static class WatchServiceThread implements Runnable {
        private Command cmd;

        public WatchServiceThread(Command cmd) {
            this.cmd = cmd;
        }
        @Override
        public void run() {
            try {
                watchForChanges(this.cmd);
            } catch (UnmodifiableClassException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void watchForChanges(Command cmd) throws UnmodifiableClassException {
        
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
        
                Path path = Paths.get("/workspaces/myApp/app/bin/main/org/chiliego");
                path.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_MODIFY);
                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = (Path)event.context();
                        LOGGER.info("Event kind: {}. File affected: {}.", event.kind(), changed);
                        if (changed.endsWith("FaultyClass.class")) {
                            cmd.run();;
                        } else {
                            LOGGER.info("Class don't end with FaultyClass.class");
                        }
                    }
                    key.reset();
                }
            } catch (IOException e) {
                LOGGER.error("Coud not register watchservice.", e);
            } catch (InterruptedException e) {
                LOGGER.error("Watchservice coud not get key.", e);
            }
    
        }
        
    }

    
}
