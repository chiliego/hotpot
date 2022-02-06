package fsu.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyClassTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger();
    private String className;
    ClassLoader targetClassLoader;

    public MyClassTransformer(String className, ClassLoader targetClassLoader) {
        this.className = className.replaceAll("\\.", "/");
        this.targetClassLoader = targetClassLoader;
        LOGGER.info("Class transformer for {}.", this.className);
    }

    @Override
    public byte[] transform(ClassLoader l, String name, Class c, ProtectionDomain d, byte[] b)
            throws IllegalClassFormatException {
        if (name.equals(this.className) && l.equals(targetClassLoader)) {
            LOGGER.info("transform class {}", name);
            
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
