package org.hotpot.instrumentation.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.asm.SubClassAdapter;
import org.hotpot.instrumentation.helper.ModifiedClass;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

public class ClassTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger(ClassTransformer.class);
    private ModifiedClass modifiedClass;
    private ModifiedClass subClass;

    public ClassTransformer(ModifiedClass modifiedClass) {
        this.modifiedClass = modifiedClass;
    }

    @Override
    public byte[] transform(ClassLoader l, String name, Class<?> clazz, ProtectionDomain pd, byte[] b)
            throws IllegalClassFormatException {
        if (name.equals(modifiedClass.getNameInternal())) {
            if(ASMUtils.onlyChangeMethodBody(b, modifiedClass.getByteCode())) {
                LOGGER.info("Swap byte code for class [{}].", name);
                return modifiedClass.getByteCode();
            }

            subClass = newSubClass();
        } else {
            LOGGER.error("No byte code swapping for class [{}].", name);
        }

        return null;
    }

    

    public ModifiedClass getSubClass() {
        return subClass;
    }

    private ModifiedClass newSubClass() {
        String superName = modifiedClass.getNameInternal();
        byte[] byteCode = modifiedClass.getByteCode();
        long currentTimeMillis = System.currentTimeMillis();
        String subClassName = superName + "_" + currentTimeMillis;
        Path subClassFile = Paths.get(subClassName + ".class").getFileName();
        Path subClassFilePath = modifiedClass.getClassFilePath()
                                    .getParent()
                                    .resolve(subClassFile);

        LOGGER.info("Creating subclass [{}] of [{}].", subClassName, superName);
        UnaryOperator<ClassVisitor> classNameModifier = cv -> getSubClassAdapter(cv, subClassName);
        byte[] subClassByteCode = ASMUtils.applyClassVisitor(byteCode, classNameModifier, false);

        return new ModifiedClass(subClassName, subClassByteCode, subClassFilePath);
    }

    private ClassVisitor getSubClassAdapter(ClassVisitor cv, String subClassName) {
        SubClassAdapter modClassAdapter = new SubClassAdapter(cv, subClassName);
        return new CheckClassAdapter(modClassAdapter);
    }
}
