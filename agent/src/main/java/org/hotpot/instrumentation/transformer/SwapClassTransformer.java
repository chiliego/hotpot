package org.hotpot.instrumentation.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SwapClassTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger(SwapClassTransformer.class);
    private Class<?> targetCls;
    private byte[] byteCode;
    private String targetClassName;

    public SwapClassTransformer(String className, byte[] byteCode) {
        this.targetClassName = className;
        this.byteCode = byteCode;
        LOGGER.info("Class transformer for [{}].", targetClassName);
    }

    public SwapClassTransformer(Class<?> targetCls, byte[] byteCode) {
        this(targetCls.getName().replace(".", "/"), byteCode);
    }

    @Override
    public byte[] transform(ClassLoader l, String name, Class<?> clazz, ProtectionDomain pd, byte[] b)
            throws IllegalClassFormatException {
        LOGGER.info("Try Swap byte code for class [{}].", name);
        LOGGER.info("ClassLoader [{}].", l);
        if (name.equals(targetClassName)) {
            LOGGER.info("Swap byte code for class [{}].", name);
            this.targetCls = clazz;
            return this.byteCode;
        } else {
            LOGGER.error("No byte code swapping for class [{}].", name);
        }

        return b;
    }

    public Class<?> getTargetCls() {
        return targetCls;
    }
}
