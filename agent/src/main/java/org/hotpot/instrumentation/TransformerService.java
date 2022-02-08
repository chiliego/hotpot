package org.hotpot.instrumentation;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;

import org.hotpot.asm.ClassNameAdapter;

public class TransformerService {
    private static Logger LOGGER = LogManager.getLogger();
    private Instrumentation inst;

    public TransformerService(Instrumentation inst) {
        this.inst = inst;
    }

    public void swapClassFile(Path classFile) {
        try {
            byte[] byteCode = Files.readAllBytes(classFile);
            swapClassFile(byteCode);
        } catch (IOException e) {
            HotPotAgent.LOGGER.error("Could not read class file " + classFile + ".", e);
        }
    }

    public void swapClassFile(byte[] byteCode) {
        ClassReader classReader = new ClassReader(byteCode);
        ClassNameAdapter classNameAdapter = new ClassNameAdapter();
        classReader.accept(classNameAdapter, 0);
        String className = classNameAdapter.getClassName();

        SwapClassTransformer transformer;
        Class<?> targetCls;
        try {
            targetCls = Class.forName(className);
            transformer = new SwapClassTransformer(targetCls, byteCode);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class [{}] not found with Class.forName.", className);
            LOGGER.error(e);
            return;
        } 

        try{
            inst.addTransformer(transformer, true);
            inst.retransformClasses(targetCls);
        } catch (UnmodifiableClassException e) {
            LOGGER.error("Can not retransform class [{}].", className);
            LOGGER.error(e);
        } finally {
            inst.removeTransformer(transformer);
        }
    }

}
