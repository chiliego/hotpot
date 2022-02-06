package fsu.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyClassTransformerStr implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger();
    private String className;

    public MyClassTransformerStr(String className) {
        this.className = className.replaceAll("\\.", "/");
        LOGGER.info("Class transformer for {}.", this.className);
    }

    @Override
    public byte[] transform(ClassLoader l, String name, Class c, ProtectionDomain d, byte[] b)
            throws IllegalClassFormatException {
        if (name.equals(this.className)) {
            LOGGER.info("transform class {}", name);
            /*
             * CustomClassWriter cr = new CustomClassWriter(b);
             * return cr.write(SwapMethodAdapter.forMethod("name", true));
             */
            String pathStr = "/workspaces/myApp/app/bin/main/org/chiliego/FaultyClass.class";
            Path classPath = Paths.get(pathStr);
            try {
                return Files.readAllBytes(classPath);
            } catch (IOException e) {
                LOGGER.error("Could not read class file " + pathStr + ".", e);
            }
        }

        return b;
    }
}
