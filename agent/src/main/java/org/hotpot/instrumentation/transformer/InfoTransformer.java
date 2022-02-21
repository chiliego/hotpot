package org.hotpot.instrumentation.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InfoTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger(InfoTransformer.class);
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        LOGGER.info("Class [{}] loaded by [{}]", className, loader);
        return null;
    }
}
