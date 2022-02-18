package org.hotpot.instrumentation.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.asm.ClassRefAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

public class ClassReferenceTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger(ClassReferenceTransformer.class);
    private String superName;
    private String subName;
    ClassRefAdapter classRefAdapter;

    public ClassReferenceTransformer(String superClass, String subClass) {
        this.superName = superClass.replace(".", "/");
        this.subName = subClass.replace(".", "/");
    }

    @Override
    public byte[] transform(ClassLoader l, String name, Class<?> clazz, ProtectionDomain p, byte[] b)
            throws IllegalClassFormatException {
        UnaryOperator<ClassVisitor> classRefModifier = cv -> getClassRefAdapter(cv);
        byte[] changeClassRefByteCode = ASMUtils.applyClassVisitor(b, classRefModifier, false);
        if (classRefAdapter != null && classRefAdapter.hasRef()) {
            LOGGER.info("Change Ref for [{}]", name);
            return changeClassRefByteCode;
        }
        
        return null;
    }

    private ClassVisitor getClassRefAdapter(ClassVisitor cv) {
        classRefAdapter = new ClassRefAdapter(cv, superName, subName);
        return new CheckClassAdapter(classRefAdapter);
    }
}
