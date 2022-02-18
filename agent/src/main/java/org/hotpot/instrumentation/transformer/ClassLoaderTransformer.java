package org.hotpot.instrumentation.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotpot.asm.ASMUtils;
import org.hotpot.asm.ModClassLoaderAdapter;
import org.hotpot.asm.ModMethodAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassLoaderTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LogManager.getLogger(ClassLoaderTransformer.class);
    private Path classPathConfFilePath;
    private Class<?> targetCls;
    private String methodName;
    private String methodDescriptor;

    public ClassLoaderTransformer(Path classPathConfFilePath, Class<?> targetclassLoaderClass, String methodName, String descriptor) {
        this.classPathConfFilePath = classPathConfFilePath;
        this.targetCls = targetclassLoaderClass;
        this.methodName = methodName;
        this.methodDescriptor = descriptor;
    }

    @Override
    public byte[] transform(ClassLoader l, String name, Class<?> clazz, ProtectionDomain pd, byte[] b)
            throws IllegalClassFormatException {

        if (clazz.equals(targetCls)) {
            LOGGER.info("Transform classloader [{}]", clazz.getName());

            UnaryOperator<ClassVisitor> methodReplacer = cv -> new ModMethodAdapter(cv, methodName, methodDescriptor,
                    createMethodAdapter(classPathConfFilePath));

            return ASMUtils.applyClassVisitor(b, methodReplacer, false);
        }

        return null;
    }

    private BiFunction<String, MethodVisitor, MethodVisitor> createMethodAdapter(Path classPathConfFilePath) {
        String classPathConfFile = classPathConfFilePath.toAbsolutePath().toString();
        return (owner, mv) -> new ModClassLoaderAdapter(owner, mv, classPathConfFile);
    }
}
