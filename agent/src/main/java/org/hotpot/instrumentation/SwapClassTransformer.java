package org.hotpot.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SwapClassTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger();
    private Class<?> targetCls;
    private byte[] byteCode;

    public SwapClassTransformer(Class<?> targetCls, byte[] byteCode) {
        this.targetCls = targetCls;
        this.byteCode = byteCode;
        LOGGER.info("Class transformer for [{}].", targetCls.getName());
    }

    @Override
    public byte[] transform(ClassLoader l, String name, Class c, ProtectionDomain d, byte[] b)
            throws IllegalClassFormatException {
                String targetClassName = this.targetCls.getName().replace(".", "/");
                ClassLoader targetClassLoader = this.targetCls.getClassLoader();
        if (name.equals(targetClassName) && l.equals(targetClassLoader)) {
            LOGGER.info("Swap byte code for class [{}].", name);
            
            return this.byteCode;
        }

        return b;
    }
}
